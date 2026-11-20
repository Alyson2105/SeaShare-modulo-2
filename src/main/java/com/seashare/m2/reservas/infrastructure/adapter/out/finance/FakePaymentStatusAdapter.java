package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.PaymentStatusResult;
import com.seashare.m2.reservas.application.port.out.PaymentStatusPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 3.
@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakePaymentStatusAdapter implements PaymentStatusPort { // estado del cobro simulado y configurable

    private final FinanceProperties properties;

    public FakePaymentStatusAdapter(FinanceProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentStatusResult getStatus(UUID reservationId) { // devuelve el estado configurado por propiedad
        PaymentStatus status = parse(properties.getFakePaymentStatus());
        String detail = status == PaymentStatus.REJECTED ? "cc_rejected_insufficient_amount" : null;
        return new PaymentStatusResult(status, detail, "m3-fake-" + reservationId); // referencia estable
    }

    private PaymentStatus parse(String value) { // tolera valores desconocidos
        if (value == null || value.isBlank()) {
            return PaymentStatus.IN_PROCESS; // por defecto no se asume resultado
        }
        try {
            return PaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PaymentStatus.UNKNOWN; // nunca se asume aprobado
        }
    }
}
