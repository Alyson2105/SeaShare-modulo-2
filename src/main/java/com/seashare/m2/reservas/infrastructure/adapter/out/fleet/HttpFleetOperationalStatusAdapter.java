package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.ExternalQueryLogEntry;
import com.seashare.m2.reservas.application.dto.ExternalQueryResult;
import com.seashare.m2.reservas.application.dto.ExternalQueryType;
import com.seashare.m2.reservas.application.dto.FleetMaintenanceWindow;
import com.seashare.m2.reservas.application.port.out.ExternalQueryLogPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // activos solo en modo http
public class HttpFleetOperationalStatusAdapter implements FleetOperationalStatusPort { // cliente HTTP real de estado operativo (CU-10)

    private static final Logger log = LoggerFactory.getLogger(HttpFleetOperationalStatusAdapter.class); // log de advertencias del registro

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M1

    private static final String VESSELS_PATH = "/api/v1/vessels/"; // ruta base del contrato CU-10

    private static final ZoneId DEFAULT_HARBOR_ZONE = ZoneId.of("America/Bogota"); // zona por defecto del contrato C3

    private final RestClient restClient; // cliente con timeouts y token ya configurados

    private final ExternalQueryLogPort externalQueryLogPort; // registro de trazabilidad de la consulta (FR-009)

    private final ClockPort clockPort; // reloj unico para evaluar el vencimiento del mantenimiento

    public HttpFleetOperationalStatusAdapter(@Qualifier("fleetRestClient") RestClient restClient,
                                             ExternalQueryLogPort externalQueryLogPort,
                                             ClockPort clockPort) { // inyecta cliente, registro y reloj
        this.restClient = restClient; // asigna cliente
        this.externalQueryLogPort = externalQueryLogPort; // asigna registro
        this.clockPort = clockPort; // asigna reloj
    }

    @Override
    public boolean isAvailable(UUID vesselId, LocalDate start, LocalDate end, ZoneId harborZone) { // consulta el estado operativo en M1
        ZoneId zone = harborZone == null ? DEFAULT_HARBOR_ZONE : harborZone; // zona del puerto o la del contrato
        try {
            JsonNode root = fetchJson(vesselId, start, end, zone); // GET con rango y zona
            String rawStatus = rawStatus(root); // estado reportado por M1
            boolean available = decide(rawStatus, root, start, end, zone); // interpreta estado y ventana de mantenimiento
            register(vesselId, ExternalQueryResult.OK, rawStatus); // valor crudo solo si la respuesta es interpretable
            return available; // veredicto del contrato C3
        } catch (TransientQueryException e) { // timeout, conexion o 503 tras el reintento
            register(vesselId, ExternalQueryResult.TIMEOUT, null); // resultado final TIMEOUT
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, e.getMessage()); // mismo ErrorCode y mensaje de la parte A
        } catch (ReservationRuleException e) { // 4xx, 5xx sin reintento u otro fallo
            register(vesselId, ExternalQueryResult.ERROR, null); // resultado final ERROR
            throw e; // la consulta a M1 conserva su error original
        }
    }

    @Override
    public Optional<FleetMaintenanceWindow> maintenanceWindow(UUID vesselId, LocalDate start, LocalDate end,
                                                              ZoneId harborZone) { // ventana vigente para el job de respaldo
        ZoneId zone = harborZone == null ? DEFAULT_HARBOR_ZONE : harborZone; // zona del puerto o la del contrato
        try {
            JsonNode root = fetchJson(vesselId, start, end, zone); // misma consulta C3
            String rawStatus = rawStatus(root); // estado reportado por M1
            Optional<FleetMaintenanceWindow> window = parseWindow(rawStatus, root); // fechas si hay mantenimiento
            register(vesselId, ExternalQueryResult.OK, rawStatus); // una fila por consulta
            return window;
        } catch (TransientQueryException e) { // timeout o 503
            register(vesselId, ExternalQueryResult.TIMEOUT, null);
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, e.getMessage());
        } catch (ReservationRuleException e) { // 4xx, 5xx o respuesta ilegible
            register(vesselId, ExternalQueryResult.ERROR, null);
            throw e;
        }
    }

    private JsonNode fetchJson(UUID vesselId, LocalDate start, LocalDate end, ZoneId zone) { // GET y parseo comun a C3
        String body = get(VESSELS_PATH + vesselId + "/operational-status"
                + "?start_date=" + start
                + "&end_date=" + end
                + "&timezone=" + URLEncoder.encode(zone.getId(), StandardCharsets.UTF_8)); // GET con rango y zona
        if (body == null || body.isBlank()) { // sin cuerpo no hay estado que leer
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is empty"); // igual estilo que CU-09
        }
        try {
            return MAPPER.readTree(body); // parsea la respuesta
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is not valid json"); // igual estilo que CU-09
        }
    }

    private String rawStatus(JsonNode root) { // estado crudo de la respuesta
        JsonNode statusNode = root.path("operational_status"); // estado reportado por M1
        String rawStatus = statusNode.isString() ? statusNode.stringValue() : null; // null si ausente o no texto
        if (rawStatus == null || rawStatus.isBlank()) { // ausente, vacio o no texto
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response is missing operational_status");
        }
        return rawStatus; // texto devuelto por M1
    }

    private Optional<FleetMaintenanceWindow> parseWindow(String rawStatus, JsonNode root) { // fechas del mantenimiento
        String status = rawStatus.trim().toUpperCase(Locale.ROOT);
        if (!"MAINTENANCE".equals(status) && !"MAINTENANCE_CLEANING".equals(status)) { // sin mantenimiento
            return Optional.empty(); // no hay ventana que sintetizar
        }
        OffsetDateTime until = offsetDate(root, "maintenance_until"); // fin del mantenimiento
        if (until == null) { // sin fecha: no se inventa una ventana
            return Optional.empty();
        }
        OffsetDateTime from = offsetDate(root, "maintenance_start"); // inicio opcional
        Instant start = from == null ? clockPort.now() : from.toInstant(); // nulo = ahora
        return Optional.of(new FleetMaintenanceWindow(start, until.toInstant())); // ventana reportada
    }

    private boolean decide(String rawStatus, JsonNode root, LocalDate start, LocalDate end, ZoneId zone) { // veredicto del contrato C3
        return switch (rawStatus.trim().toUpperCase(Locale.ROOT)) {
            case "AVAILABLE", "RESERVED", "NAVIGATION", "IN_NAVIGATION" -> true; // disponible para el rango
            case "MAINTENANCE", "MAINTENANCE_CLEANING" -> maintenanceAllows(root, start, end, zone); // evalua la ventana
            default -> throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE,
                    "Fleet response returned an unknown operational_status"); // fail-safe
        };
    }

    private boolean maintenanceAllows(JsonNode root, LocalDate start, LocalDate end, ZoneId zone) { // cruce inclusivo por fecha local
        OffsetDateTime until = offsetDate(root, "maintenance_until"); // fin obligatorio del mantenimiento
        if (until == null) { // mantenimiento sin fecha: comportamiento seguro
            return false; // bloquea todo
        }
        Instant now = clockPort.now(); // instante unico de la consulta
        if (!until.toInstant().isAfter(now)) { // maintenance_until vencido: liberacion perezosa (D-07)
            return true; // se considera disponible
        }
        OffsetDateTime from = offsetDate(root, "maintenance_start"); // inicio opcional: nulo = ahora
        Instant startInstant = from == null ? now : from.toInstant(); // instante de inicio efectivo
        LocalDate maintenanceStart = startInstant.atZone(zone).toLocalDate(); // fecha local del puerto
        LocalDate maintenanceEnd = until.toInstant().atZone(zone).toLocalDate(); // fecha local del puerto
        if (maintenanceStart.isAfter(maintenanceEnd)) { // ventana incoherente
            return false; // fail-safe: bloquea
        }
        boolean crosses = !maintenanceStart.isAfter(end) && !maintenanceEnd.isBefore(start); // rango inclusivo []
        return !crosses; // disponible solo si no toca ningun dia de la reserva
    }

    private OffsetDateTime offsetDate(JsonNode root, String field) { // lee un instante ISO-8601 con offset
        JsonNode node = root.path(field); // campo de la respuesta
        if (node.isMissingNode() || node.isNull()) { // ausente o nulo
            return null; // sin fecha
        }
        if (!node.isString()) { // tipo inesperado
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE,
                    "Fleet response has an invalid " + field); // fail-safe
        }
        try {
            return OffsetDateTime.parse(node.stringValue()); // ISO-8601 con offset
        } catch (DateTimeParseException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE,
                    "Fleet response has an invalid " + field); // fail-safe
        }
    }

    private void register(UUID vesselId, ExternalQueryResult result, String returnedStatus) { // registra sin romper nada
        try {
            externalQueryLogPort.register(new ExternalQueryLogEntry(ExternalQueryType.OPERATIONAL_STATUS, vesselId, result, returnedStatus)); // una fila por consulta
        } catch (Exception e) { // el registro nunca cambia el resultado de la consulta a M1
            log.warn("External query log registration failed for vessel {}", vesselId, e); // advertencia y sigue
        }
    }

    private String get(String path) { // GET con un solo reintento inmediato
        for (int attempt = 0; attempt < 2; attempt++) { // dos intentos como maximo
            try {
                ResponseEntity<String> response = exchange(path); // una llamada HTTP
                int status = response.getStatusCode().value(); // estado devuelto por M1
                if (status >= 200 && status < 300) { // exito
                    return response.getBody() == null ? "" : response.getBody(); // cuerpo json
                }
                if (status == 503 && attempt == 0) { // 503 es transitorio: un solo reintento
                    continue; // reintenta de inmediato sin espera
                }
                if (status == 503) { // 503 persistente tras el reintento: transitorio agotado
                    throw new TransientQueryException("Fleet service returned status 503"); // se registra TIMEOUT con el mensaje de siempre
                }
                throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service returned status " + status); // 5xx u otro estado: ERROR
            } catch (ResourceAccessException e) { // timeout o conexion rechazada (IOException)
                if (attempt == 1) { // se agoto el reintento
                    throw new TransientQueryException("Fleet service is unreachable"); // se registra TIMEOUT
                }
            }
        }
        throw new TransientQueryException("Fleet service is unreachable"); // no alcanzable, mensaje identico al de antes
    }

    private ResponseEntity<String> exchange(String path) { // mapea los estados de error de M1 a excepciones de dominio
        return restClient.get().uri(path).exchange((request, response) -> { // exchange controla cada estado sin catch ciego
            int status = response.getStatusCode().value(); // codigo http de la respuesta
            if (status == 404) { // embarcacion no registrada en M1
                throw new ReservationRuleException(ErrorCode.VESSEL_NOT_FOUND, "Vessel not found in fleet module"); // nunca reintenta
            }
            if (status == 400) { // vessel_id rechazado por sintaxis
                throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Vessel id rejected by fleet module"); // nunca reintenta
            }
            if (status == 401 || status == 403) { // token de servicio rechazado
                throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Fleet module rejected the service token"); // alerta sin reintentos
            }
            if (status < 200 || status >= 300) { // 5xx u otro estado inesperado
                return ResponseEntity.status(status).build(); // sin cuerpo; el bucle decide si reintenta
            }
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8); // cuerpo en utf-8
            return ResponseEntity.status(status).body(body); // 200 con cuerpo json
        });
    }
}
