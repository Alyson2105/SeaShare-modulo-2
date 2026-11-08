package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config;

import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.RabbitTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-02: verifica que la topologia RabbitMQ exista al arrancar la aplicacion
 * contra un broker real (contenedor compartido, no uno por prueba).
 *
 * <p>Las declaraciones las ejecuta RabbitAdmin al abrir la primera conexion
 * (forzada en {@code setUp} con {@code getQueueProperties}). El tipo de exchange
 * y los bindings se consultan por la API de management del contenedor
 * ({@code rabbitmq:3-management}).</p>
 */
@SpringBootTest
@Import({PostgresTestContainers.class, RabbitTestContainers.class})
class RabbitTopologyTest { // prueba de integracion de la topologia rabbitmq con broker real

    private static final String VHOST = "%2F"; // vhost por defecto codificado para la API de management

    @Autowired
    private RabbitMQContainer container; // contenedor compartido con management habilitado

    @Autowired
    private AmqpAdmin amqpAdmin; // administrador de declaraciones de spring amqp

    @Autowired
    private MessagingProperties properties; // nombres de topologia leidos de application.yaml

    private final JsonMapper objectMapper = new JsonMapper(); // mapeador jackson 3 para respuestas json

    @BeforeEach
    void declareTopology() { // fuerza la primera conexion y con ella las declaraciones de RabbitAdmin
        amqpAdmin.getQueueProperties(properties.getQueueStatus()); // abre conexion y dispara declaraciones
    }

    @Test
    void exchangeExistsAsDurableTopic() throws Exception { // el exchange principal es topic y durable
        JsonNode exchange = apiGet("/api/exchanges/" + VHOST + "/" + properties.getExchange());

        assertThat(exchange.get("type").asText()).isEqualTo("topic"); // verifica tipo topic
        assertThat(exchange.get("durable").asBoolean()).isTrue(); // verifica durable
    }

    @Test
    void queuesAndDlqsExistWithDeadLetterArguments() throws Exception { // las 4 colas existen con sus argumentos dlq
        JsonNode statusQueue = apiGet("/api/queues/" + VHOST + "/" + properties.getQueueStatus());
        assertDeadLetterArguments(statusQueue, properties.getQueueStatus()); // cola de estado con DLX

        JsonNode infoQueue = apiGet("/api/queues/" + VHOST + "/" + properties.getQueueInfo());
        assertDeadLetterArguments(infoQueue, properties.getQueueInfo()); // cola de informacion con DLX

        JsonNode statusDlq = apiGet("/api/queues/" + VHOST + "/" + properties.dlqStatus());
        assertThat(statusDlq.get("name").asText()).isEqualTo(properties.dlqStatus()); // DLQ de estado existe

        JsonNode infoDlq = apiGet("/api/queues/" + VHOST + "/" + properties.dlqInfo());
        assertThat(infoDlq.get("name").asText()).isEqualTo(properties.dlqInfo()); // DLQ de informacion existe
    }

    @Test
    void statusQueueBoundToStatusChangedRoutingKey() throws Exception { // binding reservation.status.changed
        JsonNode bindings = apiGet("/api/bindings/" + VHOST + "/e/" + properties.getExchange()
                + "/q/" + properties.getQueueStatus());

        assertThat(hasBindingWithRoutingKey(bindings, properties.getRoutingKeyStatusChanged())).isTrue();
    }

    @Test
    void infoQueueBoundToInfoProvidedRoutingKey() throws Exception { // binding reservation.info.provided
        JsonNode bindings = apiGet("/api/bindings/" + VHOST + "/e/" + properties.getExchange()
                + "/q/" + properties.getQueueInfo());

        assertThat(hasBindingWithRoutingKey(bindings, properties.getRoutingKeyInfoProvided())).isTrue();
    }

    @Test
    void dlqsBoundToDeadLetterExchange() throws Exception { // cada DLQ esta ligada al DLX por su routing key
        JsonNode statusDlqBindings = apiGet("/api/bindings/" + VHOST + "/e/" + properties.getDeadLetterExchange()
                + "/q/" + properties.dlqStatus());
        assertThat(hasBindingWithRoutingKey(statusDlqBindings, properties.getQueueStatus())).isTrue();

        JsonNode infoDlqBindings = apiGet("/api/bindings/" + VHOST + "/e/" + properties.getDeadLetterExchange()
                + "/q/" + properties.dlqInfo());
        assertThat(hasBindingWithRoutingKey(infoDlqBindings, properties.getQueueInfo())).isTrue();
    }

    private void assertDeadLetterArguments(JsonNode queue, String queueName) { // valida argumentos de dead-lettering
        assertThat(queue.get("name").asText()).isEqualTo(queueName); // verifica nombre de cola
        JsonNode arguments = queue.get("arguments"); // argumentos x-dead-letter-* declarados
        assertThat(arguments.get("x-dead-letter-exchange").asText()) // verifica DLX configurado
                .isEqualTo(properties.getDeadLetterExchange());
        assertThat(arguments.get("x-dead-letter-routing-key").asText()) // verifica rk hacia la DLQ
                .isEqualTo(queueName);
    }

    private boolean hasBindingWithRoutingKey(JsonNode bindings, String routingKey) { // busca binding por routing key
        for (JsonNode binding : bindings) { // recorre los bindings devueltos por management
            if (binding.has("routing_key") && binding.get("routing_key").asText().equals(routingKey)) {
                return true; // encontro el binding esperado
            }
        }
        return false; // ningun binding coincide
    }

    private JsonNode apiGet(String path) throws Exception { // GET autenticado contra la API de management
        String credentials = container.getAdminUsername() + ":" + container.getAdminPassword(); // credenciales del contenedor
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)); // codificacion basic
        HttpRequest request = HttpRequest.newBuilder(URI.create(container.getHttpUrl() + path)) // url completa
                .header("Authorization", "Basic " + basicAuth) // cabecera de autenticacion
                .GET().build(); // metodo GET
        HttpResponse<String> response = HttpClient.newHttpClient() // cliente http del jdk
                .send(request, HttpResponse.BodyHandlers.ofString()); // ejecuta la peticion
        assertThat(response.statusCode()) // management debe responder 200
                .as("GET %s devolvio %d: %s", path, response.statusCode(), response.body())
                .isEqualTo(200);
        return objectMapper.readTree(response.body()); // parsea la respuesta json
    }
}
