package com.miempresa.comuniapp.core.resources

/**
 * Interfaz para obtener strings de recursos de forma centralizada.
 * Permite inyectar strings en ViewModels sin depender directamente del contexto.
 */
interface ResourceProvider {
    fun getString(id: Int): String
    fun getFormattedString(id: Int, vararg args: Any): String
}

