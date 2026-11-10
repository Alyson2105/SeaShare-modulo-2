package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "http") // solo en modo http
public class FleetHttpConfig { // crea el cliente REST de flota con timeouts cortos

    @Bean
    public RestClient fleetRestClient(FleetProperties properties) { // unico cliente hacia modulo 1
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory(); // fabrica simple con timeouts
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs())); // 100 ms: red interna
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs())); // 300 ms: SLA de consulta
        RestClient.Builder builder = RestClient.builder() // construye el cliente
                .baseUrl(properties.getBaseUrl()) // url base desde seashare.fleet.base-url
                .requestFactory(factory); // aplica la fabrica con timeouts
        if (properties.getToken() != null && !properties.getToken().isBlank()) { // token opcional en desarrollo
            builder.defaultHeader("X-Internal-Service-Token", properties.getToken()); // viaja en cada llamada
        }
        return builder.build(); // devuelve el cliente listo
    }
}
