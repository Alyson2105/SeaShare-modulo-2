package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "seashare.finance") // propiedades leidas desde application.yaml
public class FinanceProperties { // configuracion de conexion con modulo 3 (CU-11)

    private static final Logger log = LoggerFactory.getLogger(FinanceProperties.class); // aviso de modo aislado

    private String mode = "fake"; // fake (default) o http; define que adaptadores se cargan

    private String baseUrl; // url base de modulo 3; requerida solo en modo http

    @PostConstruct
    void validate() { // P-5: el modo debe estar declarado y http exige destino
        if (mode == null || mode.isBlank()) { // perfil sin modo explicito (prod exige declararlo)
            throw new IllegalStateException(
                    "seashare.finance.mode must be declared (fake or http) in this environment"); // impide arrancar en silencio
        }
        if (!"fake".equalsIgnoreCase(mode) && !"http".equalsIgnoreCase(mode)) { // valor fuera del catalogo
            throw new IllegalStateException(
                    "seashare.finance.mode must be fake or http, got: " + mode); // error accionable
        }
        if ("http".equalsIgnoreCase(mode)) { // adaptadores reales de M3
            if (baseUrl == null || baseUrl.isBlank()) { // modo http sin url
                throw new IllegalStateException(
                        "seashare.finance.base-url is required when seashare.finance.mode=http"); // impide arrancar sin destino
            }
            return; // modo real valido
        }
        log.warn("M2 arranca con adaptadores falsos de Modulo 3 (seashare.finance.mode=fake); "
                + "no se consulta cotizacion real"); // visibilidad del modo aislado
    }

    private long individualConnectTimeoutMs = 100; // timeout de conexion de la estimacion individual

    private long individualReadTimeoutMs = 1000; // timeout de lectura de la estimacion individual

    private long batchConnectTimeoutMs = 150; // timeout de conexion del lote

    private long batchReadTimeoutMs = 1000; // timeout de lectura del lote

    private long calculatedValueConnectTimeoutMs = 100; // timeout de conexion del valor calculado (CU-12)

    private long calculatedValueReadTimeoutMs = 1000; // timeout de lectura del valor calculado (CU-12)

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
}
