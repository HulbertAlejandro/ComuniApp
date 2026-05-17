package com.miempresa.comuniapp.core.utils

sealed class RequestResult {
    data class Success(val message: String) : RequestResult()
    data class Failure(val errorMessage: String) : RequestResult()
    object Loading : RequestResult() // Nuevo estado para representar la carga
}
