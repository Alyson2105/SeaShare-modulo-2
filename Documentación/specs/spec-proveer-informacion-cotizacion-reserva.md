# Feature Specification: Proveer Información de Cotización de Reserva

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-06 (Actualizado a arquitectura dual lote/individual: 2026-09-08)  
**Actores Primarios / Disparadores**:
- **Arrendatario / Pantalla de Listado y Búsqueda de Embarcaciones**: Disparador del **Modo Lote** (*Previsualización en Pantalla de Carga/Catálogo*), cuando el usuario explora múltiples opciones antes de seleccionar fechas y pasajeros específicos.
- **Caso de Uso Interno `Iniciar reserva` (`<<include>>`)**: Disparador del **Modo Individual** (*Cotización Exacta*), invocado de forma obligatoria cuando el Arrendatario ya definió fechas y pasajeros, inmediatamente antes de persistir la reserva en estado "Pendiente de Pago".

**Dependencias Externas (APIs)**:
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: API externa de tarifas y liquidación (`Proveer cotización de reserva` / `Solicitar cotización de reserva` - Contrato UC01 de Módulo 3). Constituye el único motor financiero y tarifario de la plataforma. Ofrece dos modalidades operativas:
  1. *Modo en Lote (`batch`)*: Recibe una colección de identificadores de embarcaciones (`boat_ids`), asume valores por defecto de 1 día de duración y 1 pasajero, y devuelve cotizaciones estimadas para múltiples activos en una sola consulta.
  2. *Modo Individual (`single`)*: Recibe un `boat_id`, fecha/hora de inicio, fecha/hora de fin y cantidad de pasajeros; valida las fechas de servicio, calcula el costo total exacto según la fórmula contractual `(tarifa base × duración) + (tarifa de seguro × pasajeros)` y devuelve el precio junto con una bandera de advertencia obligatoria inmutable.
- **Casos de Uso Internos de Módulo 2**:
  - `Iniciar reserva` (`<<include>>`): Consume de forma sincrónica la cotización individual para asociar el precio oficial a la reserva temporal antes de iniciar el temporizador TTL de 15 minutos.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Previsualizar cotizaciones estimadas en lote para el catálogo de embarcaciones (Priority: P1)

Como Arrendatario navegando por la pantalla de catálogo o listado de embarcaciones disponibles, quiero visualizar una tarifa base estimada de referencia para cada embarcación antes de elegir fechas concretas o cantidad de pasajeros, para comparar precios rápidamente y decidir cuál embarcación explorar en detalle.

El sistema recopila los identificadores de las embarcaciones visibles (`boat_ids`) y realiza una consulta en lote a la API de Módulo 3. Módulo 3 procesa la solicitud asumiendo por defecto 1 día de duración y 1 pasajero, y entrega la tarifa base estimada para cada activo. Para esta previsualización en catálogo, la cotización excluye estrictamente el depósito de garantía y el seguro náutico, presentando únicamente la tarifa base entregada por Módulo 3. Módulo 2 recibe estos valores y los entrega a la interfaz de catálogo tal cual, sin alterar, redondear ni recalcular ningún monto.

***Why this priority***: Es el punto de entrada a la experiencia de descubrimiento del usuario. Permite comparar costos iniciales en el marketplace sin obligar al usuario a completar formularios de fechas previamente, incrementando la conversión y exploración del catálogo.

***Independent Test***: Se prueba enviando una lista de identificadores de embarcaciones a la interfaz en lote de Módulo 2 simulando la respuesta en lote de Módulo 3. Se valida que Módulo 2: (a) efectúe la petición en lote a Módulo 3, (b) reciba las tarifas base estimadas sin seguro ni depósito, (c) no efectúe operaciones matemáticas locales, y (d) entregue los valores íntegros a la capa de presentación.

***Acceptance Scenarios***:

1. **Scenario**: Obtención y despliegue exitoso de estimaciones en lote para catálogo
   - **Given** una lista de 15 identificadores de embarcaciones (`boat_ids`) visibles en el catálogo de búsqueda
   - **When** la pantalla de catálogo solicita las cotizaciones de previsualización
   - **Then** el sistema consulta a Módulo 3 en modo lote, recibe la tarifa base estimada para cada una de las 15 embarcaciones (sin seguro ni depósito) y las entrega a la pantalla sin modificar ningún valor numérico

