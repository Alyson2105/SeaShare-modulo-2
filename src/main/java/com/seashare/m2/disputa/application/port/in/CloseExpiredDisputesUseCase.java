package com.seashare.m2.disputa.application.port.in;

public interface CloseExpiredDisputesUseCase { // cierre automatico de la ventana de 24 h (CU-16 FR-008)

    int execute(); // cierra las disputas vencidas sin reclamo y devuelve cuantas proceso
}
