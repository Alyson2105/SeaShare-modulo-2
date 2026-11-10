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

    private long calculatedValueConnectTimeoutMs = 100; // timeout de conexion del valor calculado (CU-12)

    private long calculatedValueReadTimeoutMs = 1000; // timeout de lectura del valor calculado (CU-12)

    private int max404Retries = 3; // reintentos del 404 del valor calculado

    private long retryDelayMs = 300; // espera entre reintentos del 404

    private long maxTotalMs = 800; // tope total de tiempo para reintentar el 404

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

    public long getCalculatedValueConnectTimeoutMs() { // obtiene el timeout de conexion del valor calculado
        return calculatedValueConnectTimeoutMs; // retorna milisegundos
    }

    public void setCalculatedValueConnectTimeoutMs(long calculatedValueConnectTimeoutMs) { // asigna el timeout de conexion del valor calculado
        this.calculatedValueConnectTimeoutMs = calculatedValueConnectTimeoutMs; // guarda milisegundos
    }

    public long getCalculatedValueReadTimeoutMs() { // obtiene el timeout de lectura del valor calculado
        return calculatedValueReadTimeoutMs; // retorna milisegundos
    }

    public void setCalculatedValueReadTimeoutMs(long calculatedValueReadTimeoutMs) { // asigna el timeout de lectura del valor calculado
        this.calculatedValueReadTimeoutMs = calculatedValueReadTimeoutMs; // guarda milisegundos
    }

    public int getMax404Retries() { // obtiene los reintentos del 404
        return max404Retries; // retorna cantidad
    }

    public void setMax404Retries(int max404Retries) { // asigna los reintentos del 404
        this.max404Retries = max404Retries; // guarda cantidad
    }

    public long getRetryDelayMs() { // obtiene la espera entre reintentos del 404
        return retryDelayMs; // retorna milisegundos
    }

    public void setRetryDelayMs(long retryDelayMs) { // asigna la espera entre reintentos del 404
        this.retryDelayMs = retryDelayMs; // guarda milisegundos
    }

    public long getMaxTotalMs() { // obtiene el tope total del 404
        return maxTotalMs; // retorna milisegundos
    }

    public void setMaxTotalMs(long maxTotalMs) { // asigna el tope total del 404
        this.maxTotalMs = maxTotalMs; // guarda milisegundos
    }
}