2. **Scenario**: Embarcaciones del lote sin tarifa configurada o no cotizables en Módulo 3
   - **Given** un lote de 10 embarcaciones donde 2 de ellas no tienen tarifas configuradas en Módulo 3
   - **When** Módulo 3 procesa la solicitud en lote y excluye o marca esas 2 embarcaciones como no cotizables
   - **Then** el sistema procesa exitosamente las tarifas de las 8 embarcaciones restantes, entrega sus precios a la interfaz, y marca las 2 embarcaciones no cotizadas con el estado "Cotización no disponible", sin tumbar ni interrumpir el despliegue del resto del catálogo

3. **Scenario**: Lista de embarcaciones del lote que supera el límite máximo por petición de Módulo 3
   - **Given** un catálogo con 120 embarcaciones que supera el límite máximo de 50 embarcaciones por solicitud permitido por Módulo 3 [NEEDS CLARIFICATION: confirmar si el umbral máximo de lote de Módulo 3 es de 50 o 100 embarcaciones]
   - **When** el sistema solicita la cotización en lote para todo el conjunto
   - **Then** el sistema fragmenta automáticamente la solicitud en sub-lotes conformes al límite (ej. dos lotes de 50 y uno de 20), envía las consultas correspondientes a Módulo 3, consolida todas las respuestas en un único mapa de resultados y lo entrega a la interfaz de catálogo

4. **Scenario**: Consulta de lote con lista de identificadores vacía
   - **Given** que la pantalla de búsqueda no tiene embarcaciones para listar (cero resultados coincidentes con los filtros del usuario)
   - **When** se solicita la cotización en lote con una lista vacía de `boat_ids`
   - **Then** el sistema no ejecuta ninguna llamada remota hacia Módulo 3 y retorna inmediatamente una colección vacía

---

### User Story 2 - Obtener cotización exacta e individual con advertencia obligatoria para iniciar reserva (Priority: P1)

Como Arrendatario que ha seleccionado una embarcación específica, un rango de fechas (días de inicio y fin) y una cantidad de pasajeros, quiero que el sistema obtenga la cotización exacta calculada oficialmente por Módulo 3 junto con las advertencias contractuales aplicables, para que el caso de uso `Iniciar reserva` cree mi reserva en estado "Pendiente de Pago" con el costo oficial definitivo.

El caso de uso `Iniciar reserva` invoca este caso de uso (`<<include>>`) en modo individual enviando el `boat_id`, la fecha/hora de check-in, la fecha/hora de check-out y el número de pasajeros. Módulo 3 valida las fechas (comprobando que no estén en el pasado, que el fin sea posterior al inicio, formato válido y duración mayor a 0 días) y calcula el precio exacto aplicando su fórmula contractual `(tarifa base × duración) + (tarifa de seguro × pasajeros)`. Módulo 3 retorna el precio consolidado con su desglose y una **bandera de advertencia obligatoria** cuyo texto exacto es: `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"`. Módulo 2 recibe estos datos, preserva la advertencia al pie de la letra y los transfiere a `Iniciar reserva`, quien persiste la reserva en "Pendiente de Pago" y activa el temporizador de 15 minutos (TTL).

***Why this priority***: Es el componente de integración indispensable para el flujo de contratación del MVP. Garantiza la validez legal y financiera de la reserva, impidiendo que Módulo 2 cree reservas con precios arbitrarios o sin la advertencia contractual preceptiva.

***Independent Test***: Se prueba invocando la cotización individual con un `boat_id`, rango de fechas y número de pasajeros contra un simulador de Módulo 3. Se verifica que Módulo 2: (a) transfiera los parámetros sin alteración, (b) reciba el desglose exacto (alquiler, seguro por pasajero, moneda), (c) reciba y exponga la advertencia obligatoria sin cambios sintácticos, y (d) entregue los datos a `Iniciar reserva` sin ejecutar cálculos aritméticos locales.

