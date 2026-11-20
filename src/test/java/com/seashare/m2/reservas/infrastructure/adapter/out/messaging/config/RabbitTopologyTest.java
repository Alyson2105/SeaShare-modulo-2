package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config;

import com.seashare.m2.disputa.infrastructure.adapter.out.messaging.DisputeMessagingProperties;
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
 * T-02: verifica que la topologia RabbitMQ de M2 sea identica a la de M3:
 * colas quorum con dead-lettering al exchange por defecto, DLQ clasicas y
 * sin exchange DLX propio.
 *
 * <p>Las declaraciones las ejecuta RabbitAdmin al abrir la primera conexion
 * (forzada en {@code setUp} con {@code getQueueProperties}). El tipo de exchange,
 * los argumentos de cola y los bindings se consultan por la API de management
 * del contenedor ({@code rabbitmq:3-management}).</p>
 */
@SpringBootTest
@Import({PostgresTestContainers.class, RabbitTestContainers.class})
class RabbitTopologyTest { // prueba de integracion de la topologia rabbitmq con broker real

    private static final String VHOST = "%2F"; // vhost por defecto codificado para la API de management

    private static final String CUSTOM_DLX = "seashare.reservations.dlx"; // DLX propio que ya no debe declararse

    @Autowired
    private RabbitMQContainer container; // contenedor compartido con management habilitado

    @Autowired
    private AmqpAdmin amqpAdmin; // administrador de declaraciones de spring amqp

    @Autowired
    private MessagingProperties properties; // nombres de topologia de reservas

    @Autowired
    private DisputeMessagingProperties disputeProperties; // nombres de topologia de disputa

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
    void queuesAndDlqsExistWithDeadLetterArguments() throws Exception { // las colas son quorum y las DLQ clasicas
        assertWorkQueue(properties.getQueueStatus()); // cola de estado
        assertWorkQueue(properties.getQueueInfo()); // cola de informacion
        assertWorkQueue(disputeProperties.getQueueDispute()); // cola de disputa

        assertDlq(properties.dlqStatus()); // DLQ de estado
        assertDlq(properties.dlqInfo()); // DLQ de informacion
        assertDlq(disputeProperties.dlqDispute()); // DLQ de disputa
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
    void disputeQueueBoundToDisputeUpdatedRoutingKey() throws Exception { // binding reservation.dispute.updated
        JsonNode bindings = apiGet("/api/bindings/" + VHOST + "/e/" + properties.getExchange()
                + "/q/" + disputeProperties.getQueueDispute());

        assertThat(hasBindingWithRoutingKey(bindings, disputeProperties.getRoutingKeyDisputeUpdated())).isTrue();
    }

    @Test
    void customDeadLetterExchangeIsNotDeclared() throws Exception { // M2 ya no declara su propio DLX
        JsonNode exchanges = apiGet("/api/exchanges/" + VHOST);

        for (JsonNode exchange : exchanges) {
            assertThat(exchange.get("name").asText())
                    .as("M2 no debe declarar su propio DLX")
                    .isNotEqualTo(CUSTOM_DLX);
        }
    }

    private void assertWorkQueue(String queueName) throws Exception { // valida cola quorum con DLX por defecto
        JsonNode queue = apiGet("/api/queues/" + VHOST + "/" + queueName);

        assertThat(queue.get("name").asText()).isEqualTo(queueName);
        assertThat(queue.get("type").asText()).as(queueName).isEqualTo("quorum");
        JsonNode arguments = queue.get("arguments");
        assertThat(arguments.get("x-dead-letter-exchange").asText()).as(queueName).isEqualTo("");
        assertThat(arguments.get("x-dead-letter-routing-key").asText()).as(queueName)
                .isEqualTo(queueName + ".dlq");
    }

    private void assertDlq(String queueName) throws Exception { // valida DLQ durable clasica
        JsonNode queue = apiGet("/api/queues/" + VHOST + "/" + queueName);

        assertThat(queue.get("name").asText()).isEqualTo(queueName);
        assertThat(queue.get("type").asText()).as(queueName).isEqualTo("classic");
    }

    private boolean hasBindingWithRoutingKey(JsonNode bindings, String routingKey) { // busca binding por routing key
        for (JsonNode binding : bindings) {
            if (binding.has("routing_key") && binding.get("routing_key").asText().equals(routingKey)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode apiGet(String path) throws Exception { // GET autenticado contra la API de management
        String credentials = container.getAdminUsername() + ":" + container.getAdminPassword();
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(container.getHttpUrl() + path))
                .header("Authorization", "Basic " + basicAuth)
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("GET %s devolvio %d: %s", path, response.statusCode(), response.body())
                .isEqualTo(200);
        return objectMapper.readTree(response.body());
    }
}
