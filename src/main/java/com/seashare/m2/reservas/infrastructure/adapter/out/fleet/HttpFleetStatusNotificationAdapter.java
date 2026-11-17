package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.FleetNotificationOutcome;
import com.seashare.m2.reservas.application.port.out.FleetStatusNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // activo solo en modo http
public class HttpFleetStatusNotificationAdapter implements FleetStatusNotificationPort { // cliente HTTP real de aviso de estado a M1

    private static final Logger log = LoggerFactory.getLogger(HttpFleetStatusNotificationAdapter.class); // log de advertencias

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M1

    private static final String STATUS_EVENTS_PATH = "/api/v1/internal/status-events"; // ruta del contrato de M1

    // POST de escritura: 300 ms de lectura (consulta) no alcanzan; se usa cliente propio sin tocar FleetHttpConfig
    private static final long READ_TIMEOUT_MS = 5000;

    private final RestClient restClient; // cliente propio con timeout de lectura mayor; contrato sin autenticacion

    public HttpFleetStatusNotificationAdapter(FleetProperties properties) { // inyecta propiedades de flota
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory(); // fabrica simple con timeouts
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs())); // mismo connect que los demas clientes
        factory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS)); // lectura holgada para el POST de escritura
        this.restClient = RestClient.builder() // construye el cliente propio
                .baseUrl(properties.getBaseUrl()) // url base desde seashare.fleet.base-url
                .requestFactory(factory) // aplica la fabrica con timeouts
                .build(); // sin X-Internal-Service-Token: M1 no pide autenticacion en esta ruta
    }

    @Override
    public FleetNotificationOutcome notifyStatus(PendingOutboxMessage message) { // envia el payload tal cual y clasifica la respuesta
        ResponseEntity<String> response; // respuesta cruda de M1
        try {
            response = post(message.payload()); // POST con el payload guardado en el outbox
        } catch (ResourceAccessException e) { // timeout o conexion rechazada
            log.warn("M1 status-events unreachable for event {}", message.eventId()); // aviso y reintento
            return FleetNotificationOutcome.RETRYABLE_FAILURE; // fallo reintentable
        } catch (RuntimeException e) { // cualquier otro error del cliente
            log.warn("M1 status-events call failed for event {}: {}", message.eventId(), e.getMessage()); // aviso
            return FleetNotificationOutcome.RETRYABLE_FAILURE; // el relay reintenta con backoff
        }
        int status = response.getStatusCode().value(); // codigo http de la respuesta
        if (status >= 400 && status < 500) { // 400 = aviso invalido; resto de 4xx permanente (D-05)
            return FleetNotificationOutcome.PERMANENT_FAILURE; // dead directo sin reintento
        }
        if (status >= 500) { // 5xx transitorio
            return FleetNotificationOutcome.RETRYABLE_FAILURE; // reintento con backoff
        }
        if (status >= 200 && status < 300) { // 2xx: hay que leer el outcome, no alcanza con el status
            return mapOutcome(response.getBody(), message.eventId()); // decide segun outcome
        }
        return FleetNotificationOutcome.RETRYABLE_FAILURE; // status inesperado: mejor reintentar
    }

    private FleetNotificationOutcome mapOutcome(String body, UUID eventId) { // mapea el outcome del contrato de M1
        String outcome = parseOutcome(body); // null si el cuerpo no trae outcome legible
        if (outcome == null) { // sin outcome no se sabe si cambio el estado
            log.warn("M1 status-events response for event {} has no readable outcome", eventId); // aviso
            return FleetNotificationOutcome.RETRYABLE_FAILURE; // reintento conservador
        }
        return switch (outcome) { // catalogo de outcomes del contrato
            case "APPLIED", "DUPLICATE", "NO_CHANGE", "STALE" -> FleetNotificationOutcome.DELIVERED; // aceptado
            case "UNKNOWN_VESSEL", "INVALID_TRANSITION" -> FleetNotificationOutcome.PERMANENT_FAILURE; // rechazo definitivo
            default -> { // outcome fuera del catalogo conocido
                log.warn("M1 status-events for event {} returned unknown outcome {}", eventId, outcome); // aviso
                yield FleetNotificationOutcome.RETRYABLE_FAILURE; // no se puede clasificar: reintenta
            }
        };
    }

    private ResponseEntity<String> post(String payload) { // POST con control manual de estados sin catch ciego
        return restClient.post().uri(STATUS_EVENTS_PATH) // ruta fija del contrato
                .contentType(MediaType.APPLICATION_JSON) // cuerpo json
                .body(payload) // payload del outbox tal cual
                .exchange((request, response) -> { // exchange evita que lance en 4xx/5xx
                    int status = response.getStatusCode().value(); // codigo http
                    String body = response.getBody() == null ? "" // cuerpo vacio posible
                            : new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8); // cuerpo en utf-8
                    return ResponseEntity.status(status).body(body); // conserva status y cuerpo
                });
    }

    private static String parseOutcome(String body) { // extrae el campo outcome del json de respuesta
        if (body == null || body.isBlank()) { // 200 sin cuerpo no aporta outcome
            return null; // se trata como outcome ausente
        }
        try {
            JsonNode root = MAPPER.readTree(body); // jackson 3 lanza JacksonException, no checked
            JsonNode outcome = root.path("outcome"); // nodo outcome
            return outcome.isString() && !outcome.stringValue().isBlank() ? outcome.stringValue() : null; // null si ausente
        } catch (JacksonException e) { // cuerpo roto
            return null; // ilegible: mismo caso que outcome ausente
        }
    }
}