***Acceptance Scenarios***:

1. **Scenario**: Obtención exitosa de cotización exacta con advertencia obligatoria
   - **Given** un Arrendatario que seleccionó una embarcación disponible, 3 días de duración y 4 pasajeros
   - **When** `Iniciar reserva` invoca la cotización individual a Módulo 3
   - **Then** el sistema recibe de Módulo 3 el precio exacto desglosado (alquiler base + seguro por 4 pasajeros), la moneda y la advertencia obligatoria `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"`, transfiriéndolos intactos a `Iniciar reserva` para persistir la reserva en "Pendiente de Pago"

2. **Scenario**: Rechazo por fechas inválidas validado por Módulo 3
   - **Given** una solicitud con fecha de inicio en el pasado, o fecha de fin anterior a la de inicio, o duración efectiva de 0 días
   - **When** se envía la solicitud individual a Módulo 3
   - **Then** Módulo 3 rechaza la petición retornando un error controlado de validación temporal, y el sistema traslada dicho error a `Iniciar reserva` informando al usuario el motivo específico sin crear la reserva

3. **Scenario**: Embarcación individual sin tarifas configuradas en Módulo 3
   - **Given** una embarcación seleccionada que no tiene esquema tarifario activo en Módulo 3
   - **When** se solicita la cotización individual a Módulo 3
   - **Then** Módulo 3 devuelve un error de activo no cotizable, el sistema bloquea de inmediato la creación de la reserva y notifica al Arrendatario que la embarcación no puede reservarse por inconsistencia tarifaria

---

### User Story 3 - Recotización automática ante modificación de parámetros del viaje (Priority: P2)

Como Arrendatario ajustando mi itinerario en pantalla antes de confirmar la reserva, quiero que al modificar la cantidad de pasajeros o las fechas seleccionadas se genere una nueva cotización oficial de Módulo 3, para asegurar que el valor reflejado y contratado corresponda exactamente a los parámetros finales de mi viaje.

Si el Arrendatario modifica cualquier variable que afecte el cálculo tarifario (número de pasajeros —que altera las pólizas de seguro náutico— o el rango de días —que altera la duración o tarifas de fin de semana/temporada—) antes de que la reserva se persista formalmente, la cotización individual previamente recibida queda automáticamente invalidada y se descarta. El sistema dispara una nueva solicitud individual a Módulo 3 con los parámetros actualizados.

***Why this priority***: Previene discrepancias financieras y cobros indebidos originados por parámetros desfasados, garantizando la total concordancia entre lo cotizado por Módulo 3 y la reserva creada.

***Independent Test***: Se prueba solicitando una cotización individual para 2 pasajeros en un rango de fechas, modificando posteriormente el selector a 5 pasajeros antes de persistir la reserva. Se verifica que la primera cotización se destruye en memoria y el sistema emite una nueva llamada a Módulo 3 obteniendo el precio actualizado para 5 personas.

***Acceptance Scenarios***:

1. **Scenario**: Cambio de pasajeros o fechas actualiza la cotización individual
   - **Given** una cotización individual previa obtenida para 2 pasajeros y 2 días de navegación
   - **When** el Arrendatario cambia la selección a 4 pasajeros antes de confirmar la reserva
   - **Then** el sistema descarta la cotización anterior, invoca nuevamente a Módulo 3 en modo individual con 4 pasajeros y recibe el nuevo desglose tarifario actualizado

2. **Scenario**: Control de solicitudes rápidas sucesivas (descarte de respuestas obsoletas)
   - **Given** un Arrendatario que modifica repetidamente las fechas en el selector en pocos segundos
   - **When** se generan múltiples solicitudes asíncronas hacia Módulo 3
   - **Then** el sistema descarta las respuestas correspondientes a peticiones previas obsoletas y conserva exclusivamente la cotización vinculada a la última selección realizada por el usuario

---

### User Story 4 - Resiliencia y aislamiento ante fallas de comunicación con Módulo 3 (Priority: P2)

Como sistema, quiero gestionar de manera segura los errores de comunicación, caídas de red o demoras excesivas (*timeouts*) de la API de Módulo 3 en cualquiera de sus dos modos, para evitar que la plataforma genere reservas inconsistentes, corruptas o con montos en cero, manteniendo la experiencia de usuario bajo control.

Si Módulo 3 no responde dentro del umbral de tiempo límite o retorna un error 5xx:
- En **Modo Lote**: El sistema no bloquea ni rompe la pantalla de catálogo; despliega los activos indicando que las cotizaciones están temporalmente no disponibles.
- En **Modo Individual**: El sistema cancela el proceso transaccional de creación de reserva de forma segura (*fail-safe*), no persiste ningún registro en base de datos y presenta al Arrendatario un mensaje claro sobre la indisponibilidad del servicio de liquidación.
En ningún caso Módulo 2 asume precios por defecto, ni completa valores faltantes ni inventa cifras.

***Why this priority***: Protege la integridad del modelo de negocio de SEA-SHARE y previene la suscripción de contratos de alquiler vinculantes con tarifas erróneas o gratuitas debidas a contingencias técnicas externas.

***Independent Test***: Se prueba simulando desconexión de red o demora superior al límite de tiempo en Módulo 3 para ambos modos. Se comprueba que en lote el catálogo sigue navegable con etiquetas informativas, y que en individual se detiene el caso de uso `Iniciar reserva` sin persistir nada en "Pendiente de Pago".

***Acceptance Scenarios***:

1. **Scenario**: Falla o timeout de Módulo 3 en modo lote no interrumpe el catálogo
   - **Given** que la pantalla de búsqueda invoca la cotización en lote para 20 embarcaciones
   - **When** la API de Módulo 3 no responde a tiempo o entrega un error de servidor interno
   - **Then** el sistema captura la excepción, no detiene la navegación del catálogo y muestra las embarcaciones con la leyenda "Tarifa no disponible temporalmente"

2. **Scenario**: Falla o timeout de Módulo 3 en modo individual aborta la creación de reserva
   - **Given** una solicitud de cotización individual emitida desde `Iniciar reserva`
   - **When** se agota el tiempo de espera hacia Módulo 3 o la conexión falla
   - **Then** el sistema cancela la operación de forma segura, no genera reservas en estado "Pendiente de Pago", no inventa valores monetarios e informa al Arrendatario que el servicio de cotización no se encuentra disponible momentáneamente

---

### Edge Cases

- **Prohibición estricta de cálculos y manipulación de dinero en Módulo 2**:
  - Módulo 2 **JAMÁS calcula, suma, resta, redondea, aplica comisiones ni deduce seguros o garantías**. Todo valor monetario proviene exclusivamente de Módulo 3 y se presenta/almacena de forma literal.
- **Diferencia de alcance tarifario entre modo lote y modo individual**:
  - En **Modo Lote (Previsualización)**: Se expone exclusivamente la tarifa base estimada entregada por Módulo 3. No incluye ni seguro náutico ni depósito de garantía, reflejando el costo referencial base de la embarcación (calculado bajo el estándar de 1 día y 1 pasajero por defecto en Módulo 3).
  - En **Modo Individual (Cotización Exacta)**: Incluye la totalidad de los conceptos calculados por Módulo 3 para el viaje específico (tarifa base × duración + seguro náutico × pasajeros), junto con la bandera de advertencia obligatoria.
- **Inmutabilidad de la bandera de advertencia obligatoria**:
  - El texto recibido de Módulo 3: `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"` DEBE presentarse exactamente tal cual, sin alterar mayúsculas, signos de puntuación, ni omitir palabras, tanto en la interfaz de visualización previa como en el traspaso a `Iniciar reserva`.
- **Embarcación sin tarifa configurada**:
  - En *Modo Lote*: Se excluye del consolidado o se reporta como no cotizable de forma individual, sin afectar la entrega de precios de las demás embarcaciones del lote.
  - En *Modo Individual*: Detiene inmediatamente el flujo de reserva e impide la creación de la misma.
- **Superación del límite de IDs en peticiones en lote**:
  - Si la cantidad de embarcaciones a cotizar en lote sobrepasa el límite máximo permitido por Módulo 3 (50 embarcaciones por solicitud [NEEDS CLARIFICATION: confirmar si el umbral máximo de lote de Módulo 3 es de 50 o 100 embarcaciones]), Módulo 2 particiona la solicitud en bloques de tamaño menor o igual al límite, consulta a Módulo 3 para cada bloque y unifica las respuestas en una sola colección antes de retornar a la pantalla.
- **Colección de IDs vacía o con duplicados en modo lote**:
  - Si la lista de `boat_ids` está vacía, Módulo 2 retorna una respuesta vacía inmediatamente sin invocar la red.
  - Si la lista contiene identificadores repetidos, Módulo 2 los deduplica antes de construir la solicitud hacia Módulo 3.
- **Identificadores de embarcaciones inexistentes en Módulo 3**:
  - Si en el modo lote se envían IDs que no existen en los registros de tarifas de Módulo 3, Módulo 3 los omite o marca como inexistentes sin afectar a las embarcaciones válidas.
- **Validación de fechas rechazada por Módulo 3 en modo individual**:
  - Módulo 3 valida formalmente que: (a) la fecha de inicio no esté en el pasado, (b) la fecha de fin sea estrictamente posterior al inicio, (c) el formato cumpla ISO 8601, y (d) la duración no sea cero días. Módulo 2 no repite esta validación tarifaria, sino que traslada el código de error y mensaje estructurado recibido de Módulo 3 hacia el usuario y el caso de uso `Iniciar reserva`.
- **Valores monetarios anómalos (cero o negativos)**:
  - Si Módulo 3 retorna un valor total menor o igual a cero (0) en modo individual sin una justificación de beneficio o promoción autorizada, el sistema aplica un bloqueo preventivo y rechaza continuar con la reserva.
- **Respeto de la moneda oficial**:
  - Módulo 2 registra y exhibe el código de moneda (ej. COP, USD) devuelto por Módulo 3 en la respuesta, sin efectuar conversiones cambiarias locales ni asumir divisas predeterminadas.
- **Naturaleza de solo lectura de la cotización (sin retención de fondos ni compromisos contables)**:
  - La cotización en ambos modos es una operación de consulta de solo lectura, idempotente y sin efectos secundarios contables en Módulo 3. No retiene cupos, no realiza cargos en pasarelas ni compromete fondos. Si el Arrendatario abandona el proceso o la reserva no se confirma, no existe ningún proceso de compensación ni reversión (*rollback*) en Módulo 3.
- **Concurrencia sobre inventario**:
  - Obtener una cotización exitosa no reserva ni garantiza la disponibilidad del activo en el calendario. La reserva del activo y la resolución de concurrencia entre usuarios que compiten por las mismas fechas se ejecutan de manera atómica al persistir la reserva en el caso de uso `Iniciar reserva`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE proveer una interfaz de cotización de reservas que gestione dos modos de operación desacoplados:
  1. **Modo Lote**: Previsualización masiva de estimaciones para el catálogo y pantalla de carga de embarcaciones.
  2. **Modo Individual**: Cotización formal y exacta para la creación de una reserva dentro del caso de uso `Iniciar reserva`.
