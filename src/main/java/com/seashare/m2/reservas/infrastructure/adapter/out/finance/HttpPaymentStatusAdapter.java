package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.PaymentStatusResult;
import com.seashare.m2.reservas.application.port.out.PaymentStatusPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "http") // activo solo en modo http
public class HttpPaymentStatusAdapter implements PaymentStatusPort { // consulta UC06 de modulo 3 (CU-13 canal B)

    private static final Logger log = LoggerFactory.getLogger(HttpPaymentStatusAdapter.class);

    private static final ObjectMapper MAPPER = new JsonMapper();

    private static final String PATH = "/api/v1/reservations/"; // base del contrato UC06

    private static final String SUFFIX = "/payment-confirmation"; // recurso de confirmacion

    private final RestClient restClient; // cliente con timeouts de CU-13

    public HttpPaymentStatusAdapter(@Qualifier("paymentStatusRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PaymentStatusResult getStatus(UUID reservationId) { // nunca propaga: un fallo no cambia el estado
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(PATH + reservationId + SUFFIX)
                    .header("Accept", "application/json") // header del contrato UC06
                    .exchange((request, result) -> ResponseEntity
                            .status(result.getStatusCode().value())
                            .body(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)));
            int status = response.getStatusCode().value();
            if (status == 404) { // M3 aun crea la intencion de cobro: reintenta en el siguiente ciclo
                return unknown(null);
            }
            if (status == 401 || status == 403) { // alerta critica, sin cambio de estado
                log.error("CU-13: modulo 3 rechazo la consulta de cobro con estado {}", status);
                return unknown(null);
            }
            if (status < 200 || status >= 300) { // 400 o 5xx: se reintenta en el siguiente ciclo
                log.warn("CU-13: consulta de cobro devolvio {}", status);
                return unknown(null);
            }
            return parse(response.getBody());
        } catch (RuntimeException e) { // timeout, conexion o respuesta ilegible
            log.warn("CU-13: no se pudo consultar el cobro de {}: {}", reservationId, e.getMessage());
            return unknown(null); // fail-safe: nunca se asume aprobado
        }
    }

    private PaymentStatusResult parse(String body) { // lee el contrato UC06
        if (body == null || body.isBlank()) {
            return unknown(null);
        }
        try {
            JsonNode root = MAPPER.readTree(body);
            String statusText = text(root, "status");
            PaymentStatus status = statusText == null ? PaymentStatus.UNKNOWN
                    : PaymentStatus.valueOf(statusText.trim().toUpperCase(Locale.ROOT));
            return new PaymentStatusResult(status, text(root, "detail"), text(root, "external_reference"));
        } catch (JacksonException | IllegalArgumentException e) {
            log.warn("CU-13: respuesta de cobro ilegible: {}", e.getMessage());
            return unknown(null);
        }
    }

    private static PaymentStatusResult unknown(String detail) {
        return new PaymentStatusResult(PaymentStatus.UNKNOWN, detail, null);
    }

    private static String text(JsonNode node, String field) { // texto opcional
        JsonNode value = node.path(field);
        return value.isString() && !value.stringValue().isBlank() ? value.stringValue() : null;
    }
}
