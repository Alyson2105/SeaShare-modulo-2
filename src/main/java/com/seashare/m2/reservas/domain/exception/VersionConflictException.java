package com.seashare.m2.reservas.domain.exception;

import com.seashare.m2.shared.domain.DomainException;

public class VersionConflictException extends DomainException { // excepcion por colision optimista de version

    public static final String CODE = "VERSION_CONFLICT"; // codigo interno del catalogo

    public VersionConflictException(String message) { // constructor con mensaje explicativo
        super(CODE, message); // delega en la clase base
    }

    public VersionConflictException(String message, Throwable cause) { // constructor con mensaje y causa original
        super(CODE, message); // delega en la clase base
        if (cause != null) { // verifica causa no nula
            initCause(cause); // asocia causa raiz
        }
    }
}
