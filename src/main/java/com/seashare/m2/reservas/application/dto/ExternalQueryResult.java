package com.seashare.m2.reservas.application.dto;

public enum ExternalQueryResult { // resultado final de una consulta externa a M1

    OK, // respuesta 2xx util

    TIMEOUT, // timeout, error de conexion o 503 tras agotar el reintento

    ERROR // 4xx, 5xx sin reintento, timezone invalida o cuerpo ilegible
}
