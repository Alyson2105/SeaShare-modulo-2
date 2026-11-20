package com.seashare.m2.reservas.application.dto;

public enum PaymentStatus { // estado del cobro reportado por modulo 3 (UC06)

    IN_PROCESS, // la pasarela aun no reporta resultado definitivo

    APPROVED, // cobro aprobado

    REJECTED, // cobro rechazado; permite reintento si queda TTL

    FAILED, // cobro fallido de forma definitiva (resultado externo FAILED)

    CANCELLED, // operacion cancelada por M3

    EXPIRED, // autorizacion vencida sin captura

    UNKNOWN // estado no determinado (se trata como IN_PROCESS)
}
