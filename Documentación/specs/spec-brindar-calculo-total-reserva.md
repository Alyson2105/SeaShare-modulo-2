# Feature Specification: Brindar Cálculo Total de la Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-08 (Evolución arquitectónica y reemplazo conceptual de `Recibir solicitud de pago`)  
**Actores Primarios / Disparador**: Invocación interna desde el caso de uso `Iniciar pago` (`<<include>>`), cuando el Arrendatario titular decide formalizar el pago de su reserva en estado `Borrador`.  
**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: API externa de liquidación final (`Calcular total de reserva` / `Obtener liquidación completa de reserva` [NEEDS CLARIFICATION: confirmar el nombre formal del endpoint en el contrato de API de Módulo 3]). Módulo 3 es el único motor financiero de la plataforma y el único autorizado para liquidar el cobro: calcula y entrega el monto total definitivo y vinculante de la reserva, desglosando tarifa base, seguro náutico obligatorio por pasajero y depósito de garantía retenido temporalmente.
- **Casos de uso internos de Módulo 2**:
  - `Iniciar pago` (`<<include>>`): Invoca este caso de uso para obtener el cálculo final oficial antes de asociar el cobro a la reserva y transicionarla a `Pendiente de Pago`.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Obtener el cálculo total y definitivo de la reserva al iniciar el pago (Priority: P1)

Como Arrendatario titular que ha seleccionado una embarcación y procede a pagar mi reserva en estado `Borrador`, quiero que el sistema solicite a Módulo 3 el cálculo financiero final, vinculante y completo de la reserva (incluyendo alquiler, seguro náutico y depósito de garantía), para conocer exactamente el monto total que se cobrará en la pasarela antes de que se inicie la ventana de pago de 15 minutos (TTL).

A diferencia de la cotización preliminar provista por `Proveer información cotización de reserva` (que era una estimación de vista previa sin depósito de garantía y con bandera de advertencia), este caso de uso se ejecuta en el instante exacto en que el Arrendatario pulsa "Iniciar pago". El sistema recopila los datos consolidados de la reserva (`boat_id`, fecha/hora exacta de check-in, fecha/hora exacta de check-out y cantidad de pasajeros) y consulta la API de liquidación de Módulo 3. Módulo 3 calcula el importe oficial completo: `(tarifa base × duración) + (tarifa de seguro × pasajeros) + depósito de garantía`. Módulo 2 recibe el total y su desglose oficial, los asocia a la reserva sin realizar ninguna operación aritmética y los entrega al caso de uso `Iniciar pago`.

***Why this priority***: Constituye la base financiera vinculante de la contratación. Sin este cálculo final de Módulo 3, no es posible determinar el monto exacto a cobrar en la pasarela ni asociar una cifra definitiva a la reserva en estado `Pendiente de Pago`.

***Independent Test***: Se prueba invocando este caso de uso para una reserva existente en estado `Borrador` contra un simulador de Módulo 3. Se comprueba que Módulo 2: (a) envía los parámetros consolidados del viaje a Módulo 3, (b) recibe de Módulo 3 el monto total y su desglose completo incluyendo tarifa base, seguro y depósito de garantía, (c) no ejecuta redondeos ni sumas aritméticas locales, y (d) entrega la liquidación oficial al flujo de `Iniciar pago`.

***Acceptance Scenarios***:

1. **Scenario**: Obtención exitosa del cálculo total definitivo con desglose integral
   - **Given** una reserva existente en estado `Borrador` con fechas definidas, 3 pasajeros y un Arrendatario titular autenticado
   - **When** el Arrendatario pulsa "Iniciar pago" y el sistema invoca "Brindar cálculo total de la reserva"
   - **Then** el sistema consulta a Módulo 3 y recibe el monto total definitivo, la moneda y el desglose oficial compuesto por: alquiler base, seguro náutico obligatorio por los 3 pasajeros y depósito de garantía, entregando el resultado a `Iniciar pago` sin alterar ningún valor

