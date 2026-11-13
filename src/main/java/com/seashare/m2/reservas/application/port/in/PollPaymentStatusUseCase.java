package com.seashare.m2.reservas.application.port.in;

public interface PollPaymentStatusUseCase { // CU-13 canal B: consulta el estado del cobro en modulo 3

    int execute(); // devuelve cuantas reservas se consultaron con resultado definitivo
}
