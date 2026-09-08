# Feature Specification: Marcar Inicio de la Navegación

**Módulo**: Módulo 2 – Operación de Reservas, Tiempos y Cancelaciones  
**Fecha de Creación**: 2026-09-06  
**Actores Primarios**: Propietario (Anfitrión de la embarcación)  
**Dependencias Externas (APIs)**:
- **Módulo 1 – Gestión de Flota y Activos P2P**: API externa `Asignar estado operativo` (cambio del estado del barco a `En Navegación`, orquestado indirectamente a través del caso de uso subordinado `Actualizar estado reserva`).
- **Módulo 3 – Liquidación, Seguros y Dispersión de Fondos**: API externa `Recibir estado de reserva` (notificación de inicio del viaje para la activación formal de la póliza de seguro y control de fondos, orquestada indirectamente a través de `Actualizar estado reserva`).
- **Casos de uso internos de Módulo 2**: `Actualizar estado reserva` `(<<include>>)`.

---

## User Scenarios & Testing

### User Story 1 - El Propietario confirma la entrega del barco e inicia el viaje (Priority: P1)

En el muelle de salida, una vez revisado el estado del barco, el equipo de seguridad y la llegada del cliente, el Propietario presiona la opción para registrar la salida ('Marcar inicio de la navegación'). En ese instante, el sistema comprueba que la reserva esté en estado 'Confirmada', guarda la hora real de salida y cambia la reserva a 'En Navegación' invocando el caso de uso 'Actualizar estado reserva' `(<<include>>)`. Esta acción le avisa al Módulo 1 para que ponga el barco como 'En Navegación' y le notifica al Módulo 3 para activar el seguro del viaje.

***Why this priority***: Es el paso clave que formaliza la entrega del barco al cliente. Sin este registro, la plataforma no puede saber si el viaje realmente comenzó, si el seguro está activo o si el barco está en el agua.

***Independent Test***: Se prueba con una reserva en estado "Confirmada", realizando el inicio de viaje por parte del Propietario dentro del horario de salida. Se verifica que la reserva pasa a "En Navegación", se guarda el registro de entrega y se notifican los Módulos 1 y 3 sin realizar cálculos de dinero.

***Acceptance Scenarios***:

1. **Scenario**: Registro de salida exitoso en la fecha y hora acordadas
    - **Given** una reserva en estado principal "Confirmada" cuya fecha y hora de salida corresponde al momento actual
    - **When** el Propietario registrado solicita "Marcar inicio de la navegación"
    - **Then** el sistema verifica los permisos y el estado, invoca `(<<include>>)` "Actualizar estado reserva" para cambiar el estado a "En Navegación", guarda la hora exacta de salida y solicita poner el barco como "En Navegación" en Módulo 1 y notificar a Módulo 3

2. **Scenario**: Registro de salida exitoso tras llegada tardía del cliente (renuncia al No-Show)
    - **Given** una reserva en estado principal "Confirmada" cuya hora de salida fue hace más de 30 minutos sin que el Propietario haya reportado inasistencia
    - **When** el cliente llega tarde y el Propietario decide iniciar el viaje presionando "Marcar inicio de la navegación"
    - **Then** el sistema permite el inicio del viaje a "En Navegación", inhabilita definitivamente la opción de reportar inasistencia y guarda la hora real en que comenzó el viaje

---

### User Story 2 - Rechazar el inicio de viaje fuera de la fecha u horario permitido (Priority: P2)

Si el Propietario intenta marcar el inicio de la navegación mucho antes de la fecha y hora acordadas (por ejemplo, días u horas antes de la reserva), el sistema rechaza la solicitud e informa que el registro solo se habilita cerca de la fecha y hora programadas para el viaje.

***Why this priority***: Evita que un anfitrión bloquee el barco en el sistema días antes del viaje real o active coberturas de seguro a destiempo.

***Independent Test***: Se prueba intentando registrar el inicio de viaje en una reserva confirmada cuya fecha es lejana (por ejemplo, dentro de 2 días), comprobando que el sistema rechaza la solicitud e indica cuándo se habilitará la opción.

***Acceptance Scenarios***:

1. **Scenario**: Intento de inicio de viaje demasiado adelantado
    - **Given** una reserva en estado principal "Confirmada" programada para dentro de dos días
    - **When** el Propietario intenta marcar el inicio de la navegación
    - **Then** el sistema rechaza la solicitud e informa que la opción solo se habilita en la fecha del viaje dentro de la ventana de preparación previa

---

### User Story 3 - Rechazar el inicio de navegación en reservas no válidas o por usuarios no autorizados (Priority: P1)

Si se intenta registrar el inicio de la navegación sobre una reserva que ya está "En Navegación", "Cancelada", "Expirada", "Completada" o aún en "Pendiente de Pago", o si la solicitud la realiza una persona diferente al Propietario del barco, el sistema rechaza la acción de inmediato.

***Why this priority***: Mantiene la información del sistema correcta y evita que personas no autorizadas modifiquen el estado de los barcos o dupliquen salidas.

***Independent Test***: Se prueba intentando iniciar el viaje con usuarios no autorizados o sobre reservas en estados no permitidos, verificando que el sistema rechaza la acción sin modificar la reserva ni notificar a otros módulos.

***Acceptance Scenarios***:

1. **Scenario**: Intento de inicio en una reserva no confirmada o cancelada
    - **Given** una reserva en estado "Cancelado", "Pendiente de Pago" o "Expirado"
    - **When** el Propietario intenta marcar el inicio de la navegación
    - **Then** el sistema rechaza la acción e informa que el estado actual no permite iniciar el servicio

