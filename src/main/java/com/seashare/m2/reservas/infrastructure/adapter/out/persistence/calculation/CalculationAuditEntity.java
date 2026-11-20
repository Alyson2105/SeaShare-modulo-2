package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.calculation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calculation_audit")
@Immutable
class CalculationAuditEntity { // entidad jpa inmutable del asiento de calculo

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId; // reserva calculada

    @Column(name = "operation_id", length = 100)
    private String operationId; // identificador externo opcional

    @Column(name = "rental_amount", precision = 14, scale = 4)
    private BigDecimal rentalAmount; // alquiler

    @Column(name = "insurance_amount", precision = 14, scale = 4)
    private BigDecimal insuranceAmount; // seguro

    @Column(name = "guarantee_deposit_amount", precision = 14, scale = 4)
    private BigDecimal guaranteeDepositAmount; // deposito

    @Column(name = "total_amount", precision = 14, scale = 4)
    private BigDecimal totalAmount; // total

    @Column(name = "calculation_status", length = 20, nullable = false)
    private String calculationStatus; // SUCCESS o FAILED

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt; // instante del asiento

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public void setId(UUID id) { // asigna el id
        this.id = id; // establece id
    }

    public UUID getReservationId() { // obtiene la reserva
        return reservationId; // retorna reservationId
    }

    public void setReservationId(UUID reservationId) { // asigna la reserva
        this.reservationId = reservationId; // establece reservationId
    }

    public String getOperationId() { // obtiene la operacion
        return operationId; // retorna operationId
    }

    public void setOperationId(String operationId) { // asigna la operacion
        this.operationId = operationId; // establece operationId
    }

    public BigDecimal getRentalAmount() { // obtiene el alquiler
        return rentalAmount; // retorna rentalAmount
    }

    public void setRentalAmount(BigDecimal rentalAmount) { // asigna el alquiler
        this.rentalAmount = rentalAmount; // establece rentalAmount
    }

    public BigDecimal getInsuranceAmount() { // obtiene el seguro
        return insuranceAmount; // retorna insuranceAmount
    }

    public void setInsuranceAmount(BigDecimal insuranceAmount) { // asigna el seguro
        this.insuranceAmount = insuranceAmount; // establece insuranceAmount
    }

    public BigDecimal getGuaranteeDepositAmount() { // obtiene el deposito
        return guaranteeDepositAmount; // retorna guaranteeDepositAmount
    }

    public void setGuaranteeDepositAmount(BigDecimal guaranteeDepositAmount) { // asigna el deposito
        this.guaranteeDepositAmount = guaranteeDepositAmount; // establece guaranteeDepositAmount
    }

    public BigDecimal getTotalAmount() { // obtiene el total
        return totalAmount; // retorna totalAmount
    }

    public void setTotalAmount(BigDecimal totalAmount) { // asigna el total
        this.totalAmount = totalAmount; // establece totalAmount
    }

    public String getCalculationStatus() { // obtiene el estado
        return calculationStatus; // retorna calculationStatus
    }

    public void setCalculationStatus(String calculationStatus) { // asigna el estado
        this.calculationStatus = calculationStatus; // establece calculationStatus
    }

    public Instant getOccurredAt() { // obtiene la marca temporal
        return occurredAt; // retorna occurredAt
    }

    public void setOccurredAt(Instant occurredAt) { // asigna la marca temporal
        this.occurredAt = occurredAt; // establece occurredAt
    }
}
