package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import java.util.UUID;

/**
 * Identidad temporal del llamador del contexto disputa.
 *
 * <p>Se resuelve de los headers {@code X-User-Id} / {@code X-User-Role} (D-13).
 * Se mantiene propia del contexto para no importar la identidad de
 * {@code reservas} (D-03). TODO: unificar con la identidad de reservas cuando
 * exista autenticacion real.</p>
 */
public record DisputeCallerIdentity(
        UUID userId, // identificador del usuario que realiza la llamada
        String role // rol declarado por el llamador
) {
}
