package com.seashare.m2.shared.domain;

public abstract class DomainException extends RuntimeException { // base de todas las excepciones de dominio

    private final String code; // codigo interno del error (catalogo unico)

    protected DomainException(String code, String message) {
        super(message); // mensaje legible
        this.code = code; // guarda el codigo interno
    }

    public String getCode() {
        return code; // la capa web lo traduce a HTTP
    }
}