package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.idempotency;

import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyAdapter implements IdempotencyPort { // adaptador de infraestructura para control de idempotencia con spring data jpa

    private final IdempotencyKeyJpaRepository repository; // repositorio spring data jpa de claves de idempotencia

    public IdempotencyAdapter(IdempotencyKeyJpaRepository repository) { // inyeccion del repositorio jpa
        this.repository = repository; // asigna el repositorio
    }

    // [NEEDS CLARIFICATION] el plan no define que pasa con la misma clave y distinto cuerpo: se devuelve la reserva original sin comparar el cuerpo;
    // [NEEDS CLARIFICATION] pasados 60 s la clave expira y se reutiliza la fila (la llave primaria es key+endpoint).
    // la tabla idempotency_key tiene primary key (key, endpoint); tryClaim usa insert ... on conflict de forma atomica
    // y no debe cambiarse a insert simple con try/catch porque en postgresql una violacion de restriccion aborta
    // la transaccion en curso y la consulta posterior fallaria con "current transaction is aborted".
    // JPA no soporta ON CONFLICT; por eso la sentencia es nativa dentro del repositorio Spring Data.
    @Override
    public boolean tryClaim(String key, String endpoint, Instant now, Duration window) { // reclama clave de forma atomica con upsert condicional
        int rowsAffected = repository.tryClaim( // ejecuta upsert atomico en el repositorio jpa
                key, // clave idempotente
                endpoint, // endpoint que reclama la clave
                now.atOffset(ZoneOffset.UTC), // marca temporal actual en utc
                now.minus(window).atOffset(ZoneOffset.UTC)); // umbral temporal limite en utc
        return rowsAffected == 1; // true si inserto o actualizo por expiracion
    }

    @Override
    public void complete(String key, String endpoint, UUID reservationId) { // asocia la reserva a la clave completada
        int rowsAffected = repository.complete(reservationId.toString(), key, endpoint); // persiste id de reserva completada
        if (rowsAffected != 1) { // verifica que se actualizo exactamente una fila
            throw new IllegalStateException("No idempotency key claimed for endpoint"); // lanza si no hay reclamo previo
        }
    }

    @Override
    public Optional<UUID> findReservationId(String key, String endpoint) { // consulta referencia de reserva existente
        Optional<String> responseRef = repository.findResponseRef(key, endpoint); // consulta clave por endpoint
        if (responseRef.isEmpty() || responseRef.get().isBlank()) { // valida presencia y no vacuidad
            return Optional.empty(); // retorna vacio si no hay fila o es nulo
        }
        try {
            return Optional.of(UUID.fromString(responseRef.get().trim())); // transforma texto a uuid
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // retorna vacio si formato es invalido
        }
    }
}
