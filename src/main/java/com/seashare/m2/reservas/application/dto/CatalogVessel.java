package com.seashare.m2.reservas.application.dto;

import java.util.UUID;

public record CatalogVessel( // embarcacion del catalogo de modulo 1 (CU-09 operacion en lote)
        UUID vesselId, // identificador en modulo 1
        String name, // nombre comercial
        String type, // MOTORBOAT, SAILBOAT, YACHT o CATAMARAN
        String photoUrl, // imagen de la tarjeta
        String portName, // marina o puerto base
        Double latitude, // coordenada del puerto
        Double longitude, // coordenada del puerto
        String timezone, // zona IANA del puerto
        int maxCapacity, // pasajeros maximos
        boolean captainIncluded // viaje con capitan
) {
}
