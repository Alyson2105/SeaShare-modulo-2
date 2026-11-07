package com.seashare.m2.reservas.infrastructure.adapter.in.web;

public record ErrorBody(String code, String message) { // estructura inmutable para respuestas de error uniformes
}
