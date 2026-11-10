package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.VesselDetail;

import java.util.List;
import java.util.UUID;

public record VesselDetailResponse( // ficha tecnica de CU-19 Endpoint 1

        @JsonProperty("vessel_id")
        UUID vesselId, // identificador de la embarcacion

        @JsonProperty("name")
        String name, // nombre comercial

        @JsonProperty("navigation_type")
        String navigationType, // tipo de navegacion segun M1

        @JsonProperty("max_capacity")
        Integer maxCapacity, // capacidad maxima

        @JsonProperty("length_feet")
        Double lengthFeet, // eslora en pies

        @JsonProperty("cabin_count")
        Integer cabinCount, // camarotes

        @JsonProperty("captain_included")
        Boolean captainIncluded, // capitan incluido

        @JsonProperty("port")
        PortResponse port, // puerto de salida

        @JsonProperty("amenities")
        List<String> amenities, // comodidades

        @JsonProperty("owner")
        OwnerResponse owner // datos del propietario
) {

    public record PortResponse(
            @JsonProperty("name")
            String name, // nombre del puerto

            @JsonProperty("latitude")
            Double latitude, // latitud

            @JsonProperty("longitude")
            Double longitude, // longitud

            @JsonProperty("timezone")
            String timezone // zona IANA
    ) {
    }

    public record OwnerResponse(
            @JsonProperty("validated_name")
            String validatedName, // nombre validado por M1

            @JsonProperty("verified")
            Boolean verified // propietario verificado
    ) {
    }

    public static VesselDetailResponse from(VesselDetail detail) { // transforma la ficha de aplicacion
        return new VesselDetailResponse(
                detail.vesselId(),
                detail.name(),
                detail.type(),
                detail.maxCapacity(),
                detail.lengthFeet(),
                detail.cabinCount(),
                detail.captainIncluded(),
                new PortResponse(detail.portName(), detail.portLatitude(), detail.portLongitude(), detail.timezone()),
                detail.amenities() == null ? List.of() : detail.amenities(),
                new OwnerResponse(detail.ownerName(), detail.ownerVerified()));
    }
}
