// domain/service/AiCategorizer.kt
package com.miempresa.comuniapp.domain.service

import com.miempresa.comuniapp.domain.model.Category

/**
 * Contrato único para cualquier proveedor de clasificación por IA.
 *
 * Todas las implementaciones (OpenRouter, DeepSeek, Gemini, local) deben
 * cumplir este contrato. El resto de la app solo conoce esta interfaz,
 * nunca una implementación concreta → cambiar proveedor = cambiar 1 binding en Hilt.
 *
 * @return La [Category] sugerida, o `null` si el proveedor no está seguro.
 * @throws CancellationException si la corrutina padre fue cancelada (SIEMPRE relanzar).
 */
interface AiCategorizer {
    suspend fun categorize(title: String, description: String): Category?
}