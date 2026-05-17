package com.miempresa.comuniapp.domain.model

/**
 * Estado del proceso de moderación de un evento.
 * - PENDING:  sin revisar por un moderador.
 * - APPROVED: verificado y público.
 * - REJECTED: rechazado; debe incluir [Event.rejectionReason].
 */
enum class VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}