2. **Scenario**: Intento de inicio en un viaje que ya comenzó (duplicado)
    - **Given** una reserva que ya se encuentra en estado "En Navegación"
    - **When** se intenta presionar nuevamente el botón de inicio
    - **Then** el sistema detecta que el viaje ya está en curso y rechaza la solicitud sin duplicar registros

3. **Scenario**: Intento de registro por un usuario no autorizado
    - **Given** una reserva en estado "Confirmada"
    - **When** un usuario que no es el propietario registrado del barco intenta marcar el inicio de la navegación
    - **Then** el sistema rechaza la solicitud por falta de permisos

---

### Edge Cases

- **Imposibilidad de marcar inasistencia una vez iniciado el viaje**:
    - Al confirmar la salida y pasar a "En Navegación", la opción de "Marcar inasistencia" queda inhabilitada de forma definitiva para esa reserva.
    - Si el Propietario ya había marcado inasistencia previamente (reserva en "Cancelado" por inasistencia), la solicitud de iniciar el viaje se rechaza de inmediato.
- **Condición de carrera entre el Inicio de Navegación y el reporte de No-Show**:
    - Tras vencer los 30 minutos de tolerancia en el muelle, se habilita la opción de "Marcar inasistencia". Si en el mismo milisegundo el Propietario intenta marcar el inicio del viaje desde un dispositivo y simultáneamente se procesa un reporte de inasistencia, el control de concurrencia atómico de la base de datos asegura que solo una transacción consolide el estado final. Si el No-Show se procesa primero, la reserva pasa a `Cancelado` (Por Inasistencia) y el intento de iniciar navegación es rechazado de inmediato.
- **Múltiples intentos por mala conexión en el muelle**:
    - Si por fallas de señal en el puerto el Propietario presiona varias veces el botón de inicio, el sistema procesa una sola solicitud y responde correctamente a los reintentos sin duplicar notificaciones a Módulo 1 y Módulo 3.
- **Salida ligeramente antes o después de la hora exacta**:
    - Si el viaje sale unos minutos antes o después de la hora acordada por temas de marea o desembarque, el sistema guarda la hora real de salida, manteniendo la hora contratada original para fines del contrato.
- **Prohibición de cobrar dinero o garantías en Módulo 2**:
    - Al entregar el barco, el Módulo 2 **NO realiza cobros adicionales, no cobra depósitos de garantía ni calcula costos de combustible o tiempo extra**. Cualquier gestión de dinero le corresponde de forma exclusiva al Módulo 3.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar el inicio de la navegación (check-in de salida) de una reserva si y solo si la reserva existe y se encuentra en estado principal "Confirmada".
- **FR-002**: El sistema DEBE validar de forma estricta que el usuario que ejecuta la acción sea el Propietario registrado de la embarcación.
- **FR-003**: El sistema DEBE validar que la solicitud se realice dentro de la ventana de tiempo autorizada para la salida (en la fecha programada o dentro del margen previo permitido).
- **FR-004**: Si la reserva se encuentra en cualquier estado diferente a "Confirmada" (incluyendo `Pendiente de Pago`, `En Navegación`, `Completado`, `Expirado` o `Cancelado`), el sistema DEBE rechazar la solicitud e informar el motivo.
- **FR-005**: Al validar la entrega del barco, el sistema DEBE invocar el caso de uso subordinado "Actualizar estado reserva" `(<<include>>)`, solicitando cambiar al estado principal `En Navegación`.
- **FR-006**: El sistema DEBE guardar un registro del inicio del viaje, incluyendo: identificador de la reserva, identificador del propietario, hora real de entrega y notas opcionales.
- **FR-007**: El sistema DEBE indicar a "Actualizar estado reserva" que llame a la API de Módulo 1 (`Asignar estado operativo`) para cambiar la embarcación al estado `En Navegación`.
- **FR-008**: El sistema DEBE indicar a "Actualizar estado reserva" que llame a la API de Módulo 3 (`Recibir estado de reserva`) para notificar el cambio a `En Navegación` y activar el seguro náutico.
- **FR-009**: Al cambiar el estado a "En Navegación", el sistema DEBE desactivar de forma permanente la opción de ejecutar "Marcar inasistencia" o "Solicitar cancelación" para esa reserva.
- **FR-010**: **REGLA DE NEGOCIO ESTRICTA (Sin dinero):** El sistema **NO DEBE realizar cobros de garantías, retenciones de dinero, cálculos de tarifas ni pagos**. El Módulo 2 solo maneja los estados y tiempos del viaje; la gestión monetaria es responsabilidad del Módulo 3.

---

### Key Entities

- **Reserva (`Reservation`)**: Entidad de Módulo 2 que cambia de estado principal "Confirmada" a "En Navegación".
- **Registro de Check-in (`CheckInEvent`)**: Registro que certifica la entrega del barco. Incluye: identificador del evento, identificador de la reserva, identificador del propietario, fecha/hora real de entrega y notas de salida.
- **Embarcación**: Activo registrado en el Módulo 1 cuyo estado cambia a `En Navegación`.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero (0%) inicios de navegación permitidos en reservas que no estén en estado "Confirmada".
- **SC-002**: El 100% de los registros válidos cambian la reserva a `En Navegación` y actualizan la embarcación en Módulo 1 (`Asignar estado operativo`) en menos de 1 segundo.
- **SC-003**: Cero (0%) posibilidad de marcar inasistencia o cancelación una vez confirmado el cambio a "En Navegación".
- **SC-004**: El 100% de los inicios de viaje notifican a Módulo 3 para la activación del seguro náutico.
- **SC-005**: Cero (0) cobros de dinero, cobros de garantía o cálculo de tarifas realizados en Módulo 2 durante el registro de salida.
- **SC-006**: Cero (0%) registros de salida autorizados a personas diferentes al propietario del barco.