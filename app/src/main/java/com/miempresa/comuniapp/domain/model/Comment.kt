package com.miempresa.comuniapp.domain.model

data class Comment(
    val id: String,
    val eventId: String,
    val authorId: String,
    val content: String,
    val timestamp: Long
)