# Contexto de Progreso de Implementación - Módulo 2 (SEA-SHARE)

**Fecha**: 2026-11-03  
**Módulo**: Módulo 2 - Operación de Reservas, Tiempos y Cancelaciones  
**Stack**: Java 21, Spring Boot 4.1.2, Maven, PostgreSQL, Flyway  

---

## 1. Parte A: Recuperación y Verificación de Tests de Idempotencia

### Objetivo
Asegurar que la migración previa de `IdempotencyAdapter` hacia Spring Data JPA mantenga intactos todos los escenarios de prueba funcionales y de concurrencia requeridos para el control de idempotencia mediante `IdempotencyPort`.

### Estado de los Casos de Test
Se revisaron los tests existentes en `src/test/java/com/seashare/m2/reservas/integration/`:
- **Caso a) Clave nueva y duplicada dentro de la ventana**:
  - `newKeyIsClaimedAndSameKeyWithinWindowIsRejected()` en `IdempotencyAdapterTest.java`: `tryClaim` retorna `true` para una clave nueva y `false` para la misma clave dentro de la ventana temporal. *(Ya estaba implementado)*
- **Caso b) Clave vencida**:
  - `expiredKeyCanBeClaimedAgain()` en `IdempotencyAdapterTest.java`: `tryClaim` retorna `true` de nuevo con un `now` posterior a la ventana de expiración. *(Ya estaba implementado)*
- **Caso c) Completar clave no reclamada**:
  - `completingAnUnclaimedKeyFails()` en `IdempotencyAdapterTest.java`: invocar `complete` sobre una clave inexistente arroja `IllegalStateException("No idempotency key claimed for endpoint")`. *(Ya estaba implementado)*
- **Caso d) Concurrencia entre hilos sin `@Transactional` de test**:
  - `onlyOneConcurrentClaimSucceeds()` en `IdempotencyClaimConcurrencyTest.java`: dos hilos compiten simultáneamente con `CountDownLatch` y commits reales mediante `TransactionTemplate` (usando clave y endpoint únicos por UUID); exactamente uno obtiene `true` y el otro `false`. *(Ya estaba implementado)*

---

## 2. Parte B: Scheduler de Expiración por TTL

### Objetivo
Automatizar el paso de reservas en estado `INITIATED` cuyo tiempo de vida (`ttl_expires_at`) ya venció hacia el estado `EXPIRED`, invocando el caso de uso central `ChangeReservationStatusUseCase` (CU-08) como único escritor del estado.

### Componentes Implementados

1. **Puerto de Entrada (`port/in`)**:
   - `ExpireReservationsUseCase.java`: define el método `int execute()`, que orquesta la expiración en lote y retorna el conteo de reservas procesadas.
2. **Servicio de Aplicación (`application/service`)**:
   - `ExpireReservationsService.java`:
     - Consulta `ClockPort` para obtener la hora actual del sistema (sin usar `Instant.now()` directamente).
     - Obtiene los IDs vencidos a través del puerto de lectura con límite de lote (`batchSize`).
     - Itera cada reserva invocando `ChangeReservationStatusUseCase.execute(...)` con actor `SYSTEM` y motivo descriptivo de expiración por TTL.
     - Aislamiento transaccional: cada reserva se procesa de forma independiente bajo la transacción gestionada por `ChangeReservationStatusService`.
     - Manejo de excepciones: omite sin error `VersionConflictException` e `InvalidTransitionException` (condiciones de carrera normales, p. ej. si se pagó a tiempo); captura cualquier otra excepción con log de error y continúa con la siguiente reserva sin interrumpir el ciclo del scheduler.
3. **Método de Lectura Agregado al Puerto de Salida (`port/out`)**:
   - En `ReservationRepositoryPort.java`:
     ```java
     List<UUID> findInitiatedExpiredBefore(Instant now, int batchSize);
     ```
     Permite consultar los identificadores de reservas en `INITIATED` cuyo `ttl_expires_at <= now`, ordenadas por vencimiento y limitadas por lote.
