package com.seashare.m2.reservas.application.dto;

public record PaymentStatusResult( // respuesta del contrato UC06 de modulo 3
        PaymentStatus status, // estado del cobro
        String detail, // motivo o codigo de la pasarela
        String externalReference // identificador de la transaccion externa
) {
}
