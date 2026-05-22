package com.miempresa.comuniapp.domain.repository

import android.net.Uri

/**
 * Contrato para operaciones con Firebase Storage.
 *
 * Todas las funciones son suspend y lanzan [Exception] con mensaje
 * legible si fallan, igual que el resto de repositorios del proyecto.
 */
interface StorageRepository {

    /**
     * Sube la imagen ubicada en [localUri] a la ruta [storagePath] dentro
     * del bucket de Firebase Storage y retorna la URL pública de descarga.
     *
     * @param localUri    URI local del archivo (proveniente de cámara o galería).
     * @param storagePath Ruta destino dentro del bucket, ej:
     *                    "profile_pictures/uid123.jpg"
     * @return URL de descarga permanente (HTTPS) lista para usar con Coil.
     * @throws Exception si falla la subida o no se obtiene la URL.
     */
    suspend fun uploadImage(localUri: Uri, storagePath: String): String

    /**
     * Elimina el archivo en [storagePath] del bucket.
     * Útil al actualizar foto de perfil para no acumular archivos huérfanos.
     *
     * @throws Exception si la ruta no existe o falla la red.
     */
    suspend fun deleteImage(storagePath: String)
}