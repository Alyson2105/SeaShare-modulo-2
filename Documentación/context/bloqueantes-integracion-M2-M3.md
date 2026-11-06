# Puntos bloqueantes Módulo 2 ↔ Módulo 3

**Fecha:** 2026-11-05
**Fuentes:** M2 `Alyson2105/SeaShare-modulo-2` @ `3c06c73` (`Documentación/features/`) — M3 `C:\GitLocal\SeaShare_Modulo_3\docs\technical-plan\contracts\`

> Solo las divergencias que impedirían que la integración funcione en producción. Los contratos REST (`UC01`, `UC04`, `UC06`) y de eventos (`UC07`, `UC08`) están alineados. El problema es único y está en el flujo de información.

---

## Bloqueante 1 — Campo `vessel_id` vs `boat_id` (rompe toda la cadena financiera)

- **Qué:** el evento `reservation.info.provided` de M2 (`CU-02/plan.event.md` §4) envía **`vessel_id`**. El contrato de M3 (`events/UC03-informacion-reserva.md` §2) exige **`boat_id`**.
- **Efecto:** el consumidor de M3 valida, falla y hace `ack` + `operational_failure` **sin reintento** (política de colas, README §4.4). Resultado: ninguna reserva se registra → UC04 responde `404 RESERVATION_INFO_NOT_FOUND` para siempre → **cero cobros, cero liquidaciones**. Fallo silencioso.
- **Solución (elegir una):**
  1. **Recomendada — lo corrige M2:** renombrar `vessel_id` → `boat_id` en el payload del outbox de CU-02 (+ una línea en `plan.event.md`). `boat_id` ya es el nombre canónico en nuestros SPEC 003/UC01/UC04.
  2. Alternativa — lo corrige M3: `UC03-informacion-reserva.md` §2 acepta `vessel_id` como alias documentado (menos deseable: dos nombres para un mismo concepto).

---

## Bloqueante 2 — FR-007 del CU-17 de M2 (`ACCEPTED→COMPLETADO`, `REJECTED→APPROVED`)

- **Qué:** el spec `CU-17-actualizar-estado-disputa-garantia/spec.md` FR-007 manda emitir `COMPLETADO` (en español) y `APPROVED` para el `ACCEPTED`/`REJECTED` de la disputa. Ninguno existe en el catálogo de M3 (`UC08-disputa-garantia.md` §2: solo `PENDING`, `REJECTED`, `COMPLETED`).
- **Efecto:** implementado literal, M3 ignora ambos estados → **ningún depósito de garantía se liquida o reembolsa jamás** (queda retenido en todas las reservas completadas).
- **Nota:** el contrato real de M2 (`CU-18/plan.md` §4) ya hace la traducción correcta (`ACCEPTED → COMPLETED`, `REJECTED → REJECTED`), igual que el nuestro. El bug está solo en el texto del spec.
- **Solución (M2):** reescribir FR-007 a: *"Si el estado en M2 es `ACCEPTED`, emite `COMPLETED`; si es `REJECTED`, emite `REJECTED` (mayúsculas, inglés, según el contrato UC08 de M3)."* Y verificar que las pruebas del outbox estén alineadas con CU-18, no con este texto.

---

## Riesgo de idempotencia — `status_changed_at` con offset local

- **Qué:** M2 ejemplifica `2026-10-09T12:05:00-05:00`; M3 convención ISO-8601 en UTC (`Z`). La clave idempotente del cobro es `<reservation_id>:PENDING:<status_changed_at>` (`UC07-estado-reserva.md` §3.7): la misma transición reenviada debe producir exactamente la misma marca.
- **Efecto:** representaciones distintas del mismo instante → claves dobles → cobros duplicados posibles.
- **Solución (una línea por lado):** serializar siempre UTC con sufijo `Z` (ej. `2026-10-09T17:05:00Z`). M2 lo aplica en sus tres outboxes (CU-02, CU-14, CU-18); M3 lo agrega como nota en `UC07` §2.

---

## Resumen de acción

| # | Acción | Dueño | Costo |
|---|---|---|---|
| 1 | Renombrar `vessel_id` → `boat_id` en el evento `reservation.info.provided` | M2 | 1 línea |
| 2 | Corregir FR-007 de CU-17: `ACCEPTED→COMPLETED`, `REJECTED→REJECTED` | M2 | 1 línea |
| 3 | Serializar `status_changed_at` en UTC con `Z` | M2 + nota en M3 | 1 línea por lado |

No iniciar pruebas end-to-end hasta cerrar #1 y #2.
