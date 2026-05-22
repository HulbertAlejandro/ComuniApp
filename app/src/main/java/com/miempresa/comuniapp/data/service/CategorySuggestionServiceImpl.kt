// data/service/CategorySuggestionServiceImpl.kt
package com.miempresa.comuniapp.data.service

import android.util.Log
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.service.AiCategorizer
import com.miempresa.comuniapp.domain.service.CategorySuggestionService
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Orquestador de dos niveles que implementa [CategorySuggestionService].
 *
 * Nivel 1 → [primaryCategorizer]  : OpenRouter (red, con reintentos)
 * Nivel 2 → [fallbackCategorizer] : Heurística local (sin red, siempre disponible)
 *
 * Flujo:
 * ```
 * primaryCategorizer.categorize()
 *     ├─ éxito           → devuelve Category (puede ser null si no reconoce)
 *     ├─ IOException     → activa fallback silenciosamente
 *     └─ otro Exception  → activa fallback, loguea el error
 * ```
 * La UI nunca ve un crash; en el peor caso ve `null` (banner oculto).
 */
@Singleton
class CategorySuggestionServiceImpl @Inject constructor(
    @Named("primary_categorizer")  private val primaryCategorizer : AiCategorizer,
    @Named("fallback_categorizer") private val fallbackCategorizer: AiCategorizer
) : CategorySuggestionService {

    override suspend fun suggestCategory(title: String, description: String): Category? {
        return try {
            val result = primaryCategorizer.categorize(title, description)
            Log.d(TAG, "Proveedor primario respondió: ${result?.name ?: "null"}")
            result ?: run {
                // El modelo no reconoció categoría → intentamos con heurística como
                // segundo intento de cobertura (no es un error, es refinamiento)
                Log.d(TAG, "Primario devolvió null, consultando heurística como refinamiento")
                fallbackCategorizer.categorize(title, description)
            }
        } catch (e: CancellationException) {
            throw e  // Regla de corrutinas: NUNCA atrapar esto
        } catch (e: IOException) {
            Log.w(TAG, "Sin red. Activando fallback heurístico. (${e.message})")
            fallbackCategorizer.categorize(title, description)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en proveedor primario. Activando fallback.", e)
            fallbackCategorizer.categorize(title, description)
        }
    }

    private companion object {
        const val TAG = "SuggestionOrchestrator"
    }
}