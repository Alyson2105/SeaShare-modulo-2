package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seashare.fleet") // propiedades leidas desde application.yaml
public class FleetProperties { // configuracion de conexion con modulo 1 (CU-09 y CU-10)

    private static final Logger log = LoggerFactory.getLogger(FleetProperties.class); // aviso de modo aislado

    private String mode = "fake"; // fake (default) o http; define que adaptadores se cargan
    private String baseUrl; // url base de modulo 1; requerida solo en modo http
    private String token; // token de servicio interno; en produccion va por variable de entorno
    private long connectTimeoutMs = 100; // timeout de conexion por defecto en milisegundos
    private long readTimeoutMs = 300; // timeout de lectura por defecto en milisegundos

    @PostConstruct
    void validate() { // fail-fast: el modo debe estar declarado y http exige destino y token
        if (mode == null || mode.isBlank()) { // perfil sin modo explicito (prod exige declararlo)
            throw new IllegalStateException(
                    "seashare.fleet.mode must be declared (fake or http) in this environment"); // impide arrancar en silencio
        }
        if (!"fake".equalsIgnoreCase(mode) && !"http".equalsIgnoreCase(mode)) { // valor fuera del catalogo
            throw new IllegalStateException(
                    "seashare.fleet.mode must be fake or http, got: " + mode); // error accionable
        }
        if ("http".equalsIgnoreCase(mode)) { // adaptadores reales de M1
            if (baseUrl == null || baseUrl.isBlank()) { // sin destino no se puede llamar a M1
                throw new IllegalStateException(
                        "seashare.fleet.base-url is required when seashare.fleet.mode=http"); // impide arrancar
            }
            if (token == null || token.isBlank()) { // sin token M1 rechaza la llamada
                throw new IllegalStateException(
                        "seashare.fleet.token is required when seashare.fleet.mode=http"); // impide arrancar
            }
            return; // modo real valido
        }
        log.warn("M2 arranca con adaptadores falsos de Modulo 1 (seashare.fleet.mode=fake); "
                + "no se consulta flota real"); // visibilidad del modo aislado
    }

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
