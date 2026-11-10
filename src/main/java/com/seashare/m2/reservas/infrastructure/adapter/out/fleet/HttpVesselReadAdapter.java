package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // activo solo en modo http
public class HttpVesselReadAdapter implements VesselReadPort { // lectura de resumenes de flota para las vistas (CU-20 y CU-21)

    private static final ObjectMapper MAPPER = new JsonMapper(); // mapeador jackson 3

    private static final String VESSELS_PATH = "/api/v1/vessels/"; // ruta individual del contrato CU-09

    private static final long TOTAL_BUDGET_MS = 700; // presupuesto total de la consulta en lote

    private static final int MAX_PARALLELISM = 4; // llamadas simultaneas maximas

    private final RestClient restClient; // cliente con timeouts del modulo 1

    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLELISM, runnable -> { // pool acotado y daemon
        Thread thread = new Thread(runnable, "vessel-read"); // nombre para diagnostico
        thread.setDaemon(true); // no bloquea el apagado
        return thread; // retorna el hilo
    });

    public HttpVesselReadAdapter(@Qualifier("fleetRestClient") RestClient restClient) { // inyecta el cliente de flota
        this.restClient = restClient; // asigna cliente
    }

    @Override
    public Map<UUID, VesselSummary> getSummaries(Collection<UUID> vesselIds) { // consulta en paralelo con presupuesto total
        Set<UUID> distinct = new LinkedHashSet<>(); // deduplica ids preservando orden
        if (vesselIds != null) { // tolera coleccion nula
            for (UUID vesselId : vesselIds) { // recorre ids solicitados
                if (vesselId != null) { // ignora nulos
                    distinct.add(vesselId); // agrega una sola vez
                }
            }
        }
        if (distinct.isEmpty()) { // sin ids no hay llamadas remotas
            return Map.of(); // retorno inmediato
        }

        Map<UUID, CompletableFuture<Optional<VesselSummary>>> pending = new HashMap<>(); // futuros por embarcacion
        for (UUID vesselId : distinct) { // lanza una consulta por embarcacion distinta
            pending.put(vesselId, CompletableFuture.supplyAsync(() -> fetchOne(vesselId), executor)); // ejecuta en el pool acotado
        }

        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TOTAL_BUDGET_MS); // instante limite unico
        Map<UUID, VesselSummary> resolved = new HashMap<>(); // resultado parcial
        for (Map.Entry<UUID, CompletableFuture<Optional<VesselSummary>>> entry : pending.entrySet()) { // recolecta respetando el limite global
            long remaining = deadline - System.nanoTime(); // tiempo restante
            if (remaining <= 0) { // presupuesto agotado
                entry.getValue().cancel(true); // cancela lo pendiente
                continue; // deja la embarcacion sin datos
            }
            try {
                entry.getValue().get(remaining, TimeUnit.NANOSECONDS) // espera acotada
                        .ifPresent(summary -> resolved.put(entry.getKey(), summary)); // agrega solo respuestas utiles
            } catch (TimeoutException e) {
                entry.getValue().cancel(true); // corta la consulta lenta
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // conserva la interrupcion
                break; // abandona el resto
            } catch (Exception e) {
                entry.getValue().cancel(true); // cualquier fallo deja el elemento sin datos
            }
        }
        return Map.copyOf(resolved); // mapa inmutable con lo resuelto
    }

    @Override
    public VesselDetail getDetail(UUID vesselId) { // consulta estricta de la ficha completa
        String body;
        try {
            body = restClient.get().uri(VESSELS_PATH + vesselId).retrieve().body(String.class); // GET del contrato CU-09
        } catch (HttpClientErrorException.NotFound e) {
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_FOUND, "Vessel not found in fleet module"); // 404 sin reintento
        } catch (HttpClientErrorException.BadRequest e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Vessel id rejected by fleet module"); // 400 sin reintento
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Fleet module rejected the service token"); // alerta interna
        } catch (HttpClientErrorException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service returned status " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service is unreachable"); // timeout, conexion o 5xx
        }
        return parseDetail(vesselId, body); // payload validado
    }

    private static VesselDetail parseDetail(UUID vesselId, String body) { // extrae la ficha completa sin valores por defecto
        if (body == null || body.isBlank()) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is empty"); // fail-safe
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(body); // arbol json
        } catch (JacksonException e) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response body is not valid json"); // ilegible
        }
        String name = text(root, "name"); // nombre comercial obligatorio para la vista
        if (name == null) {
            throw new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet response is missing name"); // payload incompleto
        }
        String imageUrl = text(root, "image_url"); // contrato individual puede no traer imagen
        if (imageUrl == null) {
            imageUrl = text(root, "photo_url"); // respaldo del contrato de catalogo
        }
        JsonNode port = root.path("port"); // objeto puerto
        List<String> amenities = new ArrayList<>(); // servicios incluidos
        for (JsonNode amenity : root.path("amenities")) { // recorre el arreglo
            if (amenity.isString() && !amenity.stringValue().isBlank()) {
                amenities.add(amenity.stringValue()); // agrega textos utiles
            }
        }
        JsonNode owner = root.path("owner"); // objeto propietario
        return new VesselDetail(
                vesselId,
                name,
                text(root, "type"),
                imageUrl,
                text(port, "name"),
                null, // la ciudad no existe en el contrato de M1
                text(port, "timezone"),
                List.copyOf(amenities),
                uuid(text(owner, "owner_id")),
                text(owner, "validated_name"));
    }

    @PreDestroy
    public void shutdown() { // libera el pool al apagar el contexto
        executor.shutdownNow(); // interrumpe tareas pendientes
    }

    private Optional<VesselSummary> fetchOne(UUID vesselId) { // consulta individual fail-safe
        try {
            String body = restClient.get().uri(VESSELS_PATH + vesselId).retrieve().body(String.class); // GET del contrato CU-09
            if (body == null || body.isBlank()) { // respuesta sin cuerpo no aporta datos
                return Optional.empty(); // elemento no disponible
            }
            JsonNode root = MAPPER.readTree(body); // arbol json
            String name = text(root, "name"); // nombre comercial
            String imageUrl = text(root, "image_url"); // contrato individual no siempre trae imagen
            if (imageUrl == null) { // respaldo con el nombre del contrato de catalogo
                imageUrl = text(root, "photo_url"); // url de foto opcional
            }
            String portName = text(root.path("port"), "name"); // puerto de salida
            JsonNode owner = root.path("owner"); // objeto propietario
            UUID ownerId = uuid(text(owner, "owner_id")); // id del propietario si es valido
            String ownerName = text(owner, "validated_name"); // nombre validado
            return Optional.of(new VesselSummary(vesselId, name, imageUrl, portName, ownerId, ownerName)); // resumen disponible
        } catch (JacksonException e) {
            return Optional.empty(); // cuerpo ilegible: fail-safe por elemento
        } catch (RuntimeException e) {
            return Optional.empty(); // 4xx/5xx/timeout: fail-safe por elemento
        }
    }

    private static String text(JsonNode node, String field) { // lee un texto opcional
        JsonNode value = node.path(field); // nodo del campo
        if (!value.isString()) { // solo acepta texto
            return null; // ausente o tipo inesperado
        }
        String text = value.stringValue(); // valor textual
        return text.isBlank() ? null : text; // normaliza vacios a nulo
    }

    private static UUID uuid(String value) { // parsea uuid opcional
        if (value == null) { // sin valor
            return null; // nulo
        }
        try {
            return UUID.fromString(value); // parsea identificador
        } catch (IllegalArgumentException e) {
            return null; // formato invalido se ignora
        }
    }
}