4. **Persistencia e Infraestructura de Salida (`adapter/out/persistence`)**:
   - En `ReservationJpaRepository.java`: consulta JPQL optimizada que aprovecha el índice compuesto existente `ix_reservation_ttl` (`status`, `ttl_expires_at`):
     ```java
     @Query("select r.id from ReservationEntity r where r.status = 'INITIATED' and r.ttlExpiresAt <= :now order by r.ttlExpiresAt asc")
     List<UUID> findIdsByStatusInitiatedAndTtlExpiredBefore(@Param("now") Instant now, Pageable pageable);
     ```
   - En `ReservationRepositoryAdapter.java`: implementación de `findInitiatedExpiredBefore` utilizando `PageRequest.of(0, batchSize)` para acotar la consulta en lectura transaccional de solo lectura (`@Transactional(readOnly = true)`). Se preserva la arquitectura de un solo adaptador de persistencia para el agregado de reservas.
5. **Adaptador de Entrada Scheduler (`adapter/in/scheduler`)**:
   - `TtlExpiryJob.java`: componente `@Component` que expone el método programado `@Scheduled` que delega íntegramente en `ExpireReservationsUseCase`. Sin lógica de negocio. Incluye comentario `// TODO (D-15)` para ShedLock.
   - `SchedulerConfig.java`: configuración con `@EnableScheduling` condicionado por la propiedad `m2.scheduler.ttl.enabled` (habilitado por defecto; desactivable en suites de test para evitar ejecuciones asíncronas no deseadas).
6. **Propiedades de Configuración (`src/main/resources/application.yaml`)**:
   ```yaml
   m2:
     scheduler:
       ttl:
         enabled: true                          # Activa el job de expiración por TTL (false en tests que no lo requieran)
         fixed-delay-ms: 30000                  # Intervalo entre ejecuciones del job en milisegundos (30 s)
         batch-size: 100                        # Máximo de reservas procesadas por ciclo (100)
   ```

---

## 3. Decisiones de Diseño y Arquitectura

1. **Un solo adaptador por puerto**: Se mantuvo `ReservationRepositoryAdapter` como la única implementación de `ReservationRepositoryPort`, sin crear adaptadores paralelos ni duplicar la infraestructura de persistencia.
2. **Uso estricto de ClockPort**: Tanto en la creación como en la expiración se utiliza exclusivamente `ClockPort.now()`. Nunca se utiliza `Instant.now()` de forma directa en los servicios de aplicación.
3. **Aislamiento transaccional por reserva**: La orquestación en lote delega cada transición a `ChangeReservationStatusUseCase`, cuya implementación (`ChangeReservationStatusService`) ya es `@Transactional`. De esta manera, cada reserva corre en su propia transacción física; si una reserva falla o presenta conflicto de concurrencia optimista, su transacción local revierte sin afectar el commit de las demás reservas del lote.
4. **Sin ShedLock en esta fase**: Al contar con una sola instancia del servicio, no se introduce la dependencia de ShedLock todavía; se deja la anotación explícita `// TODO (D-15)` en el job para cuando se configure despliegue multi-réplica.
5. **Auditoría y Outbox**:
   - Cada transición a `EXPIRED` asienta su registro en la tabla de auditoría inmutable (`status_audit`) vía `StatusAuditPort`.
   - Según la especificación oficial de `CU-08` (tabla de transiciones, flujo alterno de `Expirada`), no se emite notificación saliente a Módulo 1 ni a Módulo 3, por lo que `OUTBOX_STATUSES` no incluye `EXPIRED`.
6. **Cumplimiento de Reglas ArchUnit**: El nuevo adaptador de entrada en `adapter/in/scheduler` solo depende de `port/in` (`ExpireReservationsUseCase`), sin conocer adaptadores de salida ni entidades de persistencia.

---

## 4. Archivos Modificados y Creados

### Archivos Creados
- `src/main/java/com/seashare/m2/reservas/application/port/in/ExpireReservationsUseCase.java`
- `src/main/java/com/seashare/m2/reservas/application/service/ExpireReservationsService.java`
- `src/main/java/com/seashare/m2/reservas/infrastructure/adapter/in/scheduler/TtlExpiryJob.java`
- `src/main/java/com/seashare/m2/reservas/infrastructure/adapter/in/scheduler/SchedulerConfig.java`
- `src/test/java/com/seashare/m2/reservas/application/service/ExpireReservationsServiceTest.java`
- `src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/TtlExpiryJobTest.java`
- `src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java`

