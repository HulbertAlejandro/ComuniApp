package com.miempresa.comuniapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── REQUEST ───────────────────────────────────────────────────────────────────

data class OpenRouterRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<OpenRouterMessage>,

    /** Limita la respuesta a muy pocos tokens; una categoría es 1-2 palabras. */
    @SerializedName("max_tokens")
    val maxTokens: Int = 20,

    /** 0 = determinista, siempre el mismo output para el mismo input. */
    @SerializedName("temperature")
    val temperature: Float = 0f
)

data class OpenRouterMessage(
    @SerializedName("role")
    val role: String,   // "system" | "user" | "assistant"

    @SerializedName("content")
    val content: String
)

// ── RESPONSE ──────────────────────────────────────────────────────────────────

data class OpenRouterResponse(
    @SerializedName("choices")
    val choices: List<OpenRouterChoice>?
)

data class OpenRouterChoice(
    @SerializedName("message")
    val message: OpenRouterMessage?
)