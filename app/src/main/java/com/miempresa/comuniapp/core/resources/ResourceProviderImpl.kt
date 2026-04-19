package com.miempresa.comuniapp.core.resources

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de ResourceProvider que usa el contexto de la aplicación
 * para obtener strings de los archivos de recursos.
 */
@Singleton
class ResourceProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ResourceProvider {

    override fun getString(id: Int): String {
        return context.getString(id)
    }

    override fun getFormattedString(id: Int, vararg args: Any): String {
        return context.getString(id, *args)
    }
}

