package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "seashare.finance", name = "mode", havingValue = "http") // solo en modo http
public class FinanceHttpConfig { // crea los clientes REST de modulo 3 con timeouts por endpoint

    @Bean
    public RestClient individualFinanceRestClient(FinanceProperties properties) { // cliente de la estimacion individual
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory(); // fabrica simple con timeouts
        factory.setConnectTimeout(Duration.ofMillis(properties.getIndividualConnectTimeoutMs())); // 100 ms por defecto
        factory.setReadTimeout(Duration.ofMillis(properties.getIndividualReadTimeoutMs())); // 1000 ms por defecto
        return RestClient.builder() // construye el cliente
                .baseUrl(properties.getBaseUrl()) // url base desde seashare.finance.base-url
                .requestFactory(factory) // aplica la fabrica con timeouts
                .build(); // sin token: modulo 3 no usa Authorization
    }

    @Bean
    public RestClient batchFinanceRestClient(FinanceProperties properties) { // cliente de la estimacion en lote
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory(); // fabrica simple con timeouts
        factory.setConnectTimeout(Duration.ofMillis(properties.getBatchConnectTimeoutMs())); // 150 ms por defecto
        factory.setReadTimeout(Duration.ofMillis(properties.getBatchReadTimeoutMs())); // 1000 ms por defecto
        return RestClient.builder() // construye el cliente
                .baseUrl(properties.getBaseUrl()) // url base desde seashare.finance.base-url
                .requestFactory(factory) // aplica la fabrica con timeouts
                .build(); // sin token: modulo 3 no usa Authorization
    }
}
