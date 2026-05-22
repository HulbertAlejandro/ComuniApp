package com.miempresa.comuniapp.domain.service

import com.miempresa.comuniapp.domain.model.Category

/**
 * Contrato del servicio de clasificación automática por IA.
 * Separado del repositorio porque no persiste datos: solo transforma texto.
 */
interface CategorySuggestionService {

    /**
     * Analiza [title] y [description] con Gemini y retorna la [Category]
     * que mejor describe el contenido del evento.
     *
     * @return La categoría sugerida, o null si la respuesta del modelo
     *         no pudo mapearse a ninguna categoría conocida.
     * @throws Exception si falla la red o la API retorna un error.
     */
    suspend fun suggestCategory(title: String, description: String): Category?
}