- **FR-002**: **REGLA ESTRICTA (Sin cálculos de dinero en Módulo 2):** El sistema **NO DEBE en ningún caso calcular, sumar, restar, multiplicar por duración, aplicar porcentajes ni redondear importes monetarios**. Toda cifra de tarifa base, seguro náutico, cargos o totales DEBE ser generada y liquidada exclusivamente por Módulo 3.
- **FR-003**: En **Modo Lote**, el sistema DEBE recibir una lista de identificadores de embarcaciones (`boat_ids`) desde la pantalla de listado/catálogo y solicitar a la API en lote de Módulo 3 (`Solicitar cotización de reserva`) las estimaciones correspondientes.
- **FR-004**: En **Modo Lote**, el sistema DEBE deduplicar los identificadores de embarcaciones antes de emitir la consulta a Módulo 3. Si la lista resultante se encuentra vacía, el sistema DEBE retornar una respuesta vacía inmediatamente sin realizar invocaciones remotas.
- **FR-005**: En **Modo Lote**, si la cantidad de identificadores de embarcaciones a cotizar excede el límite máximo por solicitud fijado por Módulo 3 (fijado en 50 embarcaciones [NEEDS CLARIFICATION: confirmar si el umbral máximo de lote de Módulo 3 es de 50 o 100 embarcaciones]), el sistema DEBE segmentar la petición en múltiples sub-lotes que no superen dicho umbral, consultar a Módulo 3 y consolidar las respuestas en un único resultado.
- **FR-006**: En **Modo Lote**, el sistema DEBE entregar a la pantalla de catálogo/listado únicamente la tarifa base estimada devuelta por Módulo 3 (calculada bajo los supuestos por defecto de Módulo 3 de 1 día de duración y 1 pasajero), excluyendo estrictamente de la previsualización los conceptos de seguro náutico y depósito de garantía.
- **FR-007**: En **Modo Lote**, si una o más embarcaciones carecen de tarifas configuradas en Módulo 3 o no son cotizables, el sistema DEBE procesar y retornar las tarifas de las demás embarcaciones válidas sin abortar la operación masiva, etiquetando las embarcaciones sin precio como "Cotización no disponible".
- **FR-008**: En **Modo Lote**, si la llamada a Módulo 3 falla por desconexión o timeout, el sistema DEBE capturar la excepción y retornar un estado de "Tarifas no disponibles temporalmente", permitiendo que el catálogo permanezca navegable sin interrumpir la plataforma.
- **FR-009**: En **Modo Individual**, el sistema DEBE ser invocado obligatoriamente por el caso de uso `Iniciar reserva` (`<<include>>`), recibiendo el identificador de la embarcación (`boat_id`), fecha/hora de inicio, fecha/hora de fin y número de pasajeros.
- **FR-010**: En **Modo Individual**, el sistema DEBE enviar los parámetros a la API de cotización individual de Módulo 3, delegando en Módulo 3 la validación temporal de las fechas (fechas en el pasado, fin anterior a inicio, duración de cero días o formato inválido) y la liquidación de la fórmula tarifaria integral.
- **FR-011**: En **Modo Individual**, si Módulo 3 rechaza la solicitud debido a fechas inválidas o parámetros no conformes, el sistema DEBE capturar la respuesta estructurada de error de Módulo 3 y trasladarla de inmediato a `Iniciar reserva` para informar al Arrendatario la razón del rechazo, sin persistir ninguna reserva provisional.
- **FR-012**: En **Modo Individual**, el sistema DEBE recibir de Módulo 3 y entregar al caso de uso `Iniciar reserva`:
  - Monto total liquidado.
  - Desglose oficial de conceptos (tarifa base del alquiler y tarifa del seguro náutico por pasajero).
  - Código de moneda oficial (ej. COP, USD).
  - Identificador o referencia única de la cotización emitida por Módulo 3.
  - **Bandera de advertencia obligatoria**, preservando de manera literal e inalterada el texto exacto:
    `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"`.
- **FR-013**: En **Modo Individual**, el sistema DEBE obligar a que `Iniciar reserva` presente la advertencia obligatoria al Arrendatario antes de formalizar el pago, y DEBE registrar los montos monetarios recibidos de Módulo 3 en la reserva creada en estado "Pendiente de Pago".
- **FR-014**: En **Modo Individual**, si el Arrendatario modifica las fechas, horarios, la embarcación o la cantidad de pasajeros antes de que la reserva se persista formalmente, el sistema DEBE descartar la cotización previa y solicitar una nueva cotización individual a Módulo 3 con los datos actualizados.
- **FR-015**: En **Modo Individual**, si la embarcación no posee tarifas activas en Módulo 3, o si el monto total devuelto es menor o igual a cero sin autorización expresa, el sistema DEBE abortar la creación de la reserva y notificar la inconsistencia tarifaria.
- **FR-016**: En **Modo Individual**, si Módulo 3 no responde dentro del tiempo de espera fijado o se produce un fallo de red, el sistema DEBE aplicar un bloqueo de seguridad (*fail-safe*), cancelando el proceso en `Iniciar reserva` e impidiendo que se generen reservas sin precio oficial asociado.
- **FR-017**: **REGLA (Carácter de solo lectura de la cotización):** El sistema DEBE considerar toda consulta de cotización (lote o individual) como una operación de lectura sin impacto de fondos ni de inventario contable en Módulo 3; ningún fallo posterior en Módulo 2 requerirá operaciones de reversión o compensación en Módulo 3.

