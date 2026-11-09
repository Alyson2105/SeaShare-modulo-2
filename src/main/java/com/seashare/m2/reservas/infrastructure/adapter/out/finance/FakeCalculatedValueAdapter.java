package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.port.out.CalculatedValuePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 3.
@Component
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeCalculatedValueAdapter implements CalculatedValuePort { // adaptador simulado de valor calculado

    @Override
    public CalculatedValue getCalculatedValue(UUID reservationId) { // retorna datos fijos de prueba
        return new CalculatedValue( // desglose fijo sin aritmetica
                new BigDecimal("9600000.00"), // alquiler fijo
                new BigDecimal("120000.00"), // seguro fijo
                new BigDecimal("320000.00"), // deposito fijo
                new BigDecimal("10040000.00")); // total fijo tal cual
    }
}
