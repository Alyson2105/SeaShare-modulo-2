# Feature Specification: Solicitar Tipo de Cancelación

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-06  
**Quién lo activa**: Se llama desde adentro del caso de uso `Solicitar cancelación` (relación `<<include>>`) / Es la conexión con Módulo 3.  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: API externa que recibe el tiempo de anticipación y quién pide la cancelación, y decide el tipo oficial de cancelación: `Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`.

---

## User Scenarios & Testing

### User Story 1 - Saber el tipo de cancelación cuando la pide el Arrendatario (Priority: P1)

Como sistema, quiero preguntarle a la API de Módulo 3 el tipo de cancelación cuando la pide el Arrendatario, para obtener la categoría oficial (`Flexible`, `Moderado` o `Tardío`) según cuánto tiempo falta para el zarpe.

***Por qué esta prioridad***: Es el filtro que asegura que el tipo de cancelación siempre venga de la misma fuente oficial (Módulo 3), sin que Módulo 2 haga ningún cálculo de dinero.

***Cómo probarlo de forma aislada***: Se puede probar simulando la respuesta de Módulo 3, enviando distintos valores de anticipación (por ejemplo, 100 horas, 48 horas y 5 horas), y comprobando que el caso de uso devuelve correctamente "Flexible", "Moderado" y "Tardío" sin calcular ningún porcentaje ni monto de dinero dentro de Módulo 2.

***Escenarios de Aceptación***:

1. **Escenario**: Cancelación con más de 72 horas de anticipación
    - **Dado** que el Arrendatario pide cancelar con 90 horas de anticipación antes del inicio
    - **Cuando** el sistema le pregunta a la API de Módulo 3
    - **Entonces** Módulo 3 devuelve el tipo "Flexible" y el sistema lo entrega al flujo de cancelación para guardarlo como sub-estado

2. **Escenario**: Cancelación en el rango moderado (entre 72 y 24 horas)
    - **Dado** que el Arrendatario pide cancelar con 36 horas de anticipación antes del inicio
    - **Cuando** el sistema le pregunta a la API de Módulo 3
    - **Entonces** Módulo 3 devuelve el tipo "Moderado" y el sistema lo entrega como resultado final

3. **Escenario**: Cancelación tardía (menos de 24 horas)
    - **Dado** que el Arrendatario pide cancelar con 8 horas de anticipación antes del inicio
    - **Cuando** el sistema le pregunta a la API de Módulo 3
    - **Entonces** Módulo 3 devuelve el tipo "Tardío" y el sistema lo entrega para que Módulo 3 aplique la penalidad en su liquidación

---

### User Story 2 - Saber el tipo de cancelación cuando la pide el Propietario (Priority: P1)

Como sistema, quiero preguntarle a Módulo 3 el tipo de cancelación cuando la pide el Propietario, para registrar formalmente la categoría "Por Anfitrión" que garantiza el reembolso total al turista.

***Por qué esta prioridad***: Permite diferenciar claramente cuando falla el anfitrión de cuando el turista simplemente desiste, para que Módulo 3 aplique la compensación correcta.

***Cómo probarlo de forma aislada***: Se prueba simulando la consulta con el actor "Propietario", verificando que en el 100% de los casos Módulo 3 devuelva "Por Anfitrión" y que Módulo 2 no cambie ese resultado.

***Escenarios de Aceptación***:

1. **Escenario**: Cancelación pedida por el Propietario
    - **Dado** que hay una cancelación sobre una reserva confirmada donde quien la pide es el Propietario
    - **Cuando** el sistema envía la consulta a Módulo 3
    - **Entonces** Módulo 3 devuelve el tipo "Por Anfitrión" y el sistema lo entrega al flujo que maneja la cancelación

---

### User Story 3 - Qué hacer si la API de Módulo 3 falla (Priority: P2)

Como sistema, quiero detener la consulta si la API de Módulo 3 falla o no responde, para no guardar datos incorrectos ni inventar reglas de dinero por mi cuenta.

***Por qué esta prioridad***: Mantiene claro qué le toca a cada módulo: si el motor financiero no está disponible, Módulo 2 jamás debe inventar un tipo de cancelación ni adivinar penalidades.

***Cómo probarlo de forma aislada***: Se simula un tiempo de espera agotado (timeout) o un error 503 en la API de Módulo 3; se comprueba que el caso de uso devuelve un error de "servicio no disponible" y no genera ningún tipo de cancelación por defecto.

***Escenarios de Aceptación***:

1. **Escenario**: La API de Módulo 3 no está disponible
    - **Dado** que hay una consulta de tipo de cancelación en curso
    - **Cuando** la API de Módulo 3 no responde dentro del tiempo límite
    - **Entonces** el sistema detiene la consulta, avisa que Módulo 3 no está disponible, y detiene el proceso de cancelación sin cambiar ningún dato

---

### Edge Cases