### Archivos Modificados
- `src/main/java/com/seashare/m2/reservas/application/port/out/ReservationRepositoryPort.java`: agregado método `findInitiatedExpiredBefore`.
- `src/main/java/com/seashare/m2/reservas/infrastructure/adapter/out/persistence/reservation/ReservationJpaRepository.java`: agregada query JPQL con índice `ix_reservation_ttl`.
- `src/main/java/com/seashare/m2/reservas/infrastructure/adapter/out/persistence/reservation/ReservationRepositoryAdapter.java`: implementación del método de lectura paginada.
- `src/main/resources/application.yaml`: bloque de configuración `m2.scheduler.ttl`.
- `Documentación/context/implementContext.md`: bitácora de progreso y arquitectura.

---

## 5. Pruebas y Verificación

### Tests Añadidos
1. **`ExpireReservationsServiceTest` (Unitarios Mockito)**:
   - `expiraSoloLasVencidas()`: expira solo reservas devueltas por el puerto con parámetros SYSTEM.
   - `noProcesaNadaSiNoHayVencidas()`: salida limpia si no hay IDs vencidos.
   - `bordeExactoVenceEnNowExpira()`: límite exacto en `now`.
   - `bordeUnSegundoAntesNoExpira()`: simula reserva que no ha vencido.
   - `conflictoDeVersionSeOmiteSinError()`: captura `VersionConflictException` sin propagar.
   - `transicionInvalidaSeOmiteSinError()`: captura `InvalidTransitionException` sin propagar.
   - `excepcionInesperadaEnUnaReservaNoImpideProcesarLasDemas()`: una excepción general en el medio del lote no detiene las restantes.
   - `respetaLimiteDeLote()`: valida paso de `batchSize`.
   - `nuncaLlamaInstantNowDirectamente()`: verifica uso exclusivo de `ClockPort`.
2. **`TtlExpiryJobTest` (Unitario)**:
   - `runDelegaEnElCasoDeUso()`: verifica que el job programado delega sin lógica al caso de uso.
3. **`TtlExpiryIntegrationTest` (Integración con base de datos real y puertos públicos)**:
   - `expiraReservasConTtlVencido()`: siembra reserva con `CreateReservationUseCase`, avanza el reloj con mock de `ClockPort`, ejecuta expiración y verifica estado `EXPIRED` con `ReservationRepositoryPort`.
   - `noExpiraReservasCuyoTtlNoHaVencido()`: reloj antes del TTL mantiene estado `INITIATED`.
   - `expiracionGeneraAuditoria()`: verifica que el cambio de estado asienta auditoría con `from: INITIATED` a `to: EXPIRED`.
   - `noExpiraReservasSiRelojEsAnteriorACreacion()`: valida protección temporal.

### Ejecución de Pruebas
Para ejecutar la suite completa:
```bash
./mvnw test
```
Para ejecutar específicamente las pruebas de expiración TTL:
```bash
./mvnw test -Dtest=ExpireReservationsServiceTest,TtlExpiryJobTest,TtlExpiryIntegrationTest
```
**Resultado final**: 75 pruebas ejecutadas, 0 fallos, 0 errores (BUILD SUCCESS).

---

## 6. Estado de Casos de Uso y Tareas

- **CU-08 (Actualizar Estado de Reserva)**:
  - Disparador real de expiración por TTL completado e integrado mediante `TtlExpiryJob` y `ExpireReservationsUseCase`.
  - Transición automática `INITIATED -> EXPIRED` operativa con persistencia y auditoría.
- **CU-02 (Iniciar Reserva)**:
  - Integrado con TTL de 15 minutos que ahora es consumido y liberado activamente por el scheduler.

---

## 7. Pendiente / Deuda Técnica

1. **ShedLock (D-15)**: Pendiente de incorporar cuando se prepare el despliegue con múltiples instancias/réplicas para evitar ejecuciones duplicadas simultáneas del job.
2. **Expiración de reservas en `PENDING_PAYMENT`**: De acuerdo con las reglas de negocio de la tarea, solo se incluyeron reservas en `INITIATED`. Si en el futuro se requiere expirar pasivamente reservas en `PENDING_PAYMENT` que superen su ventana de pago, se puede extender la consulta JPQL o crear una regla específica.
