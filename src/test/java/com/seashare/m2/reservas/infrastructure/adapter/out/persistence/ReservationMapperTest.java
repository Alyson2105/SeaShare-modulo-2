package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMapperTest { // pruebas unitarias para el mapeador de persistencia

    private final ReservationMapper mapper = new ReservationMapper(); // instancia del mapeador bajo prueba

    @Test
    void toDomainKeepsAllFields() { // verifica conversion completa de los seis campos hacia el dominio
        ReservationEntity entity = new ReservationEntity(); // crea entidad jpa
        UUID id = UUID.randomUUID(); // id aleatorio
        UUID renterId = UUID.randomUUID(); // arrendatario aleatorio
        UUID ownerId = UUID.randomUUID(); // propietario aleatorio
        entity.setId(id); // asigna id
        entity.setRenterId(renterId); // asigna arrendatario
        entity.setOwnerId(ownerId); // asigna propietario
        entity.setStatus(ReservationStatus.RESERVED); // asigna estado
        entity.setCancellationSubtype(CancellationSubtype.BY_OWNER); // asigna subtipo
        entity.setVersion(3L); // asigna version

        Reservation domain = mapper.toDomain(entity); // realiza mapeo a dominio

        assertThat(domain.getId()).isEqualTo(id); // verifica id
        assertThat(domain.getRenterId()).isEqualTo(renterId); // verifica renterId
        assertThat(domain.getOwnerId()).isEqualTo(ownerId); // verifica ownerId
        assertThat(domain.getStatus()).isEqualTo(ReservationStatus.RESERVED); // verifica status
        assertThat(domain.getCancellationSubtype()).isEqualTo(CancellationSubtype.BY_OWNER); // verifica cancellationSubtype
        assertThat(domain.getVersion()).isEqualTo(3L); // verifica version
    }

    @Test
    void applyChangesKeepsUnrelatedColumns() { // verifica que solo se alteren estado y subtipo preservando demas columnas
        ReservationEntity entity = new ReservationEntity(); // crea entidad con multiples atributos
        entity.setId(UUID.randomUUID()); // asigna id
        entity.setReservationCode("RES-998877"); // asigna codigo de reserva
        entity.setRenterId(UUID.randomUUID()); // asigna arrendatario
        entity.setVesselId(UUID.randomUUID()); // asigna embarcacion
        entity.setOwnerId(UUID.randomUUID()); // asigna propietario
        entity.setStatus(ReservationStatus.INITIATED); // asigna estado inicial
        entity.setStartDate(LocalDate.of(2026, 12, 1)); // asigna fecha inicio
        entity.setEndDate(LocalDate.of(2026, 12, 5)); // asigna fecha fin
        entity.setDepartureTime(LocalTime.of(8, 30)); // asigna hora salida
        entity.setPassengers(5); // asigna pasajeros
        entity.setHolderName("Carlos Ruiz"); // asigna titular
        entity.setRentalAmount(new BigDecimal("750000.0000")); // asigna monto
        entity.setCurrency("COP"); // asigna moneda
        entity.setM1Blocked(false); // asigna bandera m1
        entity.setHarborTimezone("America/Bogota"); // asigna zona horaria
        entity.setVersion(2L); // asigna version

        Reservation domain = new Reservation(entity.getId(), entity.getRenterId(), entity.getOwnerId(), ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, 2L); // modelo modificado

        mapper.applyChanges(domain, entity); // aplica cambios a la entidad existente

        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.CANCELLED); // comprueba cambio de estado
        assertThat(entity.getCancellationSubtype()).isEqualTo(CancellationSubtype.FLEXIBLE); // comprueba cambio de subtipo
        assertThat(entity.getReservationCode()).isEqualTo("RES-998877"); // verifica codigo sin cambio
        assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2026, 12, 1)); // verifica fecha inicio intacta
        assertThat(entity.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 5)); // verifica fecha fin intacta
        assertThat(entity.getDepartureTime()).isEqualTo(LocalTime.of(8, 30)); // verifica hora zarpe intacta
        assertThat(entity.getPassengers()).isEqualTo(5); // verifica pasajeros intactos
        assertThat(entity.getHolderName()).isEqualTo("Carlos Ruiz"); // verifica titular intacto
        assertThat(entity.getRentalAmount()).isEqualByComparingTo(new BigDecimal("750000.0000")); // verifica monto intacto
        assertThat(entity.getCurrency()).isEqualTo("COP"); // verifica moneda intacta
        assertThat(entity.isM1Blocked()).isFalse(); // verifica bandera m1 intacta
        assertThat(entity.getHarborTimezone()).isEqualTo("America/Bogota"); // verifica zona horaria intacta
        assertThat(entity.getVersion()).isEqualTo(2L); // verifica version intacta
    }
}
