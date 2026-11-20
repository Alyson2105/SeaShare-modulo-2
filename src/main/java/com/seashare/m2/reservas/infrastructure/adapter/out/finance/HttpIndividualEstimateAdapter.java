package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.IndividualEstimate;
import com.seashare.m2.reservas.application.port.out.IndividualEstimatePort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "http") // activos solo en modo http
public class HttpIndividualEstimateAdapter implements IndividualEstimatePort { // cliente HTTP real de estimacion individual (CU-11)

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M3

    private static final String INDIVIDUAL_PATH = "/api/v1/estimates/individual"; // ruta del contrato CU-11

    private final RestClient restClient; // cliente con timeouts individuales ya configurados

    public HttpIndividualEstimateAdapter(@Qualifier("individualFinanceRestClient") RestClient restClient) { // inyecta el cliente
        this.restClient = restClient; // asigna cliente
    }

    @Override
    public IndividualEstimate getIndividualEstimate(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers) { // consulta la estimacion a M3
        String body = post(INDIVIDUAL_PATH, jsonBody(vesselId, startDate, endDate, passengers)); // POST con reintento limitado
        return parse(body); // monto opaco y warning literal
    }

    private String post(String path, String json) { // POST con un reintento solo por conexion rechazada
        for (int attempt = 0; attempt < 2; attempt++) { // maximo dos intentos
            try {
                return exchange(path, json); // una llamada HTTP
            } catch (ResourceAccessException e) { // fallo de red antes de llegar a respuesta
                if (attempt == 0 && isRetryableConnectionFailure(e)) { // solo ConnectException reintenta
                    continue; // un reintento inmediato
                }
                throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance service is unreachable"); // timeout o sin conexion
            }
        }
        throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance service is unreachable"); // no alcanzable
    }

    static boolean isRetryableConnectionFailure(Throwable failure) { // decide el reintento: unicamente conexion rechazada
        Throwable current = failure; // recorre la cadena de causas de la excepcion
        while (current != null) { // hasta llegar al fondo de la cadena
            if (current instanceof ConnectException) { // conexion rechazada: el unico caso reintetable
                return true; // autoriza el reintento
            }
            current = current.getCause(); // sube una causa
        }
        return false; // timeouts (SocketTimeoutException) y demas no se reintantan
    }

    private String exchange(String path, String json) { // mapea los estados de error de M3 a excepciones de dominio
        return restClient.post().uri(path) // POST hacia modulo 3
                .contentType(MediaType.APPLICATION_JSON) // cuerpo json
                .body(json) // payload del contrato
                .exchange((request, response) -> { // exchange controla cada estado sin catch ciego
                    int status = response.getStatusCode().value(); // codigo http de la respuesta
                    if (status == 400) { // parametros rechazados por M3
                        throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "Finance module rejected the estimate parameters"); // nunca se reintenta
                    }
                    if (status == 401 || status == 403) { // acceso denegado en M3
                        throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Finance module rejected the request"); // nunca se reintenta
                    }
                    if (status < 200 || status >= 300) { // 422, 500, 503 u otro estado
                        throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance service returned status " + status); // cotizacion no disponible
                    }
                    return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8); // cuerpo en utf-8
                });
    }

    private static String jsonBody(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers) { // arma el body del contrato
        Map<String, Object> payload = new LinkedHashMap<>(); // mapa con orden estable
        payload.put("boat_id", vesselId.toString()); // traduce vessel_id a boat_id para M3
        payload.put("start_date", startDate.toString()); // YYYY-MM-DD
        payload.put("end_date", endDate.toString()); // YYYY-MM-DD
        payload.put("passengers", passengers); // cantidad numerica
        return MAPPER.writeValueAsString(payload); // json con solo los campos del contrato
    }

    private static IndividualEstimate parse(String body) { // extrae estimated_total y warning sin calcular nada
        if (body == null || body.isBlank()) { // 200 sin cuerpo no aporta datos
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response body is empty"); // respuesta invalida
        }
        JsonNode root; // arbol json de la respuesta
        try {
            root = MAPPER.readTree(body); // jackson 3 lanza JacksonException, que no es checked
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response body is not valid json"); // cuerpo ilegible
        }
        JsonNode totalNode = root.path("estimated_total"); // monto devuelto por M3
        if (!totalNode.isString() || totalNode.stringValue().isBlank()) { // obligatorio y como string decimal
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response is missing estimated_total"); // sin monto no hay estimacion
        }
        BigDecimal total; // monto opaco leido tal cual
        try {
            total = new BigDecimal(totalNode.stringValue().trim()); // solo lectura; M2 no calcula
        } catch (NumberFormatException e) {
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response estimated_total is not a number"); // ilegible
        }
        if (total.signum() <= 0) { // cero o negativo no sirve; signum no es aritmetica de montos
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response estimated_total must be positive"); // monto invalido
        }
        JsonNode warningNode = root.path("warning"); // texto opcional de M3
        String warning = warningNode.isString() ? warningNode.stringValue() : null; // null si no viene; los campos desconocidos se ignoran
        return new IndividualEstimate(total, warning); // devuelve el monto tal cual
    }
}
