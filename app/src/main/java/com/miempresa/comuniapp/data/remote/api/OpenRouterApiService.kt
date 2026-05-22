package com.miempresa.comuniapp.data.remote.api

import com.miempresa.comuniapp.data.remote.dto.OpenRouterRequest
import com.miempresa.comuniapp.data.remote.dto.OpenRouterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApiService {

    /**
     * Endpoint único de OpenRouter (100% compatible con la spec de OpenAI).
     * Los headers de autenticación y atribución se inyectan via [OkHttpClient]
     * en el módulo Hilt, no aquí, para mantener este contrato limpio.
     */
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>
}