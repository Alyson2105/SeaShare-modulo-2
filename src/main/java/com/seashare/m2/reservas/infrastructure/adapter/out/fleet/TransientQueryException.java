package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

class TransientQueryException extends RuntimeException { // fallo transitorio: timeout, conexion o 503 tras el reintento

    TransientQueryException(String message) { // conserva el mismo mensaje de la parte A
        super(message); // mensaje identico al de antes
    }
}
