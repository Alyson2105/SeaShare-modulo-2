package com.seashare.m2.reservas.infrastructure.adapter.out.messaging;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.EventPublishException;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import com.seashare.m2.shared.infrastructure.adapter.in.web.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * T-22/T-04: pruebas unitarias del publicador AMQP: header Message-Id,
 * persistencia, propagacion del correlation id desde MDC y manejo de nack.
 */
@ExtendWith(MockitoExtension.class)
class RabbitEventPublisherAdapterTest { // pruebas unitarias del publicador amqp

    @Mock
    private RabbitTemplate rabbitTemplate; // plantilla amqp simulada

    private RabbitEventPublisherAdapter adapter; // publicador bajo prueba

    private Message capturedMessage; // sobre amqp capturado

    private CorrelationData capturedCorrelation; // correlacion del confirm capturada

    private boolean brokerAck = true; // resultado del confirm devuelto por el broker

    @BeforeEach
    void setUp() { // configura la plantilla para capturar el sobre y responder el confirm
        adapter = new RabbitEventPublisherAdapter(rabbitTemplate, new MessagingProperties(), 100); // timeout 100 ms
        doAnswer(invocation -> { // simula la publicacion con confirm
            capturedMessage = invocation.getArgument(2); // captura el sobre enviado
            capturedCorrelation = invocation.getArgument(3); // captura la correlacion
            capturedCorrelation.getFuture() // completa el futuro del confirm
                    .complete(new CorrelationData.Confirm(brokerAck, brokerAck ? null : "rechazado")); // ack o nack
            return null; // firma void
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @AfterEach
    void cleanUp() { // limpia MDC y capturas
        MDC.remove(CorrelationIdFilter.MDC_KEY); // sin residuos de correlation id
        capturedMessage = null; // suelta el sobre
        capturedCorrelation = null; // suelta la correlacion
        brokerAck = true; // vuelve a ack por defecto
    }

    @Test
    void publicaConMessageIdPersistenteYPropagaCorrelationIdDeMdc() { // headers T-04 + T-22
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-456"); // correlation id en el MDC de la peticion

        adapter.publish(pendingMessage()); // publica

        MessageProperties properties = capturedMessage.getMessageProperties(); // propiedades enviadas
        String eventId = properties.getMessageId(); // Message-Id = event_id
        assertThat(UUID.fromString(eventId)).isNotNull(); // es un UUID valido
        assertThat(properties.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON); // application/json
        assertThat(properties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT); // persistente
        assertThat(properties.getHeaders().get(CorrelationIdFilter.HEADER)).isEqualTo("corr-456"); // header AMQP
        assertThat(capturedCorrelation.getId()).isEqualTo(eventId); // correlacion del confirm = event id
    }

    @Test
    void sinMdcNoAgregaElHeaderDeCorrelation() { // sin contexto HTTP no hay header de correlacion
        adapter.publish(pendingMessage()); // publica sin MDC

        assertThat(capturedMessage.getMessageProperties().getHeaders()) // headers enviados
                .doesNotContainKey(CorrelationIdFilter.HEADER); // sin X-Correlation-Id
    }

    @Test
    void nackDelBrokerLanzaEventPublishException() { // sin ack el relay agenda reintento
        brokerAck = false; // el broker rechaza

        assertThatThrownBy(() -> adapter.publish(pendingMessage())) // publica
                .isInstanceOf(EventPublishException.class) // error clasificado
                .hasMessageContaining("rechazado"); // motivo del nack
    }

    private PendingOutboxMessage pendingMessage() { // mensaje pendiente de prueba
        return new PendingOutboxMessage( // record de prueba
                UUID.randomUUID(), // id de fila
                UUID.randomUUID(), // event id
                "AMQP", // destino amqp
                "seashare.reservations", // exchange
                "reservation.status.changed", // routing key
                "{\"reservation_id\":\"" + UUID.randomUUID() + "\"}", // payload
                Instant.parse("2026-11-07T00:00:00Z"), // creado
                0 // sin intentos
        );
    }
}
