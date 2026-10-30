package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.shared.domain.DomainException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler { // manejador global de excepciones para respuestas uniformes

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class); // logger slf4j para registrar errores internos

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorBody> handleDomainException(DomainException ex) { // procesa excepciones de dominio
        ErrorCode errorCode = ErrorCode.fromInternal(ex.getCode()); // traduce codigo interno al catalogo
        return ResponseEntity.status(errorCode.getHttpStatus()) // asigna status http segun el error
                .body(new ErrorBody(errorCode.getCode(), ex.getMessage())); // construye cuerpo uniforme
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) { // procesa errores de validacion en request body
        String message = ex.getBindingResult().getFieldErrors().isEmpty() // verifica si existen errores de campos
                ? "Invalid request parameters" // mensaje por defecto si no hay errores de campo
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage(); // toma el primer mensaje de validacion
        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // retorna status 400 bad request
                .body(new ErrorBody(ErrorCode.INVALID_PARAMETERS.getCode(), message)); // cuerpo uniforme de validacion
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleException(Exception ex) { // handler generico para capturar cualquier excepcion no controlada y ocultar detalles sensibles
        log.error("Unhandled exception occurred", ex); // registra la traza completa solo en logs internos
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // retorna status 500 internal server error
                .body(new ErrorBody(ErrorCode.INTERNAL_ERROR.getCode(), "Unexpected internal error")); // mensaje estatico seguro sin stack trace
    }
}
