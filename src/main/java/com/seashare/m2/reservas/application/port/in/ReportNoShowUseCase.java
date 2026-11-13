package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.ReportNoShowCommand;
import com.seashare.m2.reservas.application.dto.ReportNoShowResult;

public interface ReportNoShowUseCase { // CU-05: reportar la inasistencia del arrendatario

    ReportNoShowResult execute(ReportNoShowCommand command); // cancela la reserva y registra el evento
}