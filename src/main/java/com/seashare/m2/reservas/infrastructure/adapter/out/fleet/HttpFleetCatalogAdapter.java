package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.CatalogVessel;
import com.seashare.m2.reservas.application.dto.VesselCatalogPage;
import com.seashare.m2.reservas.application.port.out.FleetCatalogPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // activo solo en modo http
public class HttpFleetCatalogAdapter implements FleetCatalogPort { // catalogo real de M1 (CU-09 operacion en lote)

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3

    private static final String CATALOG_PATH = "/api/v1/vessels"; // contrato de CU-09 (operacion 2)

    private final RestClient restClient; // cliente con timeouts y token de servicio

    public HttpFleetCatalogAdapter(@Qualifier("fleetRestClient") RestClient restClient) {
        this.restClient = restClient; // asigna cliente
    }

    @Override
    public VesselCatalogPage search(String query, int page, int size) { // GET paginado al catalogo
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(CATALOG_PATH).queryParam("page", page).queryParam("size", size);
                        if (query != null && !query.isBlank()) {
                            uriBuilder.queryParam("q", query.trim());
                        }
                        return uriBuilder.build();
                    })
                    .exchange((request, result) -> ResponseEntity
                            .status(result.getStatusCode().value())
                            .body(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)));
            int status = response.getStatusCode().value();
            if (status < 200 || status >= 300) { // 4xx y 5xx de M1: catalogo no disponible
                throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE,
                        "Fleet catalog returned status " + status);
            }
            return parse(response.getBody(), page, size);
        } catch (ReservationRuleException e) { // error ya clasificado
            throw e;
        } catch (RuntimeException e) { // timeout, conexion o respuesta ilegible
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE,
                    "Fleet catalog is unreachable"); // 503 fail-safe (CU-01)
        }
    }

    private VesselCatalogPage parse(String body, int page, int size) { // lee el contrato de CU-09 en lote
        if (body == null || body.isBlank()) { // sin cuerpo no hay catalogo
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet catalog body is empty");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(body); // arbol json
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet catalog body is not valid json");
        }
        JsonNode pagination = root.path("pagination");
        List<CatalogVessel> vessels = new ArrayList<>();
        for (JsonNode item : root.path("vessels")) { // cada tarjeta del catalogo
            UUID id = uuid(text(item, "vessel_id"));
            if (id == null) { // sin identificador no se puede reservar
                continue;
            }
            JsonNode port = item.path("port");
            vessels.add(new CatalogVessel(
                    id,
                    text(item, "name"),
                    text(item, "type"),
                    text(item, "photo_url"),
                    text(port, "name"),
                    decimal(port, "latitude"),
                    decimal(port, "longitude"),
                    text(port, "timezone"),
                    integer(item, "max_capacity"),
                    bool(item, "captain_included")));
        }
        return new VesselCatalogPage(
                List.copyOf(vessels),
                integer(pagination, "page") == 0 ? page : integer(pagination, "page"),
                integer(pagination, "page_size") == 0 ? size : integer(pagination, "page_size"),
                longValue(pagination, "total_items"),
                integer(pagination, "total_pages"));
    }

    private static String text(JsonNode node, String field) { // texto opcional
        JsonNode value = node.path(field);
        return value.isString() && !value.stringValue().isBlank() ? value.stringValue() : null;
    }

    private static int integer(JsonNode node, String field) { // entero opcional, cero si ausente
        JsonNode value = node.path(field);
        return value.isNumber() ? value.intValue() : 0;
    }

    private static long longValue(JsonNode node, String field) { // entero largo opcional
        JsonNode value = node.path(field);
        return value.isNumber() ? value.longValue() : 0L;
    }

    private static Double decimal(JsonNode node, String field) { // decimal opcional
        JsonNode value = node.path(field);
        return value.isNumber() ? value.doubleValue() : null;
    }

    private static boolean bool(JsonNode node, String field) { // booleano opcional
        JsonNode value = node.path(field);
        return value.isBoolean() && value.booleanValue();
    }

    private static UUID uuid(String value) { // uuid opcional
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
