package com.miempresa.comuniapp.domain.model

/**
 * Estado operativo de un evento a lo largo de su ciclo de vida.
 * - CREATED:  recién creado, pendiente de revisión.
 * - ACTIVE:   aprobado y visible para la comunidad.
 * - FULL:     cupo de asistentes agotado.
 * - FINISHED: el evento ya ocurrió.
 */
enum class EventStatus {
    CREATED,
    ACTIVE,
    FULL,
    FINISHED
}