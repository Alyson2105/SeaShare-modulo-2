package com.seashare.m2.reservas.application.dto;

import java.util.UUID;

public record ExternalQueryLogEntry( // datos de una consulta a M1 pendientes de registrar

        ExternalQueryType type, // tipo de consulta: VESSEL_INFO u OPERATIONAL_STATUS

        UUID vesselId, // embarcacion consultada

        ExternalQueryResult result, // resultado final: OK, TIMEOUT o ERROR

        String returnedStatus // estado crudo de M1 en CU-10 con OK; null en el resto
) {
}
