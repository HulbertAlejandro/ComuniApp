// data/service/HeuristicAiCategorizer.kt
package com.miempresa.comuniapp.data.service

import android.util.Log
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.service.AiCategorizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [AiCategorizer] 100% local y sin red.
 *
 * Se activa automáticamente cuando [OpenRouterAiCategorizer] falla
 * (sin internet, cuota agotada, error 5xx persistente).
 *
 * Sistema de puntuación:
 * - Coincidencia en el TÍTULO   = 2 puntos (más representativo del evento)
 * - Coincidencia en DESCRIPCIÓN = 1 punto
 * - Gana la categoría con mayor puntuación total.
 * - Empate: gana la primera en orden de prioridad (DEPORTES > CULTURA > …)
 * - Sin coincidencias → `null` (el banner de sugerencia no aparece)
 */
@Singleton
class HeuristicAiCategorizer @Inject constructor() : AiCategorizer {

    override suspend fun categorize(title: String, description: String): Category? {
        val titleNorm = title.lowercase().trim()
        val descNorm  = description.lowercase().trim()

        val scores = mutableMapOf<Category, Int>().withDefault { 0 }

        for ((category, keywords) in KEYWORD_MAP) {
            for (keyword in keywords) {
                if (keyword in titleNorm) scores[category] = scores.getValue(category) + 2
                if (keyword in descNorm)  scores[category] = scores.getValue(category) + 1
            }
        }

        val best = scores.maxByOrNull { it.value }
        return if ((best?.value ?: 0) > 0) {
            Log.d(TAG, "Heurística → ${best!!.key.name} (score=${best.value})")
            best.key
        } else {
            Log.d(TAG, "Heurística → sin coincidencias suficientes")
            null
        }
    }

    private companion object {
        const val TAG = "HeuristicCategorizer"

        val KEYWORD_MAP: Map<Category, List<String>> = mapOf(
            Category.DEPORTES to listOf(
                "fútbol", "futbol", "baloncesto", "basket", "tenis",
                "natación", "nadar", "correr", "maratón", "ciclismo",
                "bicicleta", "voleibol", "béisbol", "atletismo", "gimnasio",
                "torneo", "deporte", "partido", "entrenamiento", "liga",
                "campeonato", "olimpiadas", "piscina", "pádel", "padel",
                "squash", "rugby", "boxeo", "karate", "judo", "golf"
            ),
            Category.CULTURA to listOf(
                "teatro", "música", "musica", "concierto", "exposición",
                "exposicion", "arte", "danza", "baile", "pintura",
                "escultura", "cine", "película", "pelicula", "festival",
                "literatura", "poesía", "poesia", "libro", "lectura",
                "museo", "galería", "galeria", "cultura", "folclor",
                "patrimonio", "fotografía", "fotografia", "circo", "ópera",
                "opera", "orquesta", "banda", "coro"
            ),
            Category.ACADEMICO to listOf(
                "clase", "taller", "seminario", "conferencia", "charla",
                "curso", "capacitación", "capacitacion", "aprendizaje",
                "educación", "educacion", "universidad", "colegio",
                "escuela", "estudio", "investigación", "investigacion",
                "ciencia", "tecnología", "tecnologia", "formación",
                "formacion", "académico", "academico", "debate",
                "simposio", "congreso", "bootcamp", "hackathon", "stem",
                "matemáticas", "matematicas", "programación", "programacion"
            ),
            Category.VOLUNTARIADO to listOf(
                "ayuda", "voluntario", "voluntariado", "solidaridad",
                "donación", "donacion", "colecta", "recolección",
                "recoleccion", "limpieza", "apoyo", "beneficencia",
                "caridad", "asistencia", "colaboración", "colaboracion",
                "causa", "campaña", "campaña", "ong", "refugio",
                "banco de alimentos", "reforestar", "medio ambiente",
                "ecología", "ecologia", "reciclaje", "sostenibilidad"
            ),
            Category.SOCIAL to listOf(
                "reunión", "reunion", "encuentro", "celebración",
                "celebracion", "fiesta", "networking", "convivencia",
                "integración", "integracion", "vecinos", "barrio",
                "junta", "asamblea", "inauguración", "inauguracion",
                "bienvenida", "despedida", "aniversario", "cumpleaños",
                "parrillada", "picnic", "tertulia", "socializar",
                "comunidad", "plaza", "mercado", "feria"
            )
        )
    }
}