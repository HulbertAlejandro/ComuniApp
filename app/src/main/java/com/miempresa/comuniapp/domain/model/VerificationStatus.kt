package com.miempresa.comuniapp.domain.model

enum class VerificationStatus {
    PENDING,     // Sin revisar
    APPROVED,    // Verificado
    REJECTED     // Rechazado (requiere motivo)
}