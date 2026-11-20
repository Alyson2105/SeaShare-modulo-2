package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.CancellationEventRecord;

public interface CancellationEventPort { // puerto de salida para registrar el evento de cancelacion

    void save(CancellationEventRecord record); // inserta una fila inmutable en cancellation_event
}