2. **Scenario**: Asociación del cálculo definitivo previo a la transición a Pendiente de Pago
   - **Given** una reserva en estado `Borrador` que recibe satisfactoriamente el cálculo final desde Módulo 3
   - **When** el flujo de `Iniciar pago` procesa la respuesta
   - **Then** el sistema registra el desglose financiero oficial provisto por Módulo 3 en la reserva e instruye a `Actualizar estado reserva` a transicionarla a `Pendiente de Pago` activando el temporizador TTL de 15 minutos

---

### User Story 2 - Bloqueo de inicio de pago si Módulo 3 no puede calcular el total (Priority: P1)

Como sistema, quiero abortar de forma segura el inicio de pago si la API de Módulo 3 falla, tarda demasiado en responder o rechaza la liquidación por inconsistencia tarifaria del activo, para evitar que el Arrendatario avance al cobro con cifras incorrectas, valores incompletos o montos en cero.

Si Módulo 3 no responde dentro del tiempo límite (*timeout*), o si devuelve un error indicando que la embarcación no posee esquema tarifario completo o depósito de garantía parametrizado, el sistema interrumpe el flujo transaccional. La reserva se mantiene intacta en estado `Borrador`, no se transiciona a `Pendiente de Pago`, no se bloquea la embarcación en Módulo 1 y se presenta un mensaje explicativo al usuario indicando que el servicio de liquidación no se encuentra disponible momentáneamente.

***Why this priority***: Aplica el principio de diseño *fail-safe* para proteger al Arrendatario y al Propietario, garantizando que jamás se inicie una transacción de pago en la pasarela sin una liquidación financiera auditada y validada por Módulo 3.

***Independent Test***: Se prueba simulando una caída de red, un timeout o una respuesta de error 5xx desde Módulo 3 al momento de solicitar el cálculo final. Se verifica que el sistema no genera transiciones de estado, no altera inventario en Módulo 1 y retorna un rechazo controlado hacia `Iniciar pago`.

***Acceptance Scenarios***:

1. **Scenario**: Falla de conexión o timeout al solicitar el cálculo total a Módulo 3
   - **Given** una reserva en estado `Borrador` cuyo Arrendatario solicita iniciar el pago
   - **When** la llamada a la API de Módulo 3 agota el tiempo de espera o falla por desconexión
   - **Then** el sistema cancela la operación de forma segura, mantiene la reserva en estado `Borrador`, no activa el TTL de 15 minutos e informa al usuario que el servicio de cobro no está disponible temporalmente

2. **Scenario**: Rechazo de Módulo 3 por falta de tarifa o depósito de garantía no configurado
   - **Given** una solicitud de cálculo final sobre una embarcación que carece de configuración de depósito de garantía en Módulo 3
   - **When** Módulo 3 devuelve un error de liquidación no procesable
   - **Then** el sistema detiene el flujo de pago, no transiciona la reserva a `Pendiente de Pago` y notifica al Arrendatario la imposibilidad de procesar el pago para esa embarcación

---

### User Story 3 - Garantizar consistencia e inmutabilidad de los rubros monetarios (Priority: P2)

Como plataforma SEA-SHARE, quiero asegurar que Módulo 2 no altere, no descuente y no recalcule ninguno de los rubros provistos por Módulo 3, manteniendo la trazabilidad íntegra entre lo liquidado por Finanzas y lo reflejado en la reserva.

Todos los conceptos entregados por Módulo 3 (tarifa base, seguro de accidentes, depósito de garantía retenido temporalmente, cargos de gestión si aplicaran, y monto total acumulado) se almacenan de forma literal en los atributos financieros de la reserva. Módulo 2 no realiza redondeos locales, ni conversiones cambiarias, ni resta conceptos.

***Why this priority***: Garantiza el cumplimiento de la regla arquitectónica de neutralidad financiera de Módulo 2 y previene descuadres en la reconciliación contable entre la pasarela de pagos y el inventario operativo.

***Independent Test***: Se prueba inyectando una respuesta simulada de Módulo 3 con montos con decimales y desglose explícito de los tres conceptos (alquiler, seguro, depósito de garantía). Se corrobora que la entidad `Reserva` en Módulo 2 almacena con precisión exacta cada monto individual y el total, coincidiendo al 100% con la respuesta de Módulo 3.

***Acceptance Scenarios***:

1. **Scenario**: Almacenamiento literal del desglose completo emitido por Módulo 3
   - **Given** una respuesta de Módulo 3 con desglose: Alquiler = 500.000 COP, Seguro = 45.000 COP, Depósito de Garantía = 200.000 COP, Total = 745.000 COP
   - **When** el sistema recibe la respuesta de cálculo total
   - **Then** el sistema persiste exactamente cada uno de los valores monetarios y el identificador de liquidación entregados por Módulo 3, sin sumar ni restar ningún concepto en Módulo 2

---

### Edge Cases

- **Diferenciación estricta entre Cotización Preliminar y Cálculo Total**:
  - `Proveer información cotización de reserva`: Genera una estimación de vista previa (en catálogo por lote o individual en selección de fechas). No incluye depósito de garantía y lleva la advertencia obligatoria `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"`.
  - `Brindar cálculo total de la reserva`: Genera el valor final, completo y vinculante que se cobra en la pasarela. **SÍ incluye obligatoriamente la tarifa base, el seguro náutico y el depósito de garantía**.
- **Prohibición absoluta de cálculos en Módulo 2**:
  - Módulo 2 **JAMÁS calcula montos, porcentajes, deducciones de comisiones ni seguros**. Módulo 3 es el único responsable matemático y contable.
- **Discrepancia entre la cotización estimada previa y el cálculo total**:
  - El monto del cálculo final en este caso de uso puede ser mayor que la cotización estimada previa debido a la adición obligatoria del depósito de garantía y posibles actualizaciones de tarifas dinámicas o de temporada aplicadas por Módulo 3. Módulo 2 expone el nuevo total oficial desglosado con claridad antes de la redirección a la pasarela.
- **Invocación sobre reservas en estados inválidos**:
  - Si se solicita el cálculo total sobre una reserva que ya se encuentra en `Confirmada`, `En Navegación`, `Completado`, `Cancelado` o `Expirado`, el sistema rechaza de inmediato la solicitud sin contactar a Módulo 3.
- **Reserva en estado `Borrador` con embarcación no disponible en Módulo 1**:
  - Si al momento de solicitar el cálculo total se detecta que la embarcación ya no está disponible en Módulo 1 (por ejemplo, porque otro Arrendatario inició el pago previamente y la pasó a `Reservado`), el sistema cancela el proceso y notifica que el horario ha sido tomado por otro usuario.
- **Moneda del cobro**:
  - Módulo 2 adopta y presenta la moneda (ej. COP, USD) devuelta por Módulo 3 sin realizar conversiones de cambio.
- **Idempotencia ante múltiples pulsaciones de "Iniciar pago"**:
  - Si el Arrendatario presiona repetidamente el botón de pago, el sistema canaliza una única petición activa a Módulo 3 para evitar liquidaciones simultáneas redundantes.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE proveer un caso de uso interno (`Brindar cálculo total de la reserva`) invocado de forma obligatoria por `Iniciar pago` (`<<include>>`) al momento de formalizar el cobro de una reserva.
