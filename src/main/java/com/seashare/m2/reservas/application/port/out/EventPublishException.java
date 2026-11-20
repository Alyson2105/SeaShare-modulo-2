package com.seashare.m2.reservas.application.port.out;

/**
 * Excepcion lanzada por {@link EventPublisherPort} cuando el broker no confirma
 * la publicacion (nack, timeout o conexion caida). Es reintetable (D-04/CU-14):
 * el relay agenda el siguiente intento hasta agotar la politica.
 */
public class EventPublishException extends RuntimeException { // fallo de publicacion amqp sin confirm del broker

    public EventPublishException(String message) { // constructor con mensaje
        super(message); // delega en runtime
    }

    public EventPublishException(String message, Throwable cause) { // constructor con causa
        super(message, cause); // delega en runtime con causa
    }
}
