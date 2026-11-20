package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.reservation;

import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.application.port.out.InternalReservationReadPort;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class InternalReservationReadAdapter implements InternalReservationReadPort { // lector jdbc de solo lectura para CU-15

    // Solo columnas necesarias: nunca se seleccionan columnas de dinero (regla "Sin dinero").
    private static final String SELECT_RESERVATION = """
            SELECT id, reservation_code, renter_id, vessel_id, owner_id,
                   start_date, end_date, departure_time, harbor_timezone, passengers,
                   status, cancellation_subtype, ttl_expires_at,
                   actual_departure_at, actual_arrival_at, completion_notes,
                   created_at, updated_at
              FROM reservation
             WHERE id = ?
            """; // reserva por clave primaria, consulta de bajo costo

    // Ultima fila de auditoria que llevo la reserva a CANCELLED. Se ordena por occurred_at
    // descendente y se limita a una fila para obtener el actor real de esa cancelacion.
    private static final String SELECT_CANCELLATION_ACTOR = """
            SELECT actor
              FROM reservation_status_audit
             WHERE reservation_id = ?
               AND to_status = 'CANCELLED'
             ORDER BY occurred_at DESC
             LIMIT 1
            """; // actor de la cancelacion mas reciente

    // Anticipacion registrada en el evento inmutable de cancelacion. Sin evento (NO_SHOW, etc.)
    // la consulta no devuelve filas y la anticipacion se informa nula.
    private static final String SELECT_ANTICIPATION = """
            SELECT anticipation_minutes
              FROM cancellation_event
             WHERE reservation_id = ?
               AND actor = 'RENTER'
            """; // anticipacion real registrada por CU-04

    private final JdbcTemplate jdbcTemplate; // acceso jdbc de solo lectura

    public InternalReservationReadAdapter(JdbcTemplate jdbcTemplate) { // constructor con plantilla jdbc
        this.jdbcTemplate = jdbcTemplate; // asigna plantilla
    }

    @Override
    public Optional<InternalReservationView> findById(UUID reservationId) { // busca la reserva y su actor de cancelacion
        String cancellationActor = jdbcTemplate.query(SELECT_CANCELLATION_ACTOR, // consulta el actor de auditoria
                rs -> rs.next() ? rs.getString("actor") : null, reservationId); // toma la fila mas reciente o nulo
        Long anticipationMinutes = jdbcTemplate.query(SELECT_ANTICIPATION, // consulta la anticipacion persistida
                rs -> rs.next() ? rs.getLong("anticipation_minutes") : null, reservationId); // fila unica o nula

        return jdbcTemplate.query(SELECT_RESERVATION, rs -> { // consulta la reserva principal
            if (!rs.next()) { // sin filas para ese identificador
                return Optional.<InternalReservationView>empty(); // informa ausencia
            }
            return Optional.of(mapRow(rs, cancellationActor, anticipationMinutes)); // mapea la fila encontrada
        }, reservationId);
    }

    private InternalReservationView mapRow(ResultSet rs, String cancellationActor,
                                           Long anticipationMinutes) throws SQLException { // mapea columnas a la vista
        String subtype = rs.getString("cancellation_subtype"); // subtipo puede ser nulo

        return new InternalReservationView(
                rs.getObject("id", UUID.class), // id de la reserva
                rs.getString("reservation_code"), // codigo de negocio
                rs.getObject("renter_id", UUID.class), // arrendatario
                rs.getObject("vessel_id", UUID.class), // embarcacion
                rs.getObject("owner_id", UUID.class), // propietario
                rs.getObject("start_date", LocalDate.class), // fecha de inicio
                rs.getObject("end_date", LocalDate.class), // fecha de fin
                rs.getObject("departure_time", LocalTime.class), // hora de zarpe
                rs.getString("harbor_timezone"), // zona horaria del puerto
                rs.getInt("passengers"), // pasajeros
                ReservationStatus.valueOf(rs.getString("status")), // estado interno
                subtype == null ? null : CancellationSubtype.valueOf(subtype), // subtipo opcional
                toInstant(rs.getTimestamp("ttl_expires_at")), // vencimiento ttl
                toInstant(rs.getTimestamp("actual_departure_at")), // check-in real
                toInstant(rs.getTimestamp("actual_arrival_at")), // check-out real
                rs.getString("completion_notes"), // novedades de cierre
                toInstant(rs.getTimestamp("created_at")), // creacion
                toInstant(rs.getTimestamp("updated_at")), // actualizacion
                cancellationActor, // actor crudo de auditoria
                0, // dias: los calcula el servicio
                0, // noches: las calcula el servicio
                null, // actor efectivo: lo resuelve el servicio
                anticipationMinutes, // anticipacion persistida o nula
                false, // ttl: lo resuelve el servicio con el reloj
                null // segundos restantes: los calcula el servicio
        );
    }

    private Instant toInstant(Timestamp timestamp) { // convierte timestamp jdbc a instante
        return timestamp == null ? null : timestamp.toInstant(); // admite columnas nulas
    }
}
