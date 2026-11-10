package com.seashare.m2.reservas.domain.model;

import com.seashare.m2.reservas.domain.state.ReservationStateMachine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Reservation { // agregado de reserva del dominio

    private final UUID id; // identificador unico de la reserva
    private final UUID renterId; // identificador del arrendatario
    private final UUID ownerId; // identificador del propietario
    private final UUID vesselId; // identificador de la embarcacion
    private final LocalDate startDate; // fecha inicial de la reserva
    private final LocalDate endDate; // fecha final de la reserva
    private final int passengers; // cantidad de pasajeros
    private final String holderName; // nombre completo del titular
    private final String holderPhone; // telefono de contacto del titular
    private final String holderEmail; // correo electronico del titular
    private final BigDecimal estimatedTotal; // total estimado persistido literalmente
    private final String currency; // codigo de moneda
    private final LocalTime departureTime; // hora programada de zarpe
    private final String harborTimezone; // zona horaria del puerto
    private final Instant ttlExpiresAt; // instante de expiracion calculado con ttl
    private final Instant createdAt; // instante de creacion fijado por el reloj
    private ReservationStatus status; // estado actual de la reserva
    private CancellationSubtype cancellationSubtype; // subtipo de cancelacion de la reserva
    private String completionNotes; // notas de cierre en COMPLETED (CU-08)
    private String paymentReference; // referencia externa del cobro (CU-08)
    private Instant paymentConfirmedAt; // hora de confirmacion del pago (CU-08)
    private Instant statusChangedAt; // hora exacta del ultimo cambio de estado (CU-08)
    private Instant actualDepartureAt; // hora real de zarpe; la fija CU-06 (T-17c), aqui solo persistencia
    private Instant actualArrivalAt; // hora real de desembarque; la fija CU-07 (T-17d), aqui solo persistencia
    private boolean m1Blocked; // true si ya se notifico RESERVED a Modulo 1 (guarda SC-003)
    private final Long version; // version de concurrencia optimista
    private String reservationCode; // codigo visible persistido; solo lectura para las vistas

    public Reservation(UUID id, UUID renterId, UUID ownerId, UUID vesselId, LocalDate startDate, LocalDate endDate,
                       int passengers, String holderName, String holderPhone, String holderEmail,
                       BigDecimal estimatedTotal, String currency, LocalTime departureTime, String harborTimezone,
                       Instant ttlExpiresAt, Instant createdAt, ReservationStatus status,
                       CancellationSubtype cancellationSubtype, Long version) { // constructor completo del agregado
        this.id = id; // asigna identificador
        this.renterId = renterId; // asigna arrendatario
        this.ownerId = ownerId; // asigna propietario
        this.vesselId = vesselId; // asigna embarcacion
        this.startDate = startDate; // asigna fecha inicio
        this.endDate = endDate; // asigna fecha fin
        this.passengers = passengers; // asigna pasajeros
        this.holderName = holderName; // asigna titular
        this.holderPhone = holderPhone; // asigna telefono
        this.holderEmail = holderEmail; // asigna correo
        this.estimatedTotal = estimatedTotal; // asigna total estimado
        this.currency = currency; // asigna moneda
        this.departureTime = departureTime; // asigna hora salida
        this.harborTimezone = harborTimezone; // asigna zona horaria
        this.ttlExpiresAt = ttlExpiresAt; // asigna expiracion ttl
        this.createdAt = createdAt; // asigna creacion
        this.status = status; // asigna estado
        this.cancellationSubtype = cancellationSubtype; // asigna subtipo
        this.version = version; // asigna version
    }

    // constructor de compatibilidad
    public Reservation(UUID id, UUID renterId, UUID ownerId, ReservationStatus status, CancellationSubtype cancellationSubtype, Long version) {
        this(id, renterId, ownerId, null, null, null, 0, null, null, null, null, "COP", null, null, null, null, status, cancellationSubtype, version);
    }

    public static class Builder {
        private UUID id;
        private UUID renterId;
        private UUID ownerId;
        private UUID vesselId;
        private LocalDate startDate;
        private LocalDate endDate;
        private int passengers;
        private String holderName;
        private String holderPhone;
        private String holderEmail;
        private BigDecimal estimatedTotal;
        private String currency = "COP";
        private LocalTime departureTime;
        private String harborTimezone;
        private Instant ttlExpiresAt;
        private Instant createdAt;
        private ReservationStatus status;
        private CancellationSubtype cancellationSubtype;
        private Long version;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder renterId(UUID renterId) { this.renterId = renterId; return this; }
        public Builder ownerId(UUID ownerId) { this.ownerId = ownerId; return this; }
        public Builder vesselId(UUID vesselId) { this.vesselId = vesselId; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder passengers(int passengers) { this.passengers = passengers; return this; }
        public Builder holderName(String holderName) { this.holderName = holderName; return this; }
        public Builder holderPhone(String holderPhone) { this.holderPhone = holderPhone; return this; }
        public Builder holderEmail(String holderEmail) { this.holderEmail = holderEmail; return this; }
        public Builder estimatedTotal(BigDecimal estimatedTotal) { this.estimatedTotal = estimatedTotal; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder departureTime(LocalTime departureTime) { this.departureTime = departureTime; return this; }
        public Builder harborTimezone(String harborTimezone) { this.harborTimezone = harborTimezone; return this; }
        public Builder ttlExpiresAt(Instant ttlExpiresAt) { this.ttlExpiresAt = ttlExpiresAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder status(ReservationStatus status) { this.status = status; return this; }
        public Builder cancellationSubtype(CancellationSubtype cancellationSubtype) { this.cancellationSubtype = cancellationSubtype; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public Reservation build() {
            return new Reservation(id, renterId, ownerId, vesselId, startDate, endDate, passengers, holderName,
                    holderPhone, holderEmail, estimatedTotal, currency, departureTime, harborTimezone,
                    ttlExpiresAt, createdAt, status, cancellationSubtype, version);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Reservation initiate(NewReservation newReservation) {
        return builder()
                .renterId(newReservation.renterId())
                .ownerId(newReservation.ownerId())
                .vesselId(newReservation.vesselId())
                .startDate(newReservation.startDate())
                .endDate(newReservation.endDate())
                .passengers(newReservation.passengers())
                .holderName(newReservation.holderName())
                .holderPhone(newReservation.holderPhone())
                .holderEmail(newReservation.holderEmail())
                .estimatedTotal(newReservation.estimatedTotal())
                .currency("COP")
                .departureTime(newReservation.departureTime())
                .harborTimezone(newReservation.harborTimezone())
                .ttlExpiresAt(newReservation.ttlExpiresAt())
                .createdAt(newReservation.createdAt())
                .status(ReservationStatus.INITIATED)
                .cancellationSubtype(null)
                .id(null)
                .version(null)
                .build();
    }

    public boolean isRenter(UUID userId) {
        return renterId != null && renterId.equals(userId);
    }

    public boolean isOwner(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public void changeStatus(ReservationStatus to, CancellationSubtype subtype, ReservationStateMachine machine) { // cambia el estado validando reglas
        machine.validate(this.status, to, subtype); // valida transicion segun maquina de estados
        this.status = to; // asigna nuevo estado
        this.cancellationSubtype = (to == ReservationStatus.CANCELLED ? subtype : null); // asigna subtipo solo si el estado es cancelado
    }

    public String getCompletionNotes() { // obtiene notas de cierre
        return completionNotes; // retorna completionNotes
    }

    public void setCompletionNotes(String completionNotes) { // fija notas de cierre (CU-08 COMPLETED)
        this.completionNotes = completionNotes; // establece completionNotes
    }

    public String getPaymentReference() { // obtiene referencia de pago
        return paymentReference; // retorna paymentReference
    }

    public void setPaymentReference(String paymentReference) { // fija referencia de pago (CU-08 RESERVED)
        this.paymentReference = paymentReference; // establece paymentReference
    }

    public Instant getPaymentConfirmedAt() { // obtiene hora de confirmacion de pago
        return paymentConfirmedAt; // retorna paymentConfirmedAt
    }

    public void setPaymentConfirmedAt(Instant paymentConfirmedAt) { // fija hora de confirmacion de pago
        this.paymentConfirmedAt = paymentConfirmedAt; // establece paymentConfirmedAt
    }

    public Instant getStatusChangedAt() { // obtiene hora del ultimo cambio de estado
        return statusChangedAt; // retorna statusChangedAt
    }

    public void setStatusChangedAt(Instant statusChangedAt) { // fija hora del ultimo cambio de estado
        this.statusChangedAt = statusChangedAt; // establece statusChangedAt
    }

    public Instant getActualDepartureAt() { // obtiene hora real de zarpe
        return actualDepartureAt; // retorna actualDepartureAt
    }

    public void setActualDepartureAt(Instant actualDepartureAt) { // fija hora real de zarpe (la fija CU-06)
        this.actualDepartureAt = actualDepartureAt; // establece actualDepartureAt
    }

    public Instant getActualArrivalAt() { // obtiene hora real de desembarque
        return actualArrivalAt; // retorna actualArrivalAt
    }

    public void setActualArrivalAt(Instant actualArrivalAt) { // fija hora real de desembarque (la fija CU-07)
        this.actualArrivalAt = actualArrivalAt; // establece actualArrivalAt
    }

    public boolean isM1Blocked() { // consulta si Modulo 1 ya fue notificado de RESERVED
        return m1Blocked; // retorna m1Blocked
    }

    public void setM1Blocked(boolean m1Blocked) { // fija la guarda de bloqueo en Modulo 1 (T-08)
        this.m1Blocked = m1Blocked; // establece m1Blocked
    }

    public String getReservationCode() { // obtiene el codigo visible
        return reservationCode; // retorna reservationCode
    }

    public void setReservationCode(String reservationCode) { // asigna el codigo visible desde persistencia
        this.reservationCode = reservationCode; // establece reservationCode
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public UUID getRenterId() { // obtiene el id del arrendatario
        return renterId; // retorna renterId
    }

    public UUID getOwnerId() { // obtiene el id del propietario
        return ownerId; // retorna ownerId
    }

    public UUID getVesselId() { // obtiene el id de la embarcacion
        return vesselId; // retorna vesselId
    }

    public LocalDate getStartDate() { // obtiene la fecha de inicio
        return startDate; // retorna startDate
    }

    public LocalDate getEndDate() { // obtiene la fecha de fin
        return endDate; // retorna endDate
    }

    public int getPassengers() { // obtiene el numero de pasajeros
        return passengers; // retorna passengers
    }

    public String getHolderName() { // obtiene el nombre del titular
        return holderName; // retorna holderName
    }

    public String getHolderPhone() { // obtiene el telefono del titular
        return holderPhone; // retorna holderPhone
    }

    public String getHolderEmail() { // obtiene el email del titular
        return holderEmail; // retorna holderEmail
    }

    public BigDecimal getEstimatedTotal() { // obtiene el total estimado
        return estimatedTotal; // retorna estimatedTotal
    }

    public String getCurrency() { // obtiene la moneda
        return currency; // retorna currency
    }

    public LocalTime getDepartureTime() { // obtiene la hora de salida
        return departureTime; // retorna departureTime
    }

    public String getHarborTimezone() { // obtiene la zona horaria del puerto
        return harborTimezone; // retorna harborTimezone
    }

    public Instant getTtlExpiresAt() { // obtiene la expiracion de ttl
        return ttlExpiresAt; // retorna ttlExpiresAt
    }

    public Instant getCreatedAt() { // obtiene la fecha de creacion
        return createdAt; // retorna createdAt
    }

    public ReservationStatus getStatus() { // obtiene el estado
        return status; // retorna status
    }

    public CancellationSubtype getCancellationSubtype() { // obtiene el subtipo de cancelacion
        return cancellationSubtype; // retorna cancellationSubtype
    }

    public Long getVersion() { // obtiene la version
        return version; // retorna version
    }
}
