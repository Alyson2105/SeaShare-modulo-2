package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReservationPersistenceTest { // pruebas de persistencia para la entidad reservation

    @Autowired
    private ReservationJpaRepository repository; // repositorio inyectado

    private final List<UUID> createdIds = new ArrayList<>(); // lista de identificadores creados

    @AfterEach
    void tearDown() { // limpieza posterior a cada prueba
        for (UUID id : createdIds) { // recorre identificadores registrados
            if (repository.existsById(id)) { // verifica existencia
                repository.deleteById(id); // elimina registro por id
            }
        }
        createdIds.clear(); // vacia lista de identificadores
    }

    private ReservationEntity createValidEntity() { // fabrica entidad valida para pruebas
        ReservationEntity entity = new ReservationEntity(); // instancia de entidad
        entity.setReservationCode("RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)); // codigo unico
        entity.setRenterId(UUID.randomUUID()); // arrendatario aleatorio
        entity.setVesselId(UUID.randomUUID()); // embarcacion aleatoria
        entity.setOwnerId(UUID.randomUUID()); // propietario aleatorio
        entity.setStatus(ReservationStatus.INITIATED); // estado inicial
        entity.setStartDate(LocalDate.now().plusDays(5)); // fecha inicio
        entity.setEndDate(LocalDate.now().plusDays(7)); // fecha fin
        entity.setDepartureTime(LocalTime.of(9, 0)); // hora de zarpe
        entity.setPassengers(2); // cantidad pasajeros
        entity.setHolderName("Test"); // nombre titular
        entity.setCurrency("COP"); // moneda
        entity.setM1Blocked(false); // indicador m1
        entity.setHarborTimezone("America/Bogota"); // zona horaria del puerto
        entity.setEstimatedTotal(new BigDecimal("1000000.0000")); // total estimado
        entity.setRentalAmount(new BigDecimal("1000000.0000")); // monto alquiler
        entity.setInsuranceAmount(new BigDecimal("1000000.0000")); // monto seguro
        entity.setGuaranteeDepositAmount(new BigDecimal("1000000.0000")); // monto garantia
        entity.setCreatedAt(Instant.now()); // marca temporal de creacion
        return entity; // retorna entidad preparada
    }

    @Test
    void savesAndReadsReservation() { // verifica persistencia y lectura de los campos clave
        ReservationEntity entity = createValidEntity(); // genera entidad valida
        ReservationEntity saved = repository.save(entity); // almacena entidad
        createdIds.add(saved.getId()); // registra id para limpieza

        ReservationEntity found = repository.findById(saved.getId()).orElseThrow(); // recupera entidad por id

        assertThat(found.getReservationCode()).isEqualTo(entity.getReservationCode()); // valida codigo
        assertThat(found.getStatus()).isEqualTo(ReservationStatus.INITIATED); // valida estado
        assertThat(found.getStartDate()).isEqualTo(entity.getStartDate()); // valida fecha inicio
        assertThat(found.getEndDate()).isEqualTo(entity.getEndDate()); // valida fecha fin
        assertThat(found.getDepartureTime()).isEqualTo(entity.getDepartureTime()); // valida hora zarpe
        assertThat(found.getHarborTimezone()).isEqualTo("America/Bogota"); // valida zona horaria
        assertThat(found.getCurrency()).isEqualTo("COP"); // valida moneda
        assertThat(found.getRentalAmount()).isEqualByComparingTo(new BigDecimal("1000000.0000")); // valida monto con compareTo
        assertThat(found.getVersion()).isNotNull(); // valida version de bloqueo optimista
    }

    @Test
    void staleVersionFails() { // verifica que una version desactualizada provoque fallo optimista
        ReservationEntity entity = createValidEntity(); // genera entidad valida
        ReservationEntity saved = repository.save(entity); // almacena entidad inicial
        createdIds.add(saved.getId()); // registra id para limpieza

        ReservationEntity a = repository.findById(saved.getId()).orElseThrow(); // obtiene copia a
        ReservationEntity b = repository.findById(saved.getId()).orElseThrow(); // obtiene copia b

        a.setHolderName("Updated by A"); // modifica titular en copia a
        repository.save(a); // guarda copia a incrementando version

        b.setHolderName("Updated by B"); // modifica titular en copia b desactualizada
        assertThatThrownBy(() -> repository.save(b)) // intenta guardar copia b
                .isInstanceOf(ObjectOptimisticLockingFailureException.class); // espera excepcion de bloqueo optimista
    }
}
