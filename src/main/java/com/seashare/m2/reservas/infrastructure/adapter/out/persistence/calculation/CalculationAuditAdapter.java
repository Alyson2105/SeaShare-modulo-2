package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.calculation;

import com.seashare.m2.reservas.application.dto.CalculationAuditEntry;
import com.seashare.m2.reservas.application.port.out.CalculationAuditPort;
import org.springframework.stereotype.Component;

@Component
public class CalculationAuditAdapter implements CalculationAuditPort { // adaptador de persistencia de la auditoria de calculo

    private final CalculationAuditJpaRepository repository; // repositorio jpa

    public CalculationAuditAdapter(CalculationAuditJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void record(CalculationAuditEntry entry) { // inserta el asiento inmutable
        CalculationAuditEntity entity = new CalculationAuditEntity(); // crea la entidad
        entity.setReservationId(entry.reservationId()); // reserva
        entity.setOperationId(entry.operationId()); // operacion opcional
        entity.setRentalAmount(entry.rentalAmount()); // alquiler
        entity.setInsuranceAmount(entry.insuranceAmount()); // seguro
        entity.setGuaranteeDepositAmount(entry.guaranteeDepositAmount()); // deposito
        entity.setTotalAmount(entry.totalAmount()); // total
        entity.setCalculationStatus(entry.calculationStatus()); // estado
        entity.setOccurredAt(entry.occurredAt()); // marca temporal
        repository.save(entity); // persiste solo insercion
    }
}