- **Qué pasa justo en el límite de las horas**:
    - Si la cancelación ocurre justo en el límite exacto (por ejemplo, exactamente 72 horas y 0 segundos, o 24 horas y 0 segundos), Módulo 2 le envía a Módulo 3 el tiempo exacto en minutos; decidir si ese límite cuenta para un lado o para el otro es responsabilidad exclusiva de Módulo 3.
- **La inasistencia (No-Show) no pasa por aquí**:
    - Cuando el Arrendatario no se presenta tras los 30 minutos de tolerancia, ese caso no se maneja con este caso de uso; se maneja aparte con el caso de uso `Marcar inasistencia`, que asigna directamente el sub-estado "Por Inasistencia".
- **Zona horaria del puerto**:
    - El tiempo de anticipación que Módulo 2 calcula y le envía a Módulo 3 debe calcularse usando la zona horaria del puerto donde está la embarcación (dato que da Módulo 1).
- **Módulo 2 nunca calcula dinero**:
    - Módulo 2 **no calcula montos de dinero, reembolsos en efectivo ni descuentos de depósitos**. Módulo 2 solo entrega el tiempo y quién pide la cancelación; Módulo 3 devuelve una etiqueta (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`). El dinero exacto lo mueve Módulo 3 cuando liquida.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE tener una forma de consultar el tipo de cancelación, que se activa siempre desde el caso de uso `Solicitar cancelación` (`<<include>>`).
- **FR-002**: El sistema DEBE recibir estos datos de entrada: el identificador de la reserva, quién pide la cancelación (`Arrendatario` o `Propietario`) y las horas de anticipación calculadas respecto a la fecha y hora pactada de zarpe.
- **FR-003**: El sistema DEBE llamar de forma síncrona a la API externa de Módulo 3, enviándole estos datos para que decida el tipo de cancelación.
- **FR-004**: El sistema DEBE recibir la respuesta de Módulo 3 y verificar que corresponda a uno de estos valores del catálogo oficial:
    - 🔶 [PENDIENTE DE CONFIRMAR — Sub-estados de Cancelación]: `Flexible`, `Moderado`, `Tardío`, `Por Anfitrión` [FIN PENDIENTE].
- **FR-005**: Si quien pide la cancelación es el `Arrendatario`, el sistema DEBE verificar que Módulo 3 aplique estas reglas de tiempo:
    - Tipo `Flexible`: para anticipaciones de más de 72 horas.
    - Tipo `Moderado`: para anticipaciones entre 72 y 24 horas.
    - Tipo `Tardío`: para anticipaciones de menos de 24 horas.
- **FR-006**: Si quien pide la cancelación es el `Propietario`, el sistema DEBE verificar que Módulo 3 devuelva el tipo `Por Anfitrión`.
- **FR-007**: Si la API de Módulo 3 no responde, devuelve un error del servidor (5xx) o se agota el tiempo de espera, el sistema DEBE rechazar la consulta de forma segura, detener el flujo, y NO DEBE generar ningún tipo de cancelación por su cuenta.
- **FR-008**: **REGLA DE NEGOCIO ESTRICTA (sin cálculo financiero):** El sistema **NO DEBE calcular montos de dinero, porcentajes de penalidad, comisiones, ni ejecutar pagos o reembolsos**. Este caso de uso solo conecta el tiempo de la reserva con el tipo de cancelación que decide Módulo 3.
- **FR-009**: El sistema DEBE guardar un registro claro y auditable de cada consulta, con: identificador de la reserva, quién fue consultado, anticipación enviada, tipo devuelto por Módulo 3 y fecha/hora exacta.

---

### Key Entities

- **Consulta de Tipo de Cancelación (`CancellationTypeQuery`)**: objeto interno usado para comunicarse con Módulo 3. Atributos clave: identificador de la reserva, quién solicita (`Arrendatario` / `Propietario`), anticipación calculada en horas y minutos, fecha/hora de la solicitud.
- **Tipo de Cancelación (`CancellationClassification`)**: lo que responde Módulo 3. Atributos clave: el tipo (`Flexible`, `Moderado`, `Tardío`, `Por Anfitrión`), identificador de la transacción en Módulo 3, fecha/hora de la respuesta.
- **Reserva (`Reservation`)**: entidad de Módulo 2 cuya hora de zarpe se usa como base para calcular la anticipación.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: El 100% de los tipos de cancelación son decididos exclusivamente por la API de Módulo 3.
- **SC-002**: Cero (0%) cálculos de montos de dinero, porcentajes de retención o transferencias generados dentro de Módulo 2.
- **SC-003**: El 100% de las consultas a Módulo 3 se resuelven en menos de 400 milisegundos en condiciones normales de red.
- **SC-004**: Cero (0) tipos de cancelación inventados por Módulo 2 ante fallas o indisponibilidad de la API de Módulo 3.
- **SC-005**: El 100% de las solicitudes del Propietario son clasificadas como `Por Anfitrión` por Módulo 3.
