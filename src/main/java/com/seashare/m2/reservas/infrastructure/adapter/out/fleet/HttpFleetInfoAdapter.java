package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
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
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // activos solo en modo http
public class HttpFleetInfoAdapter implements FleetInfoPort { // cliente HTTP real de consulta de flota (CU-09)

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M1
    private static final String VESSELS_PATH = "/api/v1/vessels/"; // ruta base del contrato CU-09

    private final RestClient restClient; // cliente con timeouts y token ya configurados

    public HttpFleetInfoAdapter(@Qualifier("fleetRestClient") RestClient restClient) { // inyecta el cliente de flota
        this.restClient = restClient; // asigna cliente
    }

    @Override
    public FleetInfo getVesselInfo(UUID vesselId) { // consulta la ficha de la embarcacion en M1
        String body = get(VESSELS_PATH + vesselId); // GET con un solo reintento y mapeo de errores
        JsonNode root = parse(body); // cuerpo vacio o roto corta sin valores por defecto
        JsonNode ownerIdNode = root.path("owner").path("owner_id"); // nodo owner.owner_id
        UUID ownerId; // identificador del propietario
        try {
            ownerId = UUID.fromString(ownerIdNode.isString() ? ownerIdNode.stringValue() : ""); // ausente vuelve cadena vacia
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response owner_id is not a valid uuid"); // owner_id ilegible
        }
        JsonNode capacityNode = root.path("max_capacity"); // nodo max_capacity
        if (!capacityNode.isNumber()) { // sin capacidad no se reserva a ciegas
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response is missing max_capacity"); // payload incompleto
        }
        JsonNode timezoneNode = root.path("port").path("timezone"); // nodo port.timezone
        String harborTimezone = timezoneNode.isString() ? timezoneNode.stringValue() : null; // null si ausente o no texto
        if (harborTimezone == null || harborTimezone.isBlank()) { // la zona horaria es obligatoria para CU-04 y CU-05
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE, "Harbor timezone is missing"); // sin zona no hay plazos
        }
        try {
            ZoneId.of(harborTimezone); // valida el formato IANA sin conservar el objeto
        } catch (DateTimeException e) {
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE, "Harbor timezone is invalid"); // zona rechazada
        }
        return new FleetInfo(ownerId, capacityNode.intValue(), harborTimezone); // devuelve los tres campos requeridos
    }

    private static JsonNode parse(String body) { // convierte el cuerpo a arbol json con corte seguro
        if (body == null || body.isBlank()) { // 200 sin cuerpo no aporta datos
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is empty"); // fail-safe sin datos
        }
        try {
            return MAPPER.readTree(body); // jackson 3 lanza JacksonException, que no es checked
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is not valid json"); // cuerpo ilegible
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
                throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service returned status " + status); // 5xx u otro estado
            } catch (ResourceAccessException e) { // timeout o conexion rechazada (IOException)
                if (attempt == 1) { // se agoto el reintento
                    throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service is unreachable"); // corte seguro
                }
            }
        }
        throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service is unreachable"); // no alcanzable
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
