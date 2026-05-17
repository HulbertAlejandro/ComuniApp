package com.miempresa.comuniapp.data.repository.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AttendanceRepository {

    private val collection = firestore.collection("attendances")

    private val _attendances = MutableStateFlow<List<Attendance>>(emptyList())

    override val attendances: StateFlow<List<Attendance>> =
        _attendances.asStateFlow()

    init {

        collection.addSnapshotListener { snapshot, error ->

            if (error != null) return@addSnapshotListener

            snapshot?.let {

                _attendances.value = it.documents.mapNotNull { doc ->

                    doc.toObject(Attendance::class.java)?.apply {
                        id = doc.id
                    }
                }
            }
        }
    }

    override suspend fun confirmAttendance(attendance: Attendance) {

        try {

            if (
                isUserAttending(
                    attendance.eventId,
                    attendance.userId
                )
            ) {
                return
            }

            val docRef = collection.document()

            val finalAttendance = attendance.copy(
                id = docRef.id,
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
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
                ?: return

            collection
                .document(doc.id)
                .update(
                    "status",
                    status.name
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar asistencia: ${e.message}")
        }
    }

    override suspend fun getAttendanceByEvent(
        eventId: String
    ): List<Attendance> {

        try {

            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->

                doc.toObject(Attendance::class.java)?.apply {
                    id = doc.id
                }
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar asistencias del evento: ${e.message}")
        }
    }

    override suspend fun getAttendanceByUser(
        userId: String
    ): List<Attendance> {

        try {

            val snapshot = collection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->

                doc.toObject(Attendance::class.java)?.apply {
                    id = doc.id
                }
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

        try {

            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            return !snapshot.isEmpty

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al verificar asistencia: ${e.message}")
        }
    }

    override suspend fun removeAttendance(
        eventId: String,
        userId: String
    ) {

        try {

            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->

                collection
                    .document(doc.id)
                    .delete()
                    .await()
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al eliminar asistencia: ${e.message}")
        }
    }
}