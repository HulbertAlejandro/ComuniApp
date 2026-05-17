package com.miempresa.comuniapp.core.utils

import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Convierte una [FirebaseFirestoreException] en un mensaje legible en español
 * para mostrar al usuario mediante [RequestResult.Failure].
 *
 * [FirebaseFirestoreException.Code] cubre los errores más comunes de Firestore:
 * - UNAVAILABLE:          sin conexión a internet.
 * - PERMISSION_DENIED:    las reglas de seguridad bloquearon la operación.
 * - NOT_FOUND:            el documento no existe.
 * - ALREADY_EXISTS:       se intentó crear un documento que ya existe.
 * - RESOURCE_EXHAUSTED:   cuota de lecturas/escrituras agotada.
 * - UNAUTHENTICATED:      el usuario no está autenticado.
 * - DEADLINE_EXCEEDED:    la operación tardó demasiado (timeout).
 * - CANCELLED:            la operación fue cancelada (app en background).
 */
fun FirebaseFirestoreException.toUserMessage(): String = when (code) {
    FirebaseFirestoreException.Code.UNAVAILABLE ->
        "Sin conexión a internet. Verifica tu red e intenta de nuevo."
    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "No tienes permiso para realizar esta acción."
    FirebaseFirestoreException.Code.NOT_FOUND ->
        "El recurso solicitado no existe."
    FirebaseFirestoreException.Code.ALREADY_EXISTS ->
        "Este registro ya existe."
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
        "Servicio temporalmente no disponible. Intenta más tarde."
    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
        "Tu sesión expiró. Por favor inicia sesión de nuevo."
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
        "La operación tardó demasiado. Verifica tu conexión."
    FirebaseFirestoreException.Code.CANCELLED ->
        "La operación fue cancelada."
    else ->
        "Error inesperado: ${message ?: "desconocido"}"
}