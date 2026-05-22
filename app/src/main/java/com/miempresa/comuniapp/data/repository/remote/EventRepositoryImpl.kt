package com.miempresa.comuniapp.data.repository.remote


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
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
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : EventRepository {


    private val collection = firestore.collection("events")


    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()


    private var snapshotListener: ListenerRegistration? = null


    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->


        Log.d("EventRepository", "Auth changed: ${firebaseAuth.currentUser?.uid}")


        snapshotListener?.remove()
        snapshotListener = null


        if (firebaseAuth.currentUser != null) {
            startEventsListener()
        } else {
            _events.value = emptyList()
        }
    }


    init {
        auth.addAuthStateListener(authStateListener)
    }


    private fun startEventsListener() {


        snapshotListener?.remove()


        snapshotListener = collection
            .addSnapshotListener { snapshot, error ->


                if (error != null) {


                    Log.e(
                        "EventRepository",
                        "Firestore listener error: ${error.message}"
                    )


                    return@addSnapshotListener
                }


                val events = snapshot?.documents?.mapNotNull { doc ->


                    doc.toObject(Event::class.java)
                        ?.apply {
                            id = doc.id
                        }


                } ?: emptyList()


                _events.value = events
            }
    }


    override suspend fun save(event: Event): String {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: throw Exception("Usuario no autenticado")


            val docRef = collection.document()


            val finalEvent = event.copy(
                id = docRef.id,
                ownerId = currentUid,
                eventStatus = EventStatus.CREATED,
                verificationStatus = VerificationStatus.PENDING
            )


            docRef.set(finalEvent).await()


            docRef.id // 🔥 RETORNAMOS EL ID RECIÉN GENERADO


        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al guardar evento: ${e.message}")
        }
    }


    override suspend fun findById(id: String): Event? {


        return try {


            val doc = collection.document(id).get().await()


            doc.toObject(Event::class.java)
                ?.apply {
                    this.id = doc.id
                }


        } catch (e: FirebaseFirestoreException) {


            throw Exception(e.toUserMessage())


        } catch (e: Exception) {


            throw Exception(
                "Error al buscar evento: ${e.message}"
            )
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


            throw Exception(
                "Error al actualizar evento: ${e.message}"
            )
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


            throw Exception(
                "Error al eliminar evento: ${e.message}"
            )
        }
    }


    override suspend fun approveEvent(eventId: String) {


        try {


            collection
                .document(eventId)
                .update(
                    mapOf(
                        "verificationStatus" to VerificationStatus.APPROVED.name,
                        "eventStatus" to EventStatus.ACTIVE.name,
                        "rejectionReason" to null
                    )
                )
                .await()


        } catch (e: FirebaseFirestoreException) {


            throw Exception(e.toUserMessage())


        } catch (e: Exception) {


            throw Exception(
                "Error al aprobar evento: ${e.message}"
            )
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
                        "eventStatus" to EventStatus.CREATED.name,
                        "rejectionReason" to reason
                    )
                )
                .await()


        } catch (e: FirebaseFirestoreException) {


            throw Exception(e.toUserMessage())


        } catch (e: Exception) {


            throw Exception(
                "Error al rechazar evento: ${e.message}"
            )
        }
    }


    override fun getEventsByVerificationStatus(
        status: VerificationStatus
    ): Flow<List<Event>> {


        return _events.map { events ->
            events.filter {
                it.verificationStatus == status
            }
        }
    }


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


            throw Exception(
                "Error al finalizar evento: ${e.message}"
            )
        }
    }


    override suspend fun getEventsByIds(ids: List<String>): List<Event> {


        if (ids.isEmpty()) return emptyList()


        return try {


            val chunks = ids.chunked(30)


            val result = mutableListOf<Event>()


            chunks.forEach { chunk ->


                val snapshot = collection
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()


                result.addAll(
                    snapshot.documents.mapNotNull { doc ->


                        doc.toObject(Event::class.java)
                            ?.apply {
                                id = doc.id
                            }
                    }
                )
            }


            result


        } catch (e: FirebaseFirestoreException) {


            throw Exception(e.toUserMessage())


        } catch (e: Exception) {


            throw Exception(
                "Error al consultar eventos: ${e.message}"
            )
        }
    }


    override suspend fun getEventsByUser(userId: String): List<Event> {


        return try {


            val snapshot = collection
                .whereEqualTo("ownerId", userId)
                .orderBy("startDate")
                .get()
                .await()


            snapshot.documents.mapNotNull { doc ->


                doc.toObject(Event::class.java)
                    ?.apply {
                        id = doc.id
                    }
            }


        } catch (e: FirebaseFirestoreException) {


            throw Exception(e.toUserMessage())


        } catch (e: Exception) {


            throw Exception(
                "Error al consultar eventos del usuario: ${e.message}"
            )
        }
    }


    override suspend fun getEventsByCreator(userId: String): List<Event> {
        return getEventsByUser(userId)
    }


    override suspend fun addInterest(eventId: String) {


        collection.document(eventId)
            .update(
                "interestCount",
                FieldValue.increment(1)
            )
            .await()
    }


    override suspend fun removeInterest(eventId: String) {


        collection.document(eventId)
            .update(
                "interestCount",
                FieldValue.increment(-1)
            )
            .await()
    }


    override suspend fun updateAttendeesCount(
        eventId: String,
        count: Int
    ) {
        try {

            collection.document(eventId)
                .update(
                    "currentAttendees",
                    FieldValue.increment(count.toLong())
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception(
                "Error al actualizar asistentes: ${e.message}"
            )
        }
    }


    override suspend fun updateEventStatus(eventId: String) {


        val event = findById(eventId)
            ?: return


        val newStatus =
            if (
                event.maxAttendees != null &&
                event.currentAttendees >= event.maxAttendees
            ) {
                EventStatus.FULL
            } else {
                EventStatus.ACTIVE
            }


        collection.document(eventId)
            .update(
                "eventStatus",
                newStatus.name
            )
            .await()
    }


    override suspend fun getPendingEvents(): List<Event> {


        return events.value.filter {
            it.verificationStatus == VerificationStatus.PENDING
        }
    }


    override suspend fun getEventsByCategory(
        category: Category
    ): List<Event> {


        return events.value.filter {
            it.category == category &&
                    it.verificationStatus == VerificationStatus.APPROVED
        }
    }


    override suspend fun getEventsNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event> {


        return events.value.filter { event ->


            calculateDistanceKm(
                latitude,
                longitude,
                event.eventLocation.latitude,
                event.eventLocation.longitude
            ) <= radiusKm
        }
    }


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

    override suspend fun incrementCommentsCount(eventId: String) {

        collection.document(eventId)
            .update(
                "commentsCount",
                FieldValue.increment(1)
            )
            .await()
    }

    override suspend fun decrementCommentsCount(eventId: String) {

        collection.document(eventId)
            .update(
                "commentsCount",
                FieldValue.increment(-1)
            )
            .await()
    }
}