---

### Key Entities

- **Solicitud de Cotización en Lote (`BatchQuoteRequest`)**: Colección de identificadores de embarcaciones enviada a Módulo 3 (`boat_ids: List[UUID]`).
- **Respuesta de Cotización en Lote (`BatchQuoteResponse`)**: Conjunto consolidado de tarifas base estimadas por embarcación devueltas por Módulo 3. Contiene pares de `boat_id` y monto de tarifa base, código de moneda y lista de identificadores no cotizables.
- **Solicitud de Cotización Individual (`SingleQuoteRequest`)**: Conjunto de parámetros requeridos para la cotización de un viaje específico. Atributos: identificador de la embarcación (`boat_id`), fecha y hora de inicio (`start_time`), fecha y hora de fin (`end_time`), cantidad de pasajeros (`passenger_count`) e identificador del arrendatario (`renter_id`).
- **Respuesta de Cotización Individual (`SingleQuoteResponse`)**: Resultado financiero emitido por Módulo 3 para un viaje. Atributos: identificador de cotización (`quote_id`), monto total (`total_amount`), desglose de conceptos (`base_rental_amount`, `insurance_amount`), moneda (`currency`), bandera de advertencia obligatoria (`warning_banner`: `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"`) y marca temporal de cálculo.
- **Reserva (`Reservation`)**: Entidad de Módulo 2 creada en estado "Pendiente de Pago" por el caso de uso `Iniciar reserva`, la cual adopta los valores monetarios exactos de la cotización individual sin alteraciones.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los precios mostrados en el catálogo de embarcaciones y almacenados en las reservas provienen directamente de Módulo 3, con un cero por ciento (0%) de cálculos matemáticos, deducciones o redondeos realizados por Módulo 2.
- **SC-002**: En el 100% de las cotizaciones individuales entregadas a `Iniciar reserva` y presentadas al Arrendatario, la bandera de advertencia obligatoria `"Valor estimado. No incluye cargos adicionales ni depósito de seguridad"` se expone con exactitud sintáctica y sin modificaciones.
- **SC-003**: En modo lote, el 100% de las listas de embarcaciones que superen el límite máximo de Módulo 3 (50 embarcaciones [NEEDS CLARIFICATION: confirmar si el umbral máximo de lote de Módulo 3 es de 50 o 100 embarcaciones]) son fragmentadas y consolidadas sin pérdida de datos ni fallos en la consulta.
- **SC-004**: En modo lote, la existencia de embarcaciones sin tarifas configuradas en Módulo 3 genera un cero por ciento (0%) de interrupciones o caídas en la visualización de las embarcaciones válidas del catálogo.
- **SC-005**: El 100% de los rechazos de Módulo 3 por fechas inválidas (pasadas, fin menor a inicio, duración 0) impiden de forma controlada la creación de la reserva en Módulo 2, informando el motivo exacto al Arrendatario.
- **SC-006**: Cero por ciento (0%) de reservas creadas en estado "Pendiente de Pago" con montos nulos, negativos o sin cotización válida confirmada previamente por Módulo 3.
- **SC-007**: El 100% de las modificaciones de parámetros del viaje (fechas o cantidad de pasajeros) previas a la confirmación de la reserva invalidan la cotización previa y generan una nueva solicitud a Módulo 3.
- **SC-008**: El tiempo de entrega de la cotización individual desde la selección de datos hasta su presentación en `Iniciar reserva` no excede [NEEDS CLARIFICATION: definir SLA objetivo de latencia de Módulo 3, p. ej. 800 ms], y para la previsualización en lote no excede [NEEDS CLARIFICATION: definir SLA objetivo de latencia de Módulo 3, p. ej. 1500 ms].