package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M") // T-05: un unico nodo ejecuta cada job
public class ShedLockConfig { // configuracion de locks distribuidos sobre la tabla shedlock de V2

    @Bean
    public LockProvider lockProvider(DataSource dataSource) { // proveedor JDBC compartido por los jobs
        return new JdbcTemplateLockProvider( // usa la tabla shedlock existente
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource)) // reutiliza el datasource de la aplicacion
                        .usingDbTime() // reloj de la base de datos, no de cada instancia
                        .build());
    }
}
