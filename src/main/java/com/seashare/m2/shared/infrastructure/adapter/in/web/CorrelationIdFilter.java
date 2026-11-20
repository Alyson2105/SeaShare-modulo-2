package com.seashare.m2.shared.infrastructure.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP de correlation id (T-22).
 *
 * <p>Lee la cabecera {@code X-Correlation-Id} de la peticion; si no llega, genera
 * un UUID. Lo publica en MDC (para que lo usen los logs y el publicador AMQP)
 * y lo devuelve en la respuesta con la misma cabecera. El valor se limpia de MDC
 * al terminar la peticion.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // se registra antes que el resto de filtros
public class CorrelationIdFilter extends OncePerRequestFilter { // filtro unico por peticion

    public static final String HEADER = "X-Correlation-Id"; // cabecera HTTP de correlacion

    public static final String MDC_KEY = "correlationId"; // clave del MDC compartida con el publicador AMQP

    @Override
    protected void doFilterInternal(HttpServletRequest request, // request de la peticion
                                    HttpServletResponse response, // response donde se refleja el id
                                    FilterChain filterChain) // resto de la cadena
            throws ServletException, IOException { // firmas estandar de servlet
        String incoming = request.getHeader(HEADER); // cabecera enviada por el cliente
        String correlationId = (incoming == null || incoming.isBlank()) // si no viene o viene vacia
                ? UUID.randomUUID().toString() // se genera un id nuevo
                : incoming; // si viene, se respeta
        MDC.put(MDC_KEY, correlationId); // disponible para logs de toda la peticion
        response.setHeader(HEADER, correlationId); // se refleja en la respuesta
        try {
            filterChain.doFilter(request, response); // continua con el resto de la cadena
        } finally {
            MDC.remove(MDC_KEY); // limpia el MDC al terminar, hilo a hilo
        }
    }
}
