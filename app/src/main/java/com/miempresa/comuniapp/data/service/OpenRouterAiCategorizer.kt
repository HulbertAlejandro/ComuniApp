package com.miempresa.comuniapp.data.service

import android.util.Log
import com.miempresa.comuniapp.data.remote.api.OpenRouterApiService
import com.miempresa.comuniapp.data.remote.dto.OpenRouterMessage
import com.miempresa.comuniapp.data.remote.dto.OpenRouterRequest
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.service.AiCategorizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Implementación de [AiCategorizer] que consume la API de OpenRouter.
 *
 * Estrategia de resiliencia:
 * 1. Hasta [MAX_RETRIES] reintentos con Exponential Backoff en caso de 429 / 5xx.
 * 2. En cada reintento el delay se duplica: 1s → 2s → 4s (máx [MAX_DELAY_MS]).
 * 3. Errores de red (sin internet) lanzan [IOException] → el orquestador activa el fallback.
 * 4. [CancellationException] siempre se relanza (regla de corrutinas).
 */
@Singleton
class OpenRouterAiCategorizer @Inject constructor(
    private val apiService: OpenRouterApiService,
    @Named("openrouter_api_key") private val apiKey: String  // se usa solo para logging; el header lo pone OkHttp
) : AiCategorizer {

    override suspend fun categorize(title: String, description: String): Category? {
        val request = buildRequest(title, description)

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = apiService.chatCompletion(request)

                when {
                    response.isSuccessful -> {
                        val raw = response.body()
                            ?.choices?.firstOrNull()
                            ?.message?.content
                            ?.trim()
                            .orEmpty()
                        Log.d(TAG, "Respuesta de OpenRouter: '$raw'")
                        return mapToCategory(raw)  // null si no reconoce
                    }

                    response.code() == 429 -> {
                        // Too Many Requests → Exponential Backoff
                        val delayMs = computeBackoff(attempt)
                        Log.w(TAG, "429 recibido. Reintento ${attempt + 1}/$MAX_RETRIES en ${delayMs}ms")
                        delay(delayMs)
                        // Continúa el loop (siguiente iteración del repeat)
                    }

                    response.code() in 500..599 -> {
                        // Error del servidor → también con backoff
                        val delayMs = computeBackoff(attempt)
                        Log.w(TAG, "HTTP ${response.code()} del servidor. Reintento ${attempt + 1}/$MAX_RETRIES en ${delayMs}ms")
                        delay(delayMs)
                    }

                    else -> {
                        // 4xx que no sea 429 (ej. 401 Unauthorized, 400 Bad Request)
                        // Son errores de configuración, no tiene sentido reintentar
                        Log.e(TAG, "Error no recuperable HTTP ${response.code()}: ${response.errorBody()?.string()}")
                        return null
                    }
                }

            } catch (e: CancellationException) {
                throw e  // Regla crítica: SIEMPRE relanzar
            } catch (e: IOException) {
                // Sin internet u otro error de red → lanza para que el orquestador active fallback
                Log.w(TAG, "Error de red en intento ${attempt + 1}: ${e.message}")
                if (attempt == MAX_RETRIES - 1) throw e  // Agotados los reintentos
                delay(computeBackoff(attempt))
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en intento ${attempt + 1}", e)
                if (attempt == MAX_RETRIES - 1) throw e
                delay(computeBackoff(attempt))
            }
        }

        // Si llegamos aquí, agotamos reintentos sin éxito definitivo
        Log.e(TAG, "Agotados $MAX_RETRIES reintentos. Devolviendo null.")
        return null
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private fun buildRequest(title: String, description: String): OpenRouterRequest {
        return OpenRouterRequest(
            // :free = tier gratuito de OpenRouter (20 req/min, ~200/día)
            // Puedes cambiar este string por "deepseek/deepseek-chat:free" o
            // "meta-llama/llama-3.3-70b:free" sin tocar nada más
            model       = MODEL,
            maxTokens   = 20,
            temperature = 0f,
            messages    = listOf(
                OpenRouterMessage(
                    role    = "system",
                    content = """
                        Eres un clasificador estricto. Responde ÚNICAMENTE con una de estas palabras en mayúsculas: DEPORTES, CULTURA, ACADEMICO, VOLUNTARIADO, SOCIAL. No agregues nada más.
                    """.trimIndent()
                ),
                OpenRouterMessage(
                    role    = "user",
                    content = "Título: \"$title\"\nDescripción: \"$description\""
                )
            )
        )
    }

    /**
     * Mapea el texto crudo del modelo a una [Category].
     * Tolerante a tildes, espacios extras y minúsculas.
     */
    private fun mapToCategory(raw: String): Category? {
        val normalized = raw
            .uppercase()
            .replace(Regex("[ÁÀÄÂ]"), "A")
            .replace(Regex("[ÉÈËÊ]"), "E")
            .replace(Regex("[ÍÌÏÎ]"), "I")
            .replace(Regex("[ÓÒÖÔ]"), "O")
            .replace(Regex("[ÚÙÜÛ]"), "U")
            .replace(Regex("[^A-Z]"), "")

        return when {
            "DEPORT"   in normalized -> Category.DEPORTES
            "CULTUR"   in normalized -> Category.CULTURA
            "ACADEMIC" in normalized -> Category.ACADEMICO
            "VOLUNT"   in normalized -> Category.VOLUNTARIADO
            "SOCIAL"   in normalized -> Category.SOCIAL
            else                     -> null.also {
                Log.w(TAG, "No se pudo mapear la respuesta: '$raw'")
            }
        }
    }

    /**
     * Calcula el delay de Exponential Backoff con jitter y cap máximo.
     *
     * Fórmula: min(BASE_DELAY * 2^attempt, MAX_DELAY) + jitter aleatorio de ±20%
     *
     * attempt=0 → ~1000ms
     * attempt=1 → ~2000ms
     * attempt=2 → ~4000ms (capeado a MAX_DELAY_MS si supera)
     */
    private fun computeBackoff(attempt: Int): Long {
        val exponential = (BASE_DELAY_MS * 2.0.pow(attempt)).toLong()
        val capped      = min(exponential, MAX_DELAY_MS)
        val jitter      = (capped * 0.2 * (Math.random() - 0.5)).toLong()
        return capped + jitter
    }

    private companion object {
        const val TAG          = "OpenRouterCategorizer"
        const val MAX_RETRIES  = 3
        const val BASE_DELAY_MS = 1_000L   // 1 segundo base
        const val MAX_DELAY_MS  = 8_000L   // cap en 8 segundos

        // Modelo gratuito estable en OpenRouter (mayo 2026).
        // Sufijo ":free" = cero costo, rate-limited.
        // Alternativas igualmente gratuitas:
        //   "deepseek/deepseek-chat:free"
        //   "meta-llama/llama-3.3-70b:free"
        //   "mistralai/mistral-7b-instruct:free"
        const val MODEL = "google/gemini-2.5-flash"
    }
}