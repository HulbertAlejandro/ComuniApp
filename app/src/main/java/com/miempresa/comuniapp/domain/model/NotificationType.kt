package com.miempresa.comuniapp.domain.model

/**
 * Tipos de notificaciones soportadas por ComuniApp.
 *
 * Estas notificaciones pueden originarse desde:
 *
 * - Firebase Cloud Functions
 * - Firestore Triggers
 * - Acciones locales de Android
 * - Eventos del sistema
 *
 * IMPORTANTE:
 * Cada tipo debe coincidir EXACTAMENTE
 * con el valor enviado desde Cloud Functions:
 *
 * data: {
 *    type: "EVENT_APPROVED"
 * }
 */
enum class NotificationType {

    /**
     * Notificación genérica.
     */
    GENERAL,

    // ─────────────────────────────────────────────────────────────
    // EVENTOS
    // ─────────────────────────────────────────────────────────────

    /**
     * El evento fue creado/publicado.
     */
    EVENT_CREATED,

    /**
     * Evento aprobado por moderación.
     */
    EVENT_APPROVED,

    /**
     * Evento rechazado.
     */
    EVENT_REJECTED,

    /**
     * Recordatorio de evento próximo.
     */
    EVENT_REMINDER,

    /**
     * Evento cancelado.
     */
    EVENT_CANCELLED,

    /**
     * Evento actualizado/editado.
     */
    EVENT_UPDATED,

    /**
     * Evento comienza pronto.
     */
    EVENT_STARTING_SOON,

    /**
     * Evento destacado por administración.
     */
    EVENT_FEATURED,

    /**
     * Evento enviado a revisión.
     */
    EVENT_UNDER_REVIEW,

    /**
     * Evento alcanzó capacidad máxima.
     */
    EVENT_FULL,

    /**
     * Hay cupos disponibles nuevamente.
     */
    WAITLIST_AVAILABLE,

    // ─────────────────────────────────────────────────────────────
    // PARTICIPACIÓN
    // ─────────────────────────────────────────────────────────────

    /**
     * Nuevo participante en evento.
     */
    NEW_PARTICIPANT,

    /**
     * Participante abandonó evento.
     */
    PARTICIPANT_LEFT,

    // ─────────────────────────────────────────────────────────────
    // COMUNIDAD
    // ─────────────────────────────────────────────────────────────

    /**
     * Nuevo comentario en evento.
     */
    NEW_COMMENT,

    /**
     * Respuesta a comentario.
     */
    COMMENT_REPLY,

    /**
     * Nueva reacción/like.
     */
    NEW_REACTION,

    /**
     * Evento compartido.
     */
    EVENT_SHARED,

    // ─────────────────────────────────────────────────────────────
    // GAMIFICACIÓN
    // ─────────────────────────────────────────────────────────────

    /**
     * Insignia desbloqueada.
     */
    BADGE_UNLOCKED,

    /**
     * Usuario subió de nivel.
     */
    LEVEL_UP,

    /**
     * Usuario ganó puntos.
     */
    POINTS_EARNED,

    /**
     * Recompensa por racha.
     */
    STREAK_REWARD,

    // ─────────────────────────────────────────────────────────────
    // SEGURIDAD
    // ─────────────────────────────────────────────────────────────

    /**
     * Inicio de sesión detectado.
     */
    LOGIN_DETECTED,

    /**
     * Contraseña modificada.
     */
    PASSWORD_CHANGED,

    /**
     * Cuenta actualizada.
     */
    ACCOUNT_UPDATED,

    // ─────────────────────────────────────────────────────────────
    // ADMINISTRACIÓN
    // ─────────────────────────────────────────────────────────────

    /**
     * Reporte recibido.
     */
    REPORT_RECEIVED,

    NEW_INTEREST
}