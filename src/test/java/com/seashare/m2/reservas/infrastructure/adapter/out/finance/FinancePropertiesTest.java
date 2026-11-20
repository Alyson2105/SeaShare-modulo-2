package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class FinancePropertiesTest { // P-5: validacion fail-fast de la url base de M3

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void fallaSiModeHttpNoTieneBaseUrl() { // modo http exige destino configurado
        runner.withPropertyValues("seashare.finance.mode=http")
                .run(context -> {
                    assertThat(context).hasFailed(); // el contexto no debe arrancar
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("seashare.finance.base-url"); // mensaje accionable
                });
    }

    @Test
    void arrancaSiModeHttpTieneBaseUrl() { // con destino el contexto arranca
        runner.withPropertyValues(
                        "seashare.finance.mode=http",
                        "seashare.finance.base-url=http://localhost:8083")
                .run(context -> {
                    assertThat(context).hasNotFailed(); // configuracion valida
                    assertThat(context.getBean(FinanceProperties.class).getBaseUrl())
                            .isEqualTo("http://localhost:8083"); // valor enlazado
                });
    }

    @Test
    void arrancaSinBaseUrlSiModeFake() { // el modo fake no necesita M3
        runner.withPropertyValues("seashare.finance.mode=fake")
                .run(context -> assertThat(context).hasNotFailed()); // sin fail-fast
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FinanceProperties.class)
    static class TestConfig { // registra las propiedades sin levantar toda la aplicacion
    }
}
