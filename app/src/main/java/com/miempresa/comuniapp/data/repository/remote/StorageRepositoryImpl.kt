package com.miempresa.comuniapp.data.repository.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.miempresa.comuniapp.domain.repository.StorageRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [StorageRepository] usando Firebase Storage.
 *
 * Patrón de ruta recomendado:
 *   profile_pictures/{userId}.jpg
 *   event_images/{eventId}/{uuid}.jpg
 *
 * Usar el UID/eventId en la ruta garantiza que cada usuario solo pueda
 * sobreescribir su propio archivo (con las reglas de seguridad correctas).
 */
@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
) : StorageRepository {

    /**
     * Sube [localUri] a [storagePath] y retorna la URL de descarga.
     *
     * Flujo interno:
     * 1. Obtiene la referencia al archivo dentro del bucket.
     * 2. Llama a [putFile] con la URI local (funciona con content:// y file://).
     * 3. Espera la tarea con [await] (integración corrutinas de KTX).
     * 4. Solicita la URL de descarga con [downloadUrl].
     */
    override suspend fun uploadImage(localUri: Uri, storagePath: String): String {
        return try {
            val ref = storage.reference.child(storagePath)

            // putFile acepta URIs de galería (content://) y de FileProvider (file://)
            ref.putFile(localUri).await()

            // downloadUrl es una tarea separada; también necesita await
            ref.downloadUrl.await().toString()

        } catch (e: StorageException) {
            throw Exception("Error al subir la imagen: ${e.message}")
        } catch (e: Exception) {
            throw Exception("Error inesperado al subir la imagen: ${e.message}")
        }
    }

    /**
     * Elimina el archivo en [storagePath].
     * No lanza excepción si el archivo no existe (error code OBJECT_NOT_FOUND),
     * para que actualizar la foto de perfil sea idempotente.
     */
    override suspend fun deleteImage(storagePath: String) {
        try {
            storage.reference.child(storagePath).delete().await()
        } catch (e: StorageException) {
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) return
            throw Exception("Error al eliminar la imagen: ${e.message}")
        } catch (e: Exception) {
            throw Exception("Error inesperado al eliminar la imagen: ${e.message}")
        }
    }
}