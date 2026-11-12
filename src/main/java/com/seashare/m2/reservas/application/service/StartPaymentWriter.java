package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class StartPaymentWriter { // CU-03: frontera transaccional con bloqueo pesimista y transicion

    private static final String PAYMENT_REASON = "Pago iniciado por el arrendatario"; // motivo auditado

    private final ReservationRepositoryPort reservationRepository;

    private final ChangeReservationStatusUseCase changeReservationStatus;

    private final ClockPort clockPort;

    public StartPaymentWriter(ReservationRepositoryPort reservationRepository,
                              ChangeReservationStatusUseCase changeReservationStatus,
                              ClockPort clockPort) {
        this.reservationRepository = reservationRepository; // asigna repositorio
        this.changeReservationStatus = changeReservationStatus; // asigna motor CU-08
        this.clockPort = clockPort; // asigna reloj
    }

    @Transactional
    public StartPaymentResult startPayment(UUID reservationId,
                                           CalculatedValue calculation,
                                           String paymentTokenRef,
                                           Instant requestedAt) { // una sola transaccion para validar y transicionar
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId) // SELECT ... FOR UPDATE
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (reservation.getStatus() != ReservationStatus.INITIATED) { // pudo cambiar durante el calculo
            throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "Solo se puede iniciar el pago de reservas en estado Iniciada (estado actual: "
                            + reservation.getStatus() + ")");
        }
        if (reservation.getTtlExpiresAt() != null && !requestedAt.isBefore(reservation.getTtlExpiresAt())) {
            throw new ReservationRuleException(ErrorCode.RESERVATION_EXPIRED,
                    "El tiempo limite de 15 minutos para iniciar el pago de esta reserva ha expirado");
        }
        if (reservationRepository.existsOverlap(reservation.getVesselId(), reservation.getStartDate(),
                reservation.getEndDate(), CreateReservationService.BLOCKING_STATUSES)) {
            throw new ReservationRuleException(ErrorCode.CONCURRENCY_CONFLICT,
                    "La embarcacion ya no se encuentra disponible para las fechas seleccionadas debido a un pago concurrente");
        }

        persistCalculation(reservation, calculation); // guarda el desglose literal de M3

        ChangeReservationStatusResult result = changeReservationStatus.execute(new ChangeReservationStatusCommand(
                reservationId,
                ReservationStatus.PENDING_PAYMENT,
                null,
                ActorType.RENTER,
                PAYMENT_REASON,
                reservation.getRenterId(),
                null, // sin expected_version: la fila ya esta bloqueada
                null,
                null,
                null,
                null,
                paymentTokenRef)); // viaja en el evento PENDING (CU-14)

        Instant ttlExpiresAt = result.ttlExpiresAt() != null ? result.ttlExpiresAt() : reservation.getTtlExpiresAt();
        long remaining = ttlExpiresAt == null ? 0L
                : Math.max(0L, Duration.between(clockPort.now(), ttlExpiresAt).getSeconds());

        return new StartPaymentResult(
                result.reservationId(),
                result.status(),
                reservation.getVesselId(),
                calculation,
                remaining,
                ttlExpiresAt == null ? null
                        : ttlExpiresAt.atZone(HarborTime.zoneOf(reservation.getHarborTimezone())).toOffsetDateTime(),
                1); // primer intento; reintentos pertenecen a CU-13
    }

    private void persistCalculation(Reservation reservation, CalculatedValue calculation) { // sin aritmetica monetaria
        reservation.setRentalAmount(calculation.rentalAmount());
        reservation.setInsuranceAmount(calculation.insuranceAmount());
        reservation.setGuaranteeDepositAmount(calculation.guaranteeDepositAmount());
        reservation.setTotalAmount(calculation.totalAmount());
        reservationRepository.save(reservation); // persiste el desglose antes de la transicion
    }
}
