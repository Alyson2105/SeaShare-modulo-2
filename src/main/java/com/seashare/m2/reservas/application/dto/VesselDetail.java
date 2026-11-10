package com.seashare.m2.reservas.application.dto;

import java.util.List;
import java.util.UUID;

public record VesselDetail( // ficha tecnica de embarcacion para el detalle de CU-21 y CU-19

        UUID vesselId, // identificador en modulo 1

        String name, // nombre comercial

        String type, // tipo de embarcacion segun M1

        String imageUrl, // imagen principal; null si el contrato no la entrega

        Integer maxCapacity, // capacidad maxima de pasajeros

        Double lengthFeet, // eslora en pies

        Integer cabinCount, // numero de camarotes

        Boolean captainIncluded, // capitan incluido

        String portName, // nombre del puerto de salida

        String portCity, // ciudad del puerto; no existe en el contrato de M1

        Double portLatitude, // latitud del puerto

        Double portLongitude, // longitud del puerto

        String timezone, // zona IANA del puerto

        List<String> amenities, // servicios incluidos

        UUID ownerId, // propietario segun M1

        String ownerName, // nombre validado del propietario

        Boolean ownerVerified // propietario verificado por M1
) {
}
