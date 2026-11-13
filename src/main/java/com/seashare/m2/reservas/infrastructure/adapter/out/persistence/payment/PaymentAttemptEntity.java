package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempt")
class PaymentAttemptEntity { // entidad jpa de la tabla payment_attempt (CU-13)

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId; // reserva a la que pertenece el intento

    @Column(name = "result", nullable = false, length = 20)
    private String result; // APPROVED, REJECTED, CANCELLED, EXPIRED, IN_PROCESS o UNKNOWN

    @Column(name = "detail")
    private String detail; // motivo o codigo de la pasarela

    @Column(name = "external_reference", length = 100)
    private String externalReference; // identificador de la transaccion externa

    @Column(name = "consulted_at", nullable = false)
    private Instant consultedAt; // instante en que se obtuvo el resultado

    public UUID getId() {
        return id;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public Instant getConsultedAt() {
        return consultedAt;
    }

    public void setConsultedAt(Instant consultedAt) {
        this.consultedAt = consultedAt;
    }
}
