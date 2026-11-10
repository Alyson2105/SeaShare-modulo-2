# CU-12 — Pruebas backend de cálculo total

Casos de CalculatedValuePort y cliente HTTP de M3; modo fake activo por defecto. CU-03 aún no consume el puerto.

**Estado global**: Draft por integración y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU12-001](TC-CU12-001-calculo-exitoso.md) | Cálculo vinculante | FR-002, FR-003, FR-004, FR-005 | SC-001, SC-002 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-002](TC-CU12-002-montos-invalidos.md) | Desglose inválido | FR-005, FR-007 | SC-002, SC-003 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-003](TC-CU12-003-reintentos-404.md) | Información aún no disponible | FR-003, FR-004, FR-008 | SC-003, SC-004 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-004](TC-CU12-004-fallo-servicio.md) | Fallo del servicio financiero | FR-007, FR-008 | SC-004 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | In Process |
| [TC-CU12-005](TC-CU12-005-integracion-pago.md) | Asociación al pago | FR-001, FR-006, FR-009 | SC-001, SC-003 | Sin prueba de CU-12 | Blocked |
| [TC-CU12-006](TC-CU12-006-auditoria-calculo.md) | Auditoría de cálculo | FR-010 | SC-001 | Sin prueba de CU-12 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Parcial — ver caso y brechas | 005 | Backend | CU-03 pendiente |
| FR-002 | Parcial — ver caso y brechas | 001 | Backend | Sin aritmética local |
| FR-003 | Parcial — ver caso y brechas | 001, 003 | Backend | Identificador de reserva |
| FR-004 | Parcial — ver caso y brechas | 001, 003 | Backend | POST sin cuerpo y reintento 404 |
| FR-005 | Parcial — ver caso y brechas | 001, 002 | Backend | Desglose; identificador no enviado |
| FR-006 | Parcial — ver caso y brechas | 005 | Backend | Asociación pendiente CU-03 |
| FR-007 | Parcial — ver caso y brechas | 002, 004 | Backend | Errores M3 |
| FR-008 | Parcial — ver caso y brechas | 003, 004 | Backend | Fallo de red |
| FR-009 | Parcial — ver caso y brechas | 005 | Backend | Cobro depende de CU-03 |
| FR-010 | Parcial — ver caso y brechas | 006 | Backend | Auditoría no implementada |

## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Parcial | TC-001; asociación a pagos y auditoría pendientes. |
| SC-002 | Parcial | TC-001 y 002 cubren el desglose del adaptador. |
| SC-003 | Bloqueado | TC-005; CU-03 pendiente. |
| SC-004 | Bloqueado | TC-004; efectos sobre reserva y M1 dependen de CU-03. |
| SC-005 | Draft | SLA pendiente de ratificación y medición externa. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU12-001 | Sí | No | Pendiente | — | — | In Process |
| TC-CU12-002 | Sí | No | Pendiente | — | — | In Process |
| TC-CU12-003 | Sí | No | Pendiente | — | — | In Process |
| TC-CU12-004 | Sí | No | Pendiente | — | — | In Process |
| TC-CU12-005 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU12-006 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-03 aún no usa el puerto y falta base-url de M3.
2. FR-005 exige identificador de cálculo, pero [plan](../plan.md) y entidades indican que M3 no lo envía.
3. Auditoría de solicitud y montos FR-010 no está implementada.
4. El presupuesto de reintentos 404 controla la próxima espera, no garantiza un límite de latencia completo.
5. El SLA de M3 está pendiente; modo fake activo por defecto.
6. La confirmación del evento previo, asociación a la reserva y ausencia de bloqueo M1 requieren integración CU-03.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
