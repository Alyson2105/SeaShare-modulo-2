package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.BatchEstimateResult;
import com.seashare.m2.reservas.application.port.out.BatchEstimatePort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "http") // activos solo en modo http
public class HttpBatchEstimateAdapter implements BatchEstimatePort { // cliente HTTP real de estimacion en lote (CU-11)

    private static final Logger log = LoggerFactory.getLogger(HttpBatchEstimateAdapter.class); // log de bloques fallidos

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3 de la respuesta de M3

    private static final String BATCH_PATH = "/api/v1/estimates/batch"; // ruta del contrato CU-11

    private static final int MAX_IDS_PER_REQUEST = 50; // limite de boat_ids por llamada a M3

    private final RestClient restClient; // cliente con timeouts de lote ya configurados

    public HttpBatchEstimateAdapter(@Qualifier("batchFinanceRestClient") RestClient restClient) { // inyecta el cliente
        this.restClient = restClient; // asigna cliente
    }

    @Override
    public BatchEstimateResult getBatchEstimates(List<UUID> vesselIds) { // estima todas las embarcaciones pedidas
        List<UUID> uniqueIds = new ArrayList<>(new LinkedHashSet<>(vesselIds)); // deduplica preservando el orden
        if (uniqueIds.isEmpty()) { // lista vacia no llama a M3
            return new BatchEstimateResult(Map.of(), List.of()); // resultado vacio
        }
        Map<UUID, BigDecimal> estimates = new LinkedHashMap<>(); // totales validos por embarcacion
        Set<UUID> unavailable = new LinkedHashSet<>(); // embarcaciones sin estimacion
        for (int from = 0; from < uniqueIds.size(); from += MAX_IDS_PER_REQUEST) { // bloques de 50 en secuencia
            List<UUID> block = uniqueIds.subList(from, Math.min(from + MAX_IDS_PER_REQUEST, uniqueIds.size())); // un bloque
            try {
                String body = post(BATCH_PATH, jsonBody(block)); // POST sin reintentos
                mergeBlock(block, body, estimates, unavailable); // junta el resultado del bloque
            } catch (ReservationRuleException e) { // bloque fallido: error o timeout
                log.warn("Batch estimate block failed with code {}", e.getCode(), e); // aviso con el codigo
                unavailable.addAll(block); // todo el bloque queda no disponible
            } catch (RuntimeException e) { // cualquier otro fallo del bloque
                log.warn("Batch estimate block failed with unexpected error", e); // aviso sin codigo
                unavailable.addAll(block); // todo el bloque queda no disponible
            }
        }
        estimates.keySet().removeAll(unavailable); // nadie puede estar disponible y no disponible a la vez
        return new BatchEstimateResult(estimates, List.copyOf(unavailable)); // resultado consolidado
    }

    private String post(String path, String json) { // POST de un bloque con cero reintentos
        try {
            return exchange(path, json); // una sola llamada
        } catch (ResourceAccessException e) { // timeout o sin conexion
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance service is unreachable"); // el bloque falla completo
        }
    }

    private String exchange(String path, String json) { // mapea los estados de error de M3 a excepciones de dominio
        return restClient.post().uri(path) // POST hacia modulo 3
                .contentType(MediaType.APPLICATION_JSON) // cuerpo json
                .body(json) // payload con solo boat_ids
                .exchange((request, response) -> { // exchange controla cada estado sin catch ciego
                    int status = response.getStatusCode().value(); // codigo http de la respuesta
                    if (status == 400) { // parametros rechazados por M3
                        throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "Finance module rejected the estimate parameters"); // el bloque falla
                    }
                    if (status == 401 || status == 403) { // acceso denegado en M3
                        throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Finance module rejected the request"); // el bloque falla
                    }
                    if (status < 200 || status >= 300) { // 422, 500, 503 u otro estado
                        throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance service returned status " + status); // el bloque falla
                    }
                    return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8); // cuerpo en utf-8
                });
    }

    private static void mergeBlock(List<UUID> block, String body, Map<UUID, BigDecimal> estimates, Set<UUID> unavailable) { // junta un bloque al resultado
        JsonNode root = parse(body); // json ilegible o vacio falla el bloque completo
        Map<UUID, BigDecimal> totals = new HashMap<>(); // totales validos devueltos por M3
        for (JsonNode node : root.path("estimates")) { // entradas con estimacion
            UUID id = parseUuid(node.path("boat_id")); // identificador devuelto
            BigDecimal total = parseTotal(node.path("estimated_total")); // total o null si es invalido
            if (id != null && total != null) { // solo los montos validos se aceptan
                totals.put(id, total); // guarda el monto tal cual
            }
        }
        Set<UUID> rejected = new HashSet<>(); // ids que M3 marco como no disponibles
        for (JsonNode node : root.path("unavailable")) { // entradas rechazadas por M3
            UUID id = parseUuid(node.path("boat_id")); // identificador devuelto
            if (id != null) { // el motivo se ignora
                rejected.add(id); // marca el id
            }
        }
        for (UUID id : block) { // todos los ids pedidos en este bloque
            BigDecimal total = totals.get(id); // total devuelto para este id
            if (total != null && !rejected.contains(id)) { // disponible y con monto valido
                estimates.put(id, total); // agrega la estimacion
            } else { // rechazado, sin monto o ausente de la respuesta
                unavailable.add(id); // queda como no disponible
            }
        }
    }

    private static JsonNode parse(String body) { // convierte el cuerpo a arbol json con corte seguro
        if (body == null || body.isBlank()) { // 200 sin cuerpo no aporta datos
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response body is empty"); // falla el bloque
        }
        try {
            return MAPPER.readTree(body); // jackson 3 lanza JacksonException, que no es checked
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "Finance response body is not valid json"); // cuerpo ilegible
        }
    }

    private static BigDecimal parseTotal(JsonNode node) { // total valido o null sin calcular nada
        if (!node.isString()) { // contrato: string decimal
            return null; // formato inesperado
        }
        String text = node.stringValue().trim(); // sin espacios alrededor
        if (text.isEmpty()) { // vacio no sirve
            return null; // sin monto
        }
        try {
            BigDecimal total = new BigDecimal(text); // solo lectura; M2 no calcula
            return total.signum() > 0 ? total : null; // cero o negativo se rechaza con signum
        } catch (NumberFormatException e) {
            return null; // ilegible
        }
    }

    private static UUID parseUuid(JsonNode node) { // uuid devuelto por M3 o null
        if (!node.isString()) { // no es texto
            return null; // se ignora la entrada
        }
        try {
            return UUID.fromString(node.stringValue()); // traduce el id
        } catch (IllegalArgumentException e) {
            return null; // no es un uuid valido
        }
    }

    private static String jsonBody(List<UUID> block) { // arma el body con solo boat_ids
        List<String> ids = block.stream().map(UUID::toString).toList(); // ids como texto
        return MAPPER.writeValueAsString(Map.of("boat_ids", ids)); // unica clave del contrato
    }
}
