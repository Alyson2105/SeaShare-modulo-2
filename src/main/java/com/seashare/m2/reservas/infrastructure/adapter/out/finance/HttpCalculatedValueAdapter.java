package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.port.out.CalculatedValuePort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import com.seashare.m2.shared.infrastructure.adapter.in.web.CorrelationIdFilter;
import org.slf4j.MDC;
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

import java.math.BigDecimal;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "http") // activos solo en modo http
public class HttpCalculatedValueAdapter implements CalculatedValuePort { // cliente HTTP real del valor calculado (CU-12)

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M3

    private static final String RESERVATIONS_PATH = "/api/v1/reservations/"; // ruta base del contrato CU-12

    private static final String CALCULATED_VALUE_SUFFIX = "/calculated-value"; // sufijo del recurso

    private static final int NOT_FOUND_RETRIES = 3; // reintentos documentados ante 404

    private static final long NOT_FOUND_RETRY_DELAY_MS = 300L; // espera indicada por el plan de CU-12

    private final RestClient restClient; // cliente con timeouts propios ya configurados

    public HttpCalculatedValueAdapter(@Qualifier("calculatedValueRestClient") RestClient restClient) { // cliente propio de CU-12
        this.restClient = restClient; // asigna cliente
    }

    @Override
    public CalculatedValue getCalculatedValue(UUID reservationId) { // consulta el valor calculado a M3
        String body = post(reservationId); // una sola llamada, sin esperas en el hilo de peticion
        return parse(body); // desglose opaco sin aritmetica
    }

    private String post(UUID reservationId) { // POST con reintentos acotados ante 404 de M3
        String path = RESERVATIONS_PATH + reservationId + CALCULATED_VALUE_SUFFIX; // ruta completa
        boolean socketRetried = false; // unico reintento de socket ya usado
        int notFoundRetries = 0; // reintentos por informacion aun no registrada
        while (true) { // el bucle solo sale por return o throw
            try {
                ResponseEntity<String> response = exchange(path); // una llamada HTTP
                if (response.getStatusCode().value() == 404) { // M3 aun no tiene el calculo (llega de forma asincrona)
                    if (notFoundRetries < NOT_FOUND_RETRIES) { // quedan reintentos permitidos
                        notFoundRetries++; // consume uno
                        sleepBeforeRetry(); // espera acotada antes de repetir la solicitud
                        continue; // repite la solicitud
                    }
                    throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE,
                            "Finance calculation is not available yet; retry later"); // agotados los reintentos
                }
                return response.getBody() == null ? "" : response.getBody(); // 200 con cuerpo
            } catch (ResourceAccessException e) { // fallo de red antes de llegar a respuesta
                if (!socketRetried && isRetryableConnectionFailure(e)) { // solo ConnectException y una vez
                    socketRetried = true; // marca el unico reintento de socket
                    continue; // reintenta de inmediato, sin espera
                }
                throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance service is unreachable"); // timeout o sin conexion
            }
        }
    }

    private static void sleepBeforeRetry() { // espera documentada de 300 ms entre reintentos de 404
        try {
            Thread.sleep(NOT_FOUND_RETRY_DELAY_MS); // pausa acotada
        } catch (InterruptedException e) { // el hilo fue interrumpido
            Thread.currentThread().interrupt(); // conserva la interrupcion
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE,
                    "Finance calculation retry was interrupted"); // falla de forma controlada
        }
    }

    static boolean isRetryableConnectionFailure(Throwable failure) { // decide el reintento de socket: unicamente conexion rechazada
        Throwable current = failure; // recorre la cadena de causas de la excepcion
        while (current != null) { // hasta llegar al fondo de la cadena
            if (current instanceof ConnectException) { // conexion rechazada: el unico caso reintetable
                return true; // autoriza el reintento
            }
            current = current.getCause(); // sube una causa
        }
        return false; // timeouts (SocketTimeoutException) y demas no se reintantan
    }

    private ResponseEntity<String> exchange(String path) { // mapea los estados de error de M3 a excepciones de dominio
        RestClient.RequestHeadersSpec<?> request = restClient.post().uri(path) // POST hacia modulo 3
                .header("Accept", "application/json"); // header aceptado por contrato
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY); // correlation id opcional de la peticion origen
        if (correlationId != null && !correlationId.isBlank()) { // solo si hay contexto HTTP
            request = request.header("X-Correlation-Id", correlationId); // lo propaga si existe
        }
        return request.exchange((req, response) -> { // exchange controla cada estado sin catch ciego
            int status = response.getStatusCode().value(); // codigo http de la respuesta
            if (status == 404) { // informacion aun no registrada en M3: el bucle decide reintentos
                return ResponseEntity.status(404).build(); // sin cuerpo
            }
            if (status == 400 || status == 401 || status == 403) { // error de integracion: alerta sin reintento
                throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Finance module rejected the calculated value request"); // 500 interno
            }
            if (status < 200 || status >= 300) { // 422, 500, 503 u otro estado
                throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance service returned status " + status); // calculo no disponible
            }
            return ResponseEntity.status(status).body(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)); // cuerpo en utf-8
        });
    }

    private static CalculatedValue parse(String body) { // extrae los cuatro montos sin sumar ni recomponer nada
        if (body == null || body.isBlank()) { // 200 sin cuerpo no aporta datos
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance response body is empty"); // respuesta invalida
        }
        JsonNode root; // arbol json de la respuesta
        try {
            root = MAPPER.readTree(body); // jackson 3 lanza JacksonException, que no es checked
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance response body is not valid json"); // cuerpo ilegible
        }
        BigDecimal rental = parseAmount(root, "rental_amount", false); // alquiler; cero y negativo se rechazan
        BigDecimal insurance = parseAmount(root, "insurance_amount", true); // seguro; cero admitido, negativo no
        BigDecimal deposit = parseAmount(root, "guarantee_deposit_amount", true); // deposito; cero admitido, negativo no
        BigDecimal total = parseAmount(root, "total_amount", false); // total; cero o negativo se rechazan
        return new CalculatedValue(rental, insurance, deposit, total); // los montos tal cual de M3
    }

    private static BigDecimal parseAmount(JsonNode root, String field, boolean allowZero) { // valida un monto sin aritmetica
        JsonNode node = root.path(field); // campo del contrato
        if (!node.isString() || node.stringValue().isBlank()) { // obligatorio y como string decimal
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance response is missing " + field); // campo ausente
        }
        BigDecimal amount; // monto opaco leido tal cual
        try {
            amount = new BigDecimal(node.stringValue().trim()); // solo lectura; M2 no calcula
        } catch (NumberFormatException e) {
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance response " + field + " is not a number"); // no numerico
        }
        int sign = amount.signum(); // signum no es aritmetica de montos
        if (sign < 0 || (sign == 0 && !allowZero)) { // negativo siempre; cero solo si esta permitido
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "Finance response " + field + " is invalid"); // monto invalido
        }
        return amount; // devuelve el monto tal cual
    }
}
