package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservation")
public class ReservationEntity { // entidad jpa para la tabla reservation

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "reservation_code", length = 30, nullable = false, unique = true)
    private String reservationCode; // codigo visible de la reserva

    @Column(name = "renter_id", nullable = false)
    private UUID renterId; // identificador del arrendatario

    @Column(name = "vessel_id", nullable = false)
    private UUID vesselId; // identificador de la embarcacion

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; // identificador del propietario

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReservationStatus status; // estado actual de la reserva

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_subtype", length = 20)
    private CancellationSubtype cancellationSubtype; // motivo o clasificacion de la cancelacion

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // fecha de inicio de la reserva

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // fecha de fin de la reserva

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime; // hora programada de zarpe

    @Column(name = "passengers", nullable = false)
    private int passengers; // cantidad de pasajeros

    @Column(name = "holder_name", length = 150, nullable = false)
    private String holderName; // nombre del titular de la reserva

    @Column(name = "holder_phone", length = 30)
    private String holderPhone; // telefono de contacto del titular

    @Column(name = "holder_email", length = 150)
    private String holderEmail; // correo electronico del titular

    @Column(name = "estimated_total", precision = 18, scale = 4)
    private BigDecimal estimatedTotal; // total estimado inicial

    @Column(name = "rental_amount", precision = 18, scale = 4)
    private BigDecimal rentalAmount; // monto base del alquiler

    @Column(name = "insurance_amount", precision = 18, scale = 4)
    private BigDecimal insuranceAmount; // monto del seguro contratado

    @Column(name = "guarantee_deposit_amount", precision = 18, scale = 4)
    private BigDecimal guaranteeDepositAmount; // deposito en garantia

    @Column(name = "total_amount", precision = 18, scale = 4)
    private BigDecimal totalAmount; // monto total liquidado

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3, nullable = false)
    private String currency; // codigo de moneda de tres caracteres

    @Column(name = "ttl_expires_at")
    private Instant ttlExpiresAt; // fecha y hora de expiracion del ttl

    @Column(name = "actual_departure_at")
    private Instant actualDepartureAt; // fecha y hora real de zarpe

    @Column(name = "actual_arrival_at")
    private Instant actualArrivalAt; // fecha y hora real de llegada

    @Column(name = "completion_notes")
    private String completionNotes; // notas de cierre de la navegacion

    @Column(name = "payment_reference", length = 100)
    private String paymentReference; // referencia externa del pago

    @Column(name = "payment_confirmed_at")
    private Instant paymentConfirmedAt; // fecha y hora de confirmacion del pago

    @Column(name = "m1_blocked", nullable = false)
    private boolean m1Blocked; // indicador de bloqueo notificado al modulo 1

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // marca temporal de creacion del registro

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // marca temporal de ultima actualizacion

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // control de bloqueo optimista

    @Column(name = "harbor_timezone", length = 50)
    private String harborTimezone; // zona horaria del puerto de salida

    public ReservationEntity() { // constructor sin argumentos requerido por jpa
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public void setId(UUID id) { // asigna el id
        this.id = id; // establece id
    }

    public String getReservationCode() { // obtiene el codigo de reserva
        return reservationCode; // retorna reservationCode
    }

    public void setReservationCode(String reservationCode) { // asigna el codigo de reserva
        this.reservationCode = reservationCode; // establece reservationCode
    }

    public UUID getRenterId() { // obtiene el id del arrendatario
        return renterId; // retorna renterId
    }

    public void setRenterId(UUID renterId) { // asigna el id del arrendatario
        this.renterId = renterId; // establece renterId
    }

    public UUID getVesselId() { // obtiene el id de la embarcacion
        return vesselId; // retorna vesselId
    }

    public void setVesselId(UUID vesselId) { // asigna el id de la embarcacion
        this.vesselId = vesselId; // establece vesselId
    }

    public UUID getOwnerId() { // obtiene el id del propietario
        return ownerId; // retorna ownerId
    }

    public void setOwnerId(UUID ownerId) { // asigna el id del propietario
        this.ownerId = ownerId; // establece ownerId
    }

    public ReservationStatus getStatus() { // obtiene el estado
        return status; // retorna status
    }

    public void setStatus(ReservationStatus status) { // asigna el estado
        this.status = status; // establece status
    }

    public CancellationSubtype getCancellationSubtype() { // obtiene el subtipo de cancelacion
        return cancellationSubtype; // retorna cancellationSubtype
    }

    public void setCancellationSubtype(CancellationSubtype cancellationSubtype) { // asigna el subtipo de cancelacion
        this.cancellationSubtype = cancellationSubtype; // establece cancellationSubtype
    }

    public LocalDate getStartDate() { // obtiene la fecha de inicio
        return startDate; // retorna startDate
    }

    public void setStartDate(LocalDate startDate) { // asigna la fecha de inicio
        this.startDate = startDate; // establece startDate
    }

    public LocalDate getEndDate() { // obtiene la fecha de fin
        return endDate; // retorna endDate
    }

    public void setEndDate(LocalDate endDate) { // asigna la fecha de fin
        this.endDate = endDate; // establece endDate
    }

    public LocalTime getDepartureTime() { // obtiene la hora de salida
        return departureTime; // retorna departureTime
    }

    public void setDepartureTime(LocalTime departureTime) { // asigna la hora de salida
        this.departureTime = departureTime; // establece departureTime
    }

    public int getPassengers() { // obtiene el numero de pasajeros
        return passengers; // retorna passengers
    }

    public void setPassengers(int passengers) { // asigna el numero de pasajeros
        this.passengers = passengers; // establece passengers
    }

    public String getHolderName() { // obtiene el nombre del titular
        return holderName; // retorna holderName
    }

    public void setHolderName(String holderName) { // asigna el nombre del titular
        this.holderName = holderName; // establece holderName
    }

    public String getHolderPhone() { // obtiene el telefono del titular
        return holderPhone; // retorna holderPhone
    }

    public void setHolderPhone(String holderPhone) { // asigna el telefono del titular
        this.holderPhone = holderPhone; // establece holderPhone
    }

    public String getHolderEmail() { // obtiene el email del titular
        return holderEmail; // retorna holderEmail
    }

    public void setHolderEmail(String holderEmail) { // asigna el email del titular
        this.holderEmail = holderEmail; // establece holderEmail
    }

    public BigDecimal getEstimatedTotal() { // obtiene el total estimado
        return estimatedTotal; // retorna estimatedTotal
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) { // asigna el total estimado
        this.estimatedTotal = estimatedTotal; // establece estimatedTotal
    }

    public BigDecimal getRentalAmount() { // obtiene el monto de alquiler
        return rentalAmount; // retorna rentalAmount
    }

    public void setRentalAmount(BigDecimal rentalAmount) { // asigna el monto de alquiler
        this.rentalAmount = rentalAmount; // establece rentalAmount
    }

    public BigDecimal getInsuranceAmount() { // obtiene el monto del seguro
        return insuranceAmount; // retorna insuranceAmount
    }

    public void setInsuranceAmount(BigDecimal insuranceAmount) { // asigna el monto del seguro
        this.insuranceAmount = insuranceAmount; // establece insuranceAmount
    }

    public BigDecimal getGuaranteeDepositAmount() { // obtiene el deposito de garantia
        return guaranteeDepositAmount; // retorna guaranteeDepositAmount
    }

    public void setGuaranteeDepositAmount(BigDecimal guaranteeDepositAmount) { // asigna el deposito de garantia
        this.guaranteeDepositAmount = guaranteeDepositAmount; // establece guaranteeDepositAmount
    }

    public BigDecimal getTotalAmount() { // obtiene el monto total
        return totalAmount; // retorna totalAmount
    }

    public void setTotalAmount(BigDecimal totalAmount) { // asigna el monto total
        this.totalAmount = totalAmount; // establece totalAmount
    }

    public String getCurrency() { // obtiene la moneda
        return currency; // retorna currency
    }

    public void setCurrency(String currency) { // asigna la moneda
        this.currency = currency; // establece currency
    }

    public Instant getTtlExpiresAt() { // obtiene la expiracion de ttl
        return ttlExpiresAt; // retorna ttlExpiresAt
    }

    public void setTtlExpiresAt(Instant ttlExpiresAt) { // asigna la expiracion de ttl
        this.ttlExpiresAt = ttlExpiresAt; // establece ttlExpiresAt
    }

    public Instant getActualDepartureAt() { // obtiene la salida real
        return actualDepartureAt; // retorna actualDepartureAt
    }

    public void setActualDepartureAt(Instant actualDepartureAt) { // asigna la salida real
        this.actualDepartureAt = actualDepartureAt; // establece actualDepartureAt
    }

    public Instant getActualArrivalAt() { // obtiene la llegada real
        return actualArrivalAt; // retorna actualArrivalAt
    }

    public void setActualArrivalAt(Instant actualArrivalAt) { // asigna la llegada real
        this.actualArrivalAt = actualArrivalAt; // establece actualArrivalAt
    }

    public String getCompletionNotes() { // obtiene las notas de finalizacion
        return completionNotes; // retorna completionNotes
    }

    public void setCompletionNotes(String completionNotes) { // asigna las notas de finalizacion
        this.completionNotes = completionNotes; // establece completionNotes
    }

    public String getPaymentReference() { // obtiene la referencia de pago
        return paymentReference; // retorna paymentReference
    }

    public void setPaymentReference(String paymentReference) { // asigna la referencia de pago
        this.paymentReference = paymentReference; // establece paymentReference
    }

    public Instant getPaymentConfirmedAt() { // obtiene la confirmacion de pago
        return paymentConfirmedAt; // retorna paymentConfirmedAt
    }

    public void setPaymentConfirmedAt(Instant paymentConfirmedAt) { // asigna la confirmacion de pago
        this.paymentConfirmedAt = paymentConfirmedAt; // establece paymentConfirmedAt
    }

    public boolean isM1Blocked() { // consulta indicador de bloqueo m1
        return m1Blocked; // retorna m1Blocked
    }

    public void setM1Blocked(boolean m1Blocked) { // asigna indicador de bloqueo m1
        this.m1Blocked = m1Blocked; // establece m1Blocked
    }

    public Instant getCreatedAt() { // obtiene la fecha de creacion
        return createdAt; // retorna createdAt
    }

    public void setCreatedAt(Instant createdAt) { // asigna la fecha de creacion
        this.createdAt = createdAt; // establece createdAt
    }

    public Instant getUpdatedAt() { // obtiene la fecha de actualizacion
        return updatedAt; // retorna updatedAt
    }

    public void setUpdatedAt(Instant updatedAt) { // asigna la fecha de actualizacion
        this.updatedAt = updatedAt; // establece updatedAt
    }

    public Long getVersion() { // obtiene la version
        return version; // retorna version
    }

    public void setVersion(Long version) { // asigna la version
        this.version = version; // establece version
    }

    public String getHarborTimezone() { // obtiene la zona horaria del puerto
        return harborTimezone; // retorna harborTimezone
    }

    public void setHarborTimezone(String harborTimezone) { // asigna la zona horaria del puerto
        this.harborTimezone = harborTimezone; // establece harborTimezone
    }
}
