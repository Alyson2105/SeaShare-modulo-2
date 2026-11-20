package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.externalquerylog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_query_log")
class ExternalQueryLogEntity { // entidad jpa para la tabla external_query_log

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "type", length = 30, nullable = false)
    private String type; // VESSEL_INFO u OPERATIONAL_STATUS

    @Column(name = "vessel_id")
    private UUID vesselId; // embarcacion consultada; nullable en V2

    @Column(name = "result", length = 30, nullable = false)
    private String result; // OK, TIMEOUT o ERROR

    @Column(name = "returned_status", length = 30)
    private String returnedStatus; // estado crudo de M1; solo CU-10 con OK

    @Column(name = "correlation_id", length = 64)
    private String correlationId; // desde el MDC; nulo fuera de HTTP

    @Column(name = "consulted_at", nullable = false)
    private Instant consultedAt; // hora de consulta via ClockPort; columna TIMESTAMPTZ

    public ExternalQueryLogEntity() { // constructor sin argumentos requerido por jpa
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public void setId(UUID id) { // asigna el id
        this.id = id; // establece id
    }

    public String getType() { // obtiene el tipo de consulta
        return type; // retorna type
    }

    public void setType(String type) { // asigna el tipo de consulta
        this.type = type; // establece type
    }

    public UUID getVesselId() { // obtiene la embarcacion consultada
        return vesselId; // retorna vesselId
    }

    public void setVesselId(UUID vesselId) { // asigna la embarcacion consultada
        this.vesselId = vesselId; // establece vesselId
    }

    public String getResult() { // obtiene el resultado de la consulta
        return result; // retorna result
    }

    public void setResult(String result) { // asigna el resultado de la consulta
        this.result = result; // establece result
    }

    public String getReturnedStatus() { // obtiene el estado crudo devuelto por M1
        return returnedStatus; // retorna returnedStatus
    }

    public void setReturnedStatus(String returnedStatus) { // asigna el estado crudo devuelto por M1
        this.returnedStatus = returnedStatus; // establece returnedStatus
    }

    public String getCorrelationId() { // obtiene el correlation id
        return correlationId; // retorna correlationId
    }

    public void setCorrelationId(String correlationId) { // asigna el correlation id
        this.correlationId = correlationId; // establece correlationId
    }

    public Instant getConsultedAt() { // obtiene la hora de la consulta
        return consultedAt; // retorna consultedAt
    }

    public void setConsultedAt(Instant consultedAt) { // asigna la hora de la consulta
        this.consultedAt = consultedAt; // establece consultedAt
    }
}
