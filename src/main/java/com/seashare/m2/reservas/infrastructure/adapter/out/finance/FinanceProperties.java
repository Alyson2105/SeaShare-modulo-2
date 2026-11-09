package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seashare.finance") // propiedades leidas desde application.yaml
public class FinanceProperties { // configuracion de conexion con modulo 3 (CU-11)

    private String mode = "fake"; // fake (default) o http; define que adaptadores se cargan

    private String baseUrl; // url base de modulo 3; requerida solo en modo http

    private long individualConnectTimeoutMs = 100; // timeout de conexion de la estimacion individual

    private long individualReadTimeoutMs = 1000; // timeout de lectura de la estimacion individual

    private long batchConnectTimeoutMs = 150; // timeout de conexion del lote

    private long batchReadTimeoutMs = 1000; // timeout de lectura del lote

    public String getMode() { // obtiene el modo de adaptadores
        return mode; // retorna fake o http
    }

    public void setMode(String mode) { // asigna el modo de adaptadores
        this.mode = mode; // guarda el modo
    }

    public String getBaseUrl() { // obtiene la url base
        return baseUrl; // retorna la url base
    }

    public void setBaseUrl(String baseUrl) { // asigna la url base
        this.baseUrl = baseUrl; // guarda la url base
    }

    public long getIndividualConnectTimeoutMs() { // obtiene el timeout de conexion individual
        return individualConnectTimeoutMs; // retorna milisegundos
    }

    public void setIndividualConnectTimeoutMs(long individualConnectTimeoutMs) { // asigna el timeout de conexion individual
        this.individualConnectTimeoutMs = individualConnectTimeoutMs; // guarda milisegundos
    }

    public long getIndividualReadTimeoutMs() { // obtiene el timeout de lectura individual
        return individualReadTimeoutMs; // retorna milisegundos
    }

    public void setIndividualReadTimeoutMs(long individualReadTimeoutMs) { // asigna el timeout de lectura individual
        this.individualReadTimeoutMs = individualReadTimeoutMs; // guarda milisegundos
    }

    public long getBatchConnectTimeoutMs() { // obtiene el timeout de conexion del lote
        return batchConnectTimeoutMs; // retorna milisegundos
    }

    public void setBatchConnectTimeoutMs(long batchConnectTimeoutMs) { // asigna el timeout de conexion del lote
        this.batchConnectTimeoutMs = batchConnectTimeoutMs; // guarda milisegundos
    }

    public long getBatchReadTimeoutMs() { // obtiene el timeout de lectura del lote
        return batchReadTimeoutMs; // retorna milisegundos
    }

    public void setBatchReadTimeoutMs(long batchReadTimeoutMs) { // asigna el timeout de lectura del lote
        this.batchReadTimeoutMs = batchReadTimeoutMs; // guarda milisegundos
    }
}
