package com.seashare.m2.reservas.application.dto;

import java.util.UUID;

public record VesselSummary( // datos minimos de embarcacion para las vistas de consulta

        UUID vesselId, // identificador de la embarcacion en modulo 1

        String name, // nombre comercial

        String imageUrl, // imagen principal; null si el contrato de M1 no la entrega

        String portName, // nombre del puerto de salida

        UUID ownerId, // propietario segun modulo 1

        String ownerName // nombre validado del propietario
) {
}
