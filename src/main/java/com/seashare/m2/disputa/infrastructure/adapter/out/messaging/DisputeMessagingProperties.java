package com.seashare.m2.disputa.infrastructure.adapter.out.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nombres de la topologia RabbitMQ propia del contexto disputa (T-18c).
 *
 * <p>Se mantiene fuera de {@code MessagingProperties} de reservas para que
 * {@code disputa} no dependa de ese contexto. Los valores por defecto coinciden
 * con el contrato de CU-18 y con el exchange compartido de M3 (D-17).</p>
 */
@Component
@ConfigurationProperties(prefix = "m2.dispute.messaging")
public class DisputeMessagingProperties {

    private String exchange = "seashare.reservations"; // exchange topic compartido hacia M3

    private String deadLetterExchange = "seashare.reservations.dlx"; // exchange directo de dead-lettering

    private String routingKeyDisputeUpdated = "reservation.dispute.updated"; // routing key de CU-18

    private String queueDispute = "finance.guarantee-dispute.v1"; // cola de M3 para disputas

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    public void setDeadLetterExchange(String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    public String getRoutingKeyDisputeUpdated() {
        return routingKeyDisputeUpdated;
    }

    public void setRoutingKeyDisputeUpdated(String routingKeyDisputeUpdated) {
        this.routingKeyDisputeUpdated = routingKeyDisputeUpdated;
    }

    public String getQueueDispute() {
        return queueDispute;
    }

    public void setQueueDispute(String queueDispute) {
        this.queueDispute = queueDispute;
    }

    public String dlqDispute() { // nombre de la DLQ por convencion .dlq
        return queueDispute + ".dlq";
    }
}
