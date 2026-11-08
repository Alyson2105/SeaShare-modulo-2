package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nombres de la topologia RabbitMQ (T-02).
 *
 * <p>Centraliza exchange, DLX, routing keys y colas en application.yaml para que
 * la topologia (RabbitTopologyConfig) y el outbox (OutboxAdapter) lean los mismos
 * valores y no diverjan. Los valores por defecto coinciden con el contrato de
 * CU-14 y con los nombres ya usados por OutboxAdapter.</p>
 */
@Component
@ConfigurationProperties(prefix = "m2.messaging")
public class MessagingProperties { // propiedades de configuracion de la topologia rabbitmq

    private String exchange = "seashare.reservations"; // exchange topic principal de eventos de reserva

    private String deadLetterExchange = "seashare.reservations.dlx"; // exchange directo para dead-lettering

    private String routingKeyStatusChanged = "reservation.status.changed"; // routing key del evento de cambio de estado

    private String routingKeyInfoProvided = "reservation.info.provided"; // routing key del evento de informacion de reserva

    private String queueStatus = "finance.reservation-status.v1"; // cola de M3 para cambios de estado

    private String queueInfo = "finance.reservation-info.v1"; // cola de M3 para informacion de reserva

    public String getExchange() { // obtiene el exchange principal
        return exchange; // retorna exchange
    }

    public void setExchange(String exchange) { // asigna el exchange principal
        this.exchange = exchange; // establece exchange
    }

    public String getDeadLetterExchange() { // obtiene el exchange de dead-lettering
        return deadLetterExchange; // retorna deadLetterExchange
    }

    public void setDeadLetterExchange(String deadLetterExchange) { // asigna el exchange de dead-lettering
        this.deadLetterExchange = deadLetterExchange; // establece deadLetterExchange
    }

    public String getRoutingKeyStatusChanged() { // obtiene la routing key de cambio de estado
        return routingKeyStatusChanged; // retorna routingKeyStatusChanged
    }

    public void setRoutingKeyStatusChanged(String routingKeyStatusChanged) { // asigna la routing key de cambio de estado
        this.routingKeyStatusChanged = routingKeyStatusChanged; // establece routingKeyStatusChanged
    }

    public String getRoutingKeyInfoProvided() { // obtiene la routing key de informacion de reserva
        return routingKeyInfoProvided; // retorna routingKeyInfoProvided
    }

    public void setRoutingKeyInfoProvided(String routingKeyInfoProvided) { // asigna la routing key de informacion
        this.routingKeyInfoProvided = routingKeyInfoProvided; // establece routingKeyInfoProvided
    }

    public String getQueueStatus() { // obtiene la cola de cambios de estado
        return queueStatus; // retorna queueStatus
    }

    public void setQueueStatus(String queueStatus) { // asigna la cola de cambios de estado
        this.queueStatus = queueStatus; // establece queueStatus
    }

    public String getQueueInfo() { // obtiene la cola de informacion de reserva
        return queueInfo; // retorna queueInfo
    }

    public void setQueueInfo(String queueInfo) { // asigna la cola de informacion de reserva
        this.queueInfo = queueInfo; // establece queueInfo
    }

    public String dlqStatus() { // calcula el nombre de la DLQ de la cola de estado
        return queueStatus + ".dlq"; // sufijo .dlq por convencion
    }

    public String dlqInfo() { // calcula el nombre de la DLQ de la cola de informacion
        return queueInfo + ".dlq"; // sufijo .dlq por convencion
    }
}