- **FR-002**: **REGLA ESTRICTA (Sin cálculos de dinero en Módulo 2):** El sistema **NO DEBE en ningún caso calcular, sumar, restar, retener ni redondear importes monetarios**. El cálculo del monto total final, del seguro náutico y del depósito de garantía DEBE ser realizado exclusivamente por Módulo 3.
- **FR-003**: El sistema DEBE recibir los parámetros consolidados de la reserva en estado `Borrador`: identificador de la reserva, identificador de la embarcación (`boat_id`), fecha/hora de inicio, fecha/hora de fin, cantidad de pasajeros e identificador del Arrendatario.
- **FR-004**: El sistema DEBE consultar la API de liquidación final de Módulo 3 enviando los parámetros consolidados del viaje [NEEDS CLARIFICATION: confirmar el nombre formal del endpoint en el contrato de API de Módulo 3].
- **FR-005**: El sistema DEBE recibir de Módulo 3 la liquidación definitiva y completa, que contenga obligatoriamente:
  - Identificador único de liquidación o cálculo emitido por Módulo 3.
  - Importe total final vinculante a cobrar al Arrendatario.
  - Desglose oficial de rubros: tarifa base de alquiler por la duración total, tarifa de seguro náutico acumulada por la totalidad de los pasajeros, y valor del depósito de garantía retenido temporalmente.
  - Código de moneda oficial.
- **FR-006**: El sistema DEBE entregar la liquidación completa al caso de uso `Iniciar pago` y asociar los montos oficiales a la entidad `Reserva` sin ninguna modificación aritmética.
- **FR-007**: Si Módulo 3 devuelve una respuesta de error (embarcación sin tarifas o depósito no configurado), el sistema DEBE interrumpir el flujo de pago, no alterar el estado de la reserva (manteniéndola en `Borrador`) y notificar el error al Arrendatario.
- **FR-008**: Si la comunicación con Módulo 3 agota el tiempo de espera (*timeout*) o se interrumpe por fallo de red, el sistema DEBE aplicar un bloqueo de seguridad (*fail-safe*), abortar la operación, mantener la reserva en `Borrador` y notificar la indisponibilidad temporal del servicio financiero.
- **FR-009**: Si la reserva no se encuentra en estado `Borrador`, el sistema DEBE denegar la solicitud de cálculo total informando la incompatibilidad de estado.
- **FR-010**: El sistema DEBE tratar el cálculo final como la cifra oficial que Módulo 3 procesará posteriormente en la pasarela de pagos al confirmarse el cobro.
- **FR-011**: El sistema DEBE registrar un asiento auditable de la solicitud de cálculo total y de la respuesta recibida de Módulo 3, capturando identificador de reserva, identificador de liquidación, monto total, moneda y marca temporal.

---

### Key Entities

- **Solicitud de Cálculo Total (`TotalCalculationRequest`)**: Parámetros enviados a Módulo 3: `reservation_id`, `boat_id`, `start_time`, `end_time`, `passenger_count`, `renter_id`.
- **Respuesta de Cálculo Total (`TotalCalculationResponse`)**: Liquidación emitida por Módulo 3. Atributos: `calculation_id`, `total_amount`, desglose (`base_rental_amount`, `insurance_total_amount`, `security_deposit_amount`), `currency`, `created_at`.
- **Reserva (`Reservation`)**: Entidad de dominio de Módulo 2 en estado `Borrador` que recibe y almacena los montos del cálculo total definitivo antes de transicionar a `Pendiente de Pago`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los importes finales totales, seguros y depósitos de garantía asociados a las reservas para cobro provienen directamente de Módulo 3, con un cero por ciento (0%) de cálculos o redondeos aritméticos ejecutados en Módulo 2.
- **SC-002**: El 100% de los cálculos finales entregados por este caso de uso incluyen de forma desglosada la tarifa base, el seguro de pasajeros y el depósito de garantía.
- **SC-003**: Cero por ciento (0%) de reservas transicionadas a "Pendiente de Pago" o enviadas a cobro sin haber obtenido exitosamente el cálculo total definitivo de Módulo 3.
- **SC-004**: En el 100% de los casos de falla de red o rechazo de Módulo 3, la reserva permanece intacta en estado `Borrador` sin bloqueos erróneos de inventario en Módulo 1.
- **SC-005**: El tiempo de respuesta de obtención del cálculo total desde la invocación interna hasta la entrega a `Iniciar pago` es menor a [NEEDS CLARIFICATION: definir SLA objetivo de latencia de Módulo 3 para cálculo final, ej. 800 ms].
