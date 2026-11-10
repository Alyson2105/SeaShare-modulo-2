package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seashare.fleet") // propiedades leidas desde application.yaml
public class FleetProperties { // configuracion de conexion con modulo 1 (CU-09 y CU-10)

    private String mode = "fake"; // fake (default) o http; define que adaptadores se cargan
    private String baseUrl; // url base de modulo 1; requerida solo en modo http
    private String token; // token de servicio interno; en produccion va por variable de entorno
    private long connectTimeoutMs = 100; // timeout de conexion por defecto en milisegundos
    private long readTimeoutMs = 300; // timeout de lectura por defecto en milisegundos

    public String getMode() { // obtiene el modo de adaptadores
        return mode; // retorna fake o http
    }

    public void setMode(String mode) { // asigna el modo de adaptadores
        this.mode = mode; // guarda el modo
    }

    public String getBaseUrl() { // obtiene la url base de modulo 1
        return baseUrl; // retorna la url base
    }

    public void setBaseUrl(String baseUrl) { // asigna la url base de modulo 1
        this.baseUrl = baseUrl; // guarda la url base
    }

    public String getToken() { // obtiene el token de servicio
        return token; // retorna el token
    }

    public void setToken(String token) { // asigna el token de servicio
        this.token = token; // guarda el token
    }

    public long getConnectTimeoutMs() { // obtiene el timeout de conexion
        return connectTimeoutMs; // retorna milisegundos
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) { // asigna el timeout de conexion
        this.connectTimeoutMs = connectTimeoutMs; // guarda milisegundos
    }

    public long getReadTimeoutMs() { // obtiene el timeout de lectura
        return readTimeoutMs; // retorna milisegundos
    }

    public void setReadTimeoutMs(long readTimeoutMs) { // asigna el timeout de lectura
        this.readTimeoutMs = readTimeoutMs; // guarda milisegundos
    }
}
