# CU-15 — Pruebas backend de consulta interna para M3

Casos para el GET interno propuesto a M3. CU-15 no está implementado; las vistas de usuario de CU-21 son contratos distintos.

**Estado global**: Draft por implementación y contratos pendientes.

## Trazabilidad

| TC | Escenario | FR | SC | Test automatizado parcial | Status |
|---|---|---|---|---|---|
| [TC-CU15-001](TC-CU15-001-consulta-reserva-activa.md) | Consulta de reserva activa | FR-001, FR-002, FR-007 | SC-001, SC-002 | Sin prueba de CU-15 | Blocked |
| [TC-CU15-002](TC-CU15-002-consulta-ttl.md) | Consulta de reserva pendiente | FR-002, FR-005, FR-007 | SC-002 | Sin prueba de CU-15 | Blocked |
| [TC-CU15-003](TC-CU15-003-consulta-cierre.md) | Consulta de reserva completada | FR-003, FR-006 | SC-003 | Sin prueba de CU-15 | Blocked |
| [TC-CU15-004](TC-CU15-004-consulta-cancelacion.md) | Consulta de reserva cancelada | FR-004, FR-006 | SC-003 | Sin prueba de CU-15 | Blocked |
| [TC-CU15-005](TC-CU15-005-acceso-y-reserva-inexistente.md) | Autorización de consulta | FR-001, FR-007 | SC-002 | Sin prueba de CU-15 | Blocked |

## Cobertura de requisitos

| FR | Cobertura | TC | Alcance | Motivo / Observación |
|---|---|---|---|---|
| FR-001 | Bloqueado — CU-15 pendiente | 001, 005 | Backend | Endpoint interno |
| FR-002 | Bloqueado — CU-15 pendiente | 001, 002 | Backend | Datos operativos |
| FR-003 | Bloqueado — CU-15 pendiente | 003 | Backend | Datos de cierre |
| FR-004 | Bloqueado — CU-15 pendiente | 004 | Backend | Datos de cancelación |
| FR-005 | Bloqueado — CU-15 pendiente | 002 | Backend | Vencimiento TTL |
| FR-006 | Bloqueado — CU-15 pendiente | 003, 004 | Backend | Sin cálculos financieros |
| FR-007 | Bloqueado — CU-15 pendiente | 001, 002, 005 | Backend | Solo lectura |


## Criterios de éxito

| SC | Estado | Observación |
|---|---|---|
| SC-001 | Bloqueado | TC-001; latencia menor a 200 ms requiere medición. |
| SC-002 | Bloqueado | TC-001, 002 y 005; ausencia de escrituras. |
| SC-003 | Bloqueado | TC-003 y 004; sin valoración financiera. |

## Checklist de ejecución

| TC | Documentado | Ejecutado | Resultado | Ejecutado por | Fecha | Observaciones |
|---|---|---|---|---|---|---|
| TC-CU15-001 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU15-002 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU15-003 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU15-004 | Sí | No | Pendiente | — | — | Blocked |
| TC-CU15-005 | Sí | No | Pendiente | — | — | Blocked |

## Brechas detectadas

1. [Contexto](../../../context/implementContext.md): CU-15 no tiene endpoint interno; JWT de servicio pendiente.
2. Check-out y clasificación de cancelación dependen de CU-07 y CU-04 no implementados.
3. El spec pide respuestas idénticas repetidas, pero el TTL restante cambia con el reloj aun sin escrituras.
4. Ejemplos del plan incluyen identificadores con letras fuera de UUID hexadecimal; usar UUID válidos en ejecución.
5. El plan declara latencia garantizada sin medición; se necesita prueba de rendimiento.

## Convenciones de estado

- **Draft**: contrato incompleto o inconsistente.
- **In Process**: ejecutable con implementación actual.
- **Approved**: aprobado tras revisión y ejecución; ninguno aquí.
- **Blocked**: depende de funcionalidad pendiente.
