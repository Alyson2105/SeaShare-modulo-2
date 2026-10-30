package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.state.InvalidTransitionException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest { // pruebas unitarias del manejador global de excepciones

    private MockMvc mockMvc; // cliente mockmvc para simular peticiones http sin servidor

    @BeforeEach
    void setUp() { // configura el entorno de pruebas standalone
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean(); // fabrica validadora para bean validation
        validator.afterPropertiesSet(); // inicializa la configuracion del validador

        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController()) // inicializa mockmvc en modo standalone aislando dependencias
                .setControllerAdvice(new GlobalExceptionHandler()) // registra el advice global para interceptar errores
                .setValidator(validator) // asocia el validador para habilitar la anotacion valid
                .build(); // construye la instancia de prueba de mockmvc
    }

    @Test
    void domainErrorIsTranslatedTo409() throws Exception { // traduce excepcion de dominio a estado 409
        mockMvc.perform(get("/test/domain-error")) // invoca endpoint con error de dominio
                .andExpect(status().isConflict()) // verifica codigo http 409
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE")); // valida codigo en cuerpo de respuesta
    }

    @Test
    void validationErrorIs400() throws Exception { // traduce error de validacion a estado 400
        mockMvc.perform(post("/test/validation-error") // invoca endpoint con body invalido
                        .contentType(MediaType.APPLICATION_JSON) // establece encabezado json
                        .content("{\"title\":\"\"}")) // envia campo vacio que incumple notblank
                .andExpect(status().isBadRequest()) // verifica codigo http 400
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida codigo en cuerpo de respuesta
    }

    @Test
    void unexpectedErrorIs500WithoutStackTrace() throws Exception { // error no controlado retorna 500 sin exponer detalles sensibles
        String body = mockMvc.perform(get("/test/unexpected-error")) // invoca endpoint que falla inesperadamente
                .andExpect(status().isInternalServerError()) // verifica codigo http 500
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR")) // valida codigo en cuerpo de respuesta
                .andReturn() // obtiene el resultado de la ejecucion
                .getResponse() // extrae la respuesta servlet
                .getContentAsString(); // obtiene el contenido como texto plano

        assertThat(body).doesNotContain("Exception") // verifica ausencia de nombres de excepcion
                .doesNotContain("at com.") // verifica ausencia de traza de pila
                .doesNotContain("secret internal detail"); // verifica ausencia del mensaje interno sensible
    }

    @RestController
    static class TestController { // controlador auxiliar para emitir excepciones de prueba

        @GetMapping("/test/domain-error")
        public void throwDomainError() { // lanza excepcion de dominio
            throw new InvalidTransitionException(ReservationStatus.INITIATED, ReservationStatus.RESERVED, "Cannot skip payment"); // transicion invalida
        }

        @PostMapping("/test/validation-error")
        public void throwValidationError(@Valid @RequestBody TestRequest request) { // recibe peticion sujeta a validacion
        }

        @GetMapping("/test/unexpected-error")
        public void throwUnexpectedError() { // lanza excepcion no esperada
            throw new RuntimeException("secret internal detail"); // error generico con detalle confidencial
        }
    }

    static class TestRequest { // dto de prueba para validacion de entrada

        @NotBlank
        private String title; // campo requerido no blanco

        public TestRequest() { // constructor por defecto requerido para deserializacion
        }

        public TestRequest(String title) { // constructor con argumentos
            this.title = title; // asigna titulo
        }

        public String getTitle() { // getter para validacion y serializacion
            return title; // retorna titulo
        }

        public void setTitle(String title) { // setter para deserializacion
            this.title = title; // asigna titulo
        }
    }
}
