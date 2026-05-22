package com.miempresa.comuniapp.core.utils

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Convierte una [FirebaseAuthException] en un mensaje legible en español
 * para mostrar al usuario mediante [RequestResult.Failure].
 *
 * Los códigos de error de Firebase Auth son strings definidos en la
 * documentación oficial. Los más comunes para ComuniApp:
 *
 * - ERROR_EMAIL_ALREADY_IN_USE:  el email ya tiene una cuenta registrada.
 * - ERROR_INVALID_EMAIL:         formato de email incorrecto.
 * - ERROR_WEAK_PASSWORD:         contraseña menor a 6 caracteres.
 * - ERROR_USER_NOT_FOUND:        no existe cuenta con ese email.
 * - ERROR_WRONG_PASSWORD:        contraseña incorrecta.
 * - ERROR_USER_DISABLED:         la cuenta fue deshabilitada por el administrador.
 * - ERROR_TOO_MANY_REQUESTS:     demasiados intentos fallidos; cuenta bloqueada temporalmente.
 * - ERROR_NETWORK_REQUEST_FAILED: sin conexión a internet.
 * - ERROR_INVALID_CREDENTIAL:    credenciales inválidas o expiradas (Google Sign-In).
 */
fun FirebaseAuthException.toUserMessage(): String = when (errorCode) {
    "ERROR_EMAIL_ALREADY_IN_USE"   ->
        "Este correo electrónico ya está registrado. Intenta iniciar sesión."
    "ERROR_INVALID_EMAIL"          ->
        "El formato del correo electrónico no es válido."
    "ERROR_WEAK_PASSWORD"          ->
        "La contraseña es muy débil. Usa al menos 6 caracteres."
    "ERROR_USER_NOT_FOUND"         ->
        "No existe una cuenta con este correo electrónico."
    "ERROR_WRONG_PASSWORD"         ->
        "La contraseña es incorrecta. Intenta de nuevo."
    "ERROR_USER_DISABLED"          ->
        "Esta cuenta ha sido deshabilitada. Contacta al soporte."
    "ERROR_TOO_MANY_REQUESTS"      ->
        "Demasiados intentos fallidos. Espera unos minutos antes de intentar de nuevo."
    "ERROR_NETWORK_REQUEST_FAILED" ->
        "Sin conexión a internet. Verifica tu red e intenta de nuevo."
    "ERROR_INVALID_CREDENTIAL"     ->
        "Las credenciales son inválidas o han expirado."
    "ERROR_OPERATION_NOT_ALLOWED"  ->
        "Este método de inicio de sesión no está habilitado."
    else ->
        "Error de autenticación: ${message ?: errorCode}"
}

/**
 * Convierte una [FirebaseFirestoreException] en un mensaje legible en español.
 * (Sin cambios respecto a la versión anterior)
 */
fun FirebaseFirestoreException.toUserMessage(): String = when (code) {
    FirebaseFirestoreException.Code.UNAVAILABLE        ->
        "Sin conexión a internet. Verifica tu red e intenta de nuevo."
    FirebaseFirestoreException.Code.PERMISSION_DENIED  ->
        "No tienes permiso para realizar esta acción."
    FirebaseFirestoreException.Code.NOT_FOUND          ->
        "El recurso solicitado no existe."
    FirebaseFirestoreException.Code.ALREADY_EXISTS     ->
        "Este registro ya existe."
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
        "Servicio temporalmente no disponible. Intenta más tarde."
    FirebaseFirestoreException.Code.UNAUTHENTICATED    ->
        "Tu sesión expiró. Por favor inicia sesión de nuevo."
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED  ->
        "La operación tardó demasiado. Verifica tu conexión."
    FirebaseFirestoreException.Code.CANCELLED          ->
        "La operación fue cancelada."
    else ->
        "Error inesperado: ${message ?: "desconocido"}"
}