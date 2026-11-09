package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.ExternalQueryLogEntry;
import com.seashare.m2.reservas.application.dto.ExternalQueryResult;
import com.seashare.m2.reservas.application.dto.ExternalQueryType;
import com.seashare.m2.reservas.application.port.out.ExternalQueryLogPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // activos solo en modo http
public class HttpFleetOperationalStatusAdapter implements FleetOperationalStatusPort { // cliente HTTP real de estado operativo (CU-10)

    private static final Logger log = LoggerFactory.getLogger(HttpFleetOperationalStatusAdapter.class); // log de advertencias del registro

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M1

    private static final String VESSELS_PATH = "/api/v1/vessels/"; // ruta base del contrato CU-10

    private final RestClient restClient; // cliente con timeouts y token ya configurados

    private final ExternalQueryLogPort externalQueryLogPort; // registro de trazabilidad de la consulta (FR-009)

    public HttpFleetOperationalStatusAdapter(@Qualifier("fleetRestClient") RestClient restClient, ExternalQueryLogPort externalQueryLogPort) { // inyecta cliente y registro
        this.restClient = restClient; // asigna cliente
        this.externalQueryLogPort = externalQueryLogPort; // asigna registro
    }

    @Override
    public boolean isAvailable(UUID vesselId) { // consulta el estado operativo en M1
        try {
            String body = get(VESSELS_PATH + vesselId + "/operational-status"); // GET con un solo reintento
            if (body == null || body.isBlank()) { // sin cuerpo no hay estado que leer
                throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is empty"); // igual estilo que CU-09
            }
            String rawStatus; // estado crudo devuelto por M1
            try {
                JsonNode root = MAPPER.readTree(body); // parsea la respuesta
                JsonNode statusNode = root.path("operational_status"); // estado reportado por M1
                rawStatus = statusNode.isString() ? statusNode.stringValue() : null; // null si ausente o no texto
            } catch (JacksonException e) {
                throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is not valid json"); // igual estilo que CU-09
            }
            if (rawStatus == null || rawStatus.isBlank()) { // ausente, vacio o no texto
                throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response is missing operational_status"); // el catch registra ERROR
            }
            register(vesselId, ExternalQueryResult.OK, rawStatus); // valor crudo, incluso si M1 devuelve uno desconocido
            return "AVAILABLE".equals(rawStatus); // true solo si AVAILABLE
        } catch (TransientQueryException e) { // timeout, conexion o 503 tras el reintento
            register(vesselId, ExternalQueryResult.TIMEOUT, null); // resultado final TIMEOUT
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, e.getMessage()); // mismo ErrorCode y mensaje de la parte A
        } catch (ReservationRuleException e) { // 4xx, 5xx sin reintento u otro fallo
            register(vesselId, ExternalQueryResult.ERROR, null); // resultado final ERROR
            throw e; // la consulta a M1 conserva su error original
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
