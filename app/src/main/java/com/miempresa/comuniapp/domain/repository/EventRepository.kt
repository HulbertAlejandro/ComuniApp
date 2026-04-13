package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface EventRepository {

    val events: StateFlow<List<Event>>

    suspend fun save(event: Event)

    suspend fun findById(id: String): Event?

    suspend fun update(event: Event)

    suspend fun delete(id: String)

    // =============================
    // Moderación
    // =============================

    suspend fun getPendingEvents(): List<Event>

    suspend fun approveEvent(eventId: String)

    suspend fun rejectEvent(eventId: String, reason: String)

    /**
     * Devuelve un Flow de eventos filtrados por su estado de verificación.
     * Útil para el feed principal (APPROVED).
     */
    fun getEventsByVerificationStatus(status: VerificationStatus): Flow<List<Event>>

    // =============================
    // Estados del evento
    // =============================

    suspend fun markAsFinished(eventId: String)

    suspend fun updateEventStatus(eventId: String)

    // =============================
    // Filtros (Feed)
    // =============================

    suspend fun getEventsByCategory(category: Category): List<Event>

    suspend fun getEventsNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event>

    suspend fun getEventsByUser(userId: String): List<Event>

    // =============================
    // Interacción social
    // =============================

    suspend fun addInterest(eventId: String)

    suspend fun removeInterest(eventId: String)

    suspend fun getInterestedEventIds(): Flow<Set<String>>

    // =============================
    // Asistencia (cupo)
    // =============================

    suspend fun updateAttendeesCount(eventId: String, count: Int)

}
