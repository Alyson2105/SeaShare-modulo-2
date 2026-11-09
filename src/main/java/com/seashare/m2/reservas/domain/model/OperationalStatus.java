package com.seashare.m2.reservas.domain.model;

public enum OperationalStatus { // estados operativos de embarcacion reconocidos por Modulo 1 (consistencia-m2-m1.md L73-L76)
    AVAILABLE, // Disponible
    RESERVED, // Reservado (bloqueo por pago confirmado)
    IN_NAVIGATION, // En Navegacion
    MAINTENANCE_CLEANING // En Mantenimiento/Limpieza
}
