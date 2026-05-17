package com.miempresa.comuniapp.data.repository.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : EventRepository {

    private val collection = firestore.collection("events")

    private val _events = MutableStateFlow<List<Event>>(emptyList())

    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    init {

        collection.addSnapshotListener { snapshot, error ->

            if (error != null) return@addSnapshotListener

            snapshot?.let {

                _events.value = it.documents.mapNotNull { doc ->

                    doc.toObject(Event::class.java)?.apply {
                        id = doc.id
                    }
                }
            }
        }
    }

    override suspend fun save(event: Event) {

        try {

            val docRef = collection.document()

            val finalEvent = event.copy(
                id = docRef.id,
                eventStatus = EventStatus.CREATED,
                verificationStatus = VerificationStatus.PENDING
            )

            docRef.set(finalEvent).await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al guardar el evento: ${e.message}")
        }
    }

    override suspend fun findById(id: String): Event? {

        try {

            val doc = collection
                .document(id)
                .get()
                .await()

            return doc.toObject(Event::class.java)?.apply {
                this.id = doc.id
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al buscar el evento: ${e.message}")
        }
    }

    override suspend fun update(event: Event) {

        try {

            collection
                .document(event.id)
                .set(event)
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar el evento: ${e.message}")
        }
    }

    override suspend fun delete(id: String) {

        try {

            collection
                .document(id)
                .delete()
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al eliminar el evento: ${e.message}")
        }
    }

    // =========================================================
    // MODERACIÓN
    // =========================================================

    override suspend fun getPendingEvents(): List<Event> {

        try {

            val snapshot = collection
                .whereEqualTo(
                    "verificationStatus",
                    VerificationStatus.PENDING.name
                )
                .orderBy("date")
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->

                doc.toObject(Event::class.java)?.apply {
                    id = doc.id
                }
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar eventos pendientes: ${e.message}")
        }
    }

    override suspend fun approveEvent(eventId: String) {

        try {

            collection
                .document(eventId)
                .update(
                    mapOf(
                        "verificationStatus" to VerificationStatus.APPROVED.name,
                        "rejectionReason" to null
                    )
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al aprobar el evento: ${e.message}")
        }
    }

    override suspend fun rejectEvent(
        eventId: String,
        reason: String
    ) {

        try {

            collection
                .document(eventId)
                .update(
                    mapOf(
                        "verificationStatus" to VerificationStatus.REJECTED.name,
                        "rejectionReason" to reason
                    )
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al rechazar el evento: ${e.message}")
        }
    }

    override fun getEventsByVerificationStatus(
        status: VerificationStatus
    ): Flow<List<Event>> {

        return _events.map { list ->
            list.filter {
                it.verificationStatus == status
            }
        }
    }

    // =========================================================
    // ESTADOS
    // =========================================================

    override suspend fun markAsFinished(eventId: String) {

        try {

            collection
                .document(eventId)
                .update(
                    "eventStatus",
                    EventStatus.FINISHED.name
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al finalizar evento: ${e.message}")
        }
    }

    override suspend fun updateEventStatus(eventId: String) {

        try {

            val event = findById(eventId)
                ?: return

            val newStatus = when {

                event.maxAttendees != null &&
                        event.currentAttendees >= event.maxAttendees -> {
                    EventStatus.FULL
                }

                else -> EventStatus.ACTIVE
            }

            collection
                .document(eventId)
                .update(
                    "eventStatus",
                    newStatus.name
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar estado del evento: ${e.message}")
        }
    }

    // =========================================================
    // FILTROS
    // =========================================================

    override suspend fun getEventsByCategory(
        category: Category
    ): List<Event> {

        try {

            /**
             * Consulta avanzada:
             * - whereEqualTo
             * - enum.name
             * - orderBy
             */
            val snapshot = collection
                .whereEqualTo(
                    "category",
                    category.name
                )
                .whereEqualTo(
                    "verificationStatus",
                    VerificationStatus.APPROVED.name
                )
                .orderBy("date")
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->

                doc.toObject(Event::class.java)?.apply {
                    id = doc.id
                }
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar eventos por categoría: ${e.message}")
        }
    }

    override suspend fun getEventsNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event> {

        return try {

            _events.value.filter { event ->

                calculateDistanceKm(
                    latitude,
                    longitude,
                    event.eventLocation.latitude,
                    event.eventLocation.longitude
                ) <= radiusKm
            }

        } catch (e: Exception) {

            throw Exception("Error al consultar eventos cercanos: ${e.message}")
        }
    }

    override suspend fun getEventsByUser(userId: String): List<Event> {

        try {

            val snapshot = collection
                .whereEqualTo("ownerId", userId)
                .orderBy("date")
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->

                doc.toObject(Event::class.java)?.apply {
                    id = doc.id
                }
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar eventos del usuario: ${e.message}")
        }
    }

    override suspend fun getEventsByCreator(userId: String): List<Event> {

        return getEventsByUser(userId)
    }

    // =========================================================
    // INTERACCIÓN
    // =========================================================

    override suspend fun addInterest(eventId: String) {

        try {

            collection
                .document(eventId)
                .update(
                    "interestCount",
                    FieldValue.increment(1)
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al agregar interés: ${e.message}")
        }
    }

    override suspend fun removeInterest(eventId: String) {

        try {

            collection
                .document(eventId)
                .update(
                    "interestCount",
                    FieldValue.increment(-1)
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al remover interés: ${e.message}")
        }
    }

    // =========================================================
    // ASISTENTES
    // =========================================================

    override suspend fun updateAttendeesCount(
        eventId: String,
        count: Int
    ) {

        try {

            collection
                .document(eventId)
                .update(
                    "currentAttendees",
                    count
                )
                .await()

            updateEventStatus(eventId)

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar asistentes: ${e.message}")
        }
    }

    // =========================================================
    // CONSULTA POR IDS
    // =========================================================

    override suspend fun getEventsByIds(ids: List<String>): List<Event> {

        try {

            if (ids.isEmpty()) return emptyList()

            /**
             * Consulta avanzada usando whereIn.
             * Firestore permite máximo 10 IDs por consulta.
             */
            val chunks = ids.chunked(10)

            val events = mutableListOf<Event>()

            chunks.forEach { chunk ->

                val snapshot = collection
                    .whereIn("id", chunk)
                    .get()
                    .await()

                events.addAll(
                    snapshot.documents.mapNotNull { doc ->

                        doc.toObject(Event::class.java)?.apply {
                            id = doc.id
                        }
                    }
                )
            }

            return events

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar eventos por IDs: ${e.message}")
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val r = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)

        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)

        return r * 2 * atan2(
            sqrt(a),
            sqrt(1 - a)
        )
    }
}