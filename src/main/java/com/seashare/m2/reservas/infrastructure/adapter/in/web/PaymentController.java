package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
import com.seashare.m2.reservas.application.port.in.StartPaymentUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class PaymentController { // CU-03: iniciar el pago de una reserva INITIATED

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"); // formato de correo

    private final StartPaymentUseCase startPaymentUseCase; // caso de uso

    public PaymentController(StartPaymentUseCase startPaymentUseCase) {
        this.startPaymentUseCase = startPaymentUseCase; // asigna caso de uso
    }

    @PostMapping("/api/v1/reservations/{reservationId}/payments")
    public ResponseEntity<StartPaymentResponse> startPayment(
            CallerIdentity identity, // identidad temporal por headers
            @PathVariable("reservationId") String reservationId, // reserva en INITIATED
            @Valid @RequestBody StartPaymentRequest request) { // politica y datos de pago
        UUID id = parseUuid(reservationId); // 400 INVALID_UUID
        requireAcceptedPolicy(request); // FR-014
        String token = requirePaymentData(request); // FR-013 / datos de M3

        StartPaymentResult result = startPaymentUseCase.start(new StartPaymentCommand(
                id, identity.userId(), identity.role(), token)); // 403, 404, 409 o 503

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore()) // informacion transaccional en tiempo real
                .body(StartPaymentResponse.from(result));
    }

    private UUID parseUuid(String value) { // valida el identificador de la ruta
        try {
            return UUID.fromString(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID,
                    "El identificador de la reserva no es válido");
        }
    }

    private void requireAcceptedPolicy(StartPaymentRequest request) { // FR-014
        if (request == null || !Boolean.TRUE.equals(request.acceptsCancellationPolicy())) {
            throw new ReservationRuleException(ErrorCode.POLICY_NOT_ACCEPTED,
                    "Se requiere tu consentimiento explícito para las políticas de cancelación para iniciar el pago");
        }
    }

    private String requirePaymentData(StartPaymentRequest request) { // datos minimos del medio de pago
        StartPaymentRequest.PaymentData payment = request.payment();
        StartPaymentRequest.PaymentMetadata metadata = payment == null ? null : payment.paymentMetadata();
        boolean incomplete = payment == null
                || isBlank(payment.paymentTokenRef())
                || isBlank(payment.paymentMethodType())
                || metadata == null
                || isBlank(metadata.payerEmail())
                || !EMAIL.matcher(metadata.payerEmail().trim()).matches()
                || isBlank(metadata.paymentMethodId())
                || metadata.installments() == null
                || metadata.installments() < 1;
        if (incomplete) {
            throw new ReservationRuleException(ErrorCode.PAYMENT_DATA_INCOMPLETE,
                    "Faltan los datos del medio de pago para iniciar el cobro");
        }
        return payment.paymentTokenRef().trim(); // token opaco hacia M3
    }

    private boolean isBlank(String value) { // utilidad de validacion
        return value == null || value.isBlank();
    }
}
