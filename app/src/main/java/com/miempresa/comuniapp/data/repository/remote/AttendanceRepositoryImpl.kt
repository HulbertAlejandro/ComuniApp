package com.miempresa.comuniapp.data.repository.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [AttendanceRepository] con Firestore.
 *
 * ── Corrección del rebote visual en el botón "Asistir" ──────────────────────
 * El problema original era que [observeUserAttendance] derivaba el estado
 * del [_attendances] global usando un filtro en memoria. Esto causaba rebote
 * visual porque:
 *
 * 1. El usuario toca "Asistir" → [_isAttending] = true (optimista)
 * 2. Firestore confirma la escritura (~100ms)
 * 3. El listener global dispara con el snapshot anterior (sin el nuevo documento)
 * 4. [_attendances] se actualiza → filter devuelve false → [_isAttending] = false
 * 5. Firestore envía el snapshot final con el nuevo documento
 * 6. [_attendances] se actualiza → filter devuelve true → [_isAttending] = true
 *
 * La solución es que [observeUserAttendance] tenga su PROPIO listener de Firestore
 * filtrado exactamente por eventId + userId. Este listener solo dispara cuando
 * cambia la asistencia de ese usuario específico en ese evento específico,
 * eliminando las emisiones intermedias que causaban el rebote.
 */
@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AttendanceRepository {

    private val collection = firestore.collection("attendances")

    /**
     * StateFlow global de todas las asistencias.
     * Alimentado por un listener sin filtro para consultas generales.
     */
    private val _attendances = MutableStateFlow<List<Attendance>>(emptyList())
    override val attendances: StateFlow<List<Attendance>> = _attendances.asStateFlow()

    init {
        collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AttendanceRepository", "Listener global error: ${error.message}")
                return@addSnapshotListener
            }
            _attendances.value = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Attendance::class.java)?.apply { id = doc.id }
            } ?: emptyList()
        }
    }

    override suspend fun confirmAttendance(attendance: Attendance) {
        try {
            if (isUserAttending(attendance.eventId, attendance.userId)) return

            val docRef = collection.document()
            val finalAttendance = attendance.copy(
                id     = docRef.id,
                status = AttendanceStatus.CONFIRMED
            )
            docRef.set(finalAttendance).await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al confirmar asistencia: ${e.message}")
        }
    }

    override suspend fun updateAttendanceStatus(
        eventId: String,
        userId: String,
        status: AttendanceStatus
    ) {
        try {
            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get().await()

            val doc = snapshot.documents.firstOrNull() ?: return
            collection.document(doc.id).update("status", status.name).await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al actualizar asistencia: ${e.message}")
        }
    }

    override suspend fun getAttendanceByEvent(eventId: String): List<Attendance> {
        return try {
            val snapshot = collection.whereEqualTo("eventId", eventId).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Attendance::class.java)?.apply { id = doc.id }
            }
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al consultar asistencias del evento: ${e.message}")
        }
    }

    override suspend fun getAttendanceByUser(userId: String): List<Attendance> {
        return try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Attendance::class.java)?.apply { id = doc.id }
            }
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al consultar asistencias del usuario: ${e.message}")
        }
    }

    override suspend fun isUserAttending(
        eventId: String,
        userId: String
    ): Boolean {

        return try {

            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .whereEqualTo(
                    "status",
                    AttendanceStatus.CONFIRMED.name
                )
                .get()
                .await()

            !snapshot.isEmpty

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception(
                "Error al verificar asistencia: ${e.message}"
            )
        }
    }

    override suspend fun removeAttendance(eventId: String, userId: String) {
        try {
            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get().await()

            snapshot.documents.forEach { doc ->
                collection.document(doc.id).delete().await()
            }
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al eliminar asistencia: ${e.message}")
        }
    }

    /**
     * Observa en tiempo real si un usuario está asistiendo a un evento específico.
     *
     * ── Por qué usa su propio listener y NO el StateFlow global ─────────────
     * El StateFlow global [_attendances] emite cada vez que CUALQUIER asistencia
     * en CUALQUIER evento cambia. Derivar de él con un filtro en memoria causa
     * el rebote visual porque el listener global dispara con el estado anterior
     * antes de incluir el nuevo documento, generando una secuencia:
     *   true → false → true (al confirmar asistencia)
     *   false → true → false (al cancelar asistencia)
     *
     * Con un listener propio filtrado exactamente por eventId + userId, Firestore
     * solo dispara cuando cambia ESA asistencia específica, eliminando el rebote.
     *
     * ── Ciclo de vida ────────────────────────────────────────────────────────
     * El listener se crea cuando el ViewModel suscribe el Flow (en loadEvent)
     * y se elimina automáticamente en awaitClose() cuando el Job de asistencia
     * se cancela (cambio de sesión o destrucción del ViewModel).
     *
     * @param eventId ID del evento a observar.
     * @param userId  ID del usuario cuya asistencia se observa.
     * @return Flow<Boolean> que emite true si el usuario está asistiendo.
     */
    override fun observeUserAttendance(eventId: String, userId: String): Flow<Boolean> =
        callbackFlow {
            Log.d(
                "AttendanceRepository",
                "Iniciando listener de asistencia: eventId=$eventId userId=$userId"
            )

            val listener = collection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(
                            "AttendanceRepository",
                            "Error en listener de asistencia: ${error.message}"
                        )
                        // No cerrar el Flow; mantener el último estado conocido
                        return@addSnapshotListener
                    }
                    // El documento existe → usuario está asistiendo
                    val attending = snapshot != null && !snapshot.isEmpty
                    Log.d(
                        "AttendanceRepository",
                        "Estado asistencia actualizado: $attending para userId=$userId"
                    )
                    trySend(attending)
                }

            awaitClose {
                Log.d(
                    "AttendanceRepository",
                    "Listener de asistencia eliminado: eventId=$eventId userId=$userId"
                )
                listener.remove()
            }
        }
}