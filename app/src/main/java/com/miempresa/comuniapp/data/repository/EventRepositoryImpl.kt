package com.miempresa.comuniapp.data.repository

import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor() : EventRepository {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    init {
        _events.value = listOf(
            Event(
                id = "1",
                title = "Torneo de Fútbol Comunitario",
                description = "Ven a participar en nuestro torneo relámpago de fútbol 7. Premios para los tres primeros lugares.",
                category = Category.DEPORTES,
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6097, -74.0817),

                startDate = "2026-05-25 08:00",
                endDate = "2026-05-25 17:00",

                maxAttendees = 50,
                currentAttendees = 32,

                organizerName = "Junta de Acción Comunal",

                eventStatus = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED,

                interestCount = 24,
                commentsCount = 8,

                ownerId = "1",
                moderationDate = null
            ),

            Event(
                id = "2",
                title = "Sesión de Yoga al Aire Libre",
                description = "Inicia tu domingo con energía y paz mental. Clase apta para todos los niveles.",
                category = Category.SOCIAL,
                imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6100, -74.0820),

                startDate = "2026-05-26 07:00",
                endDate = "2026-05-26 09:00",

                maxAttendees = 20,
                currentAttendees = 20,

                organizerName = "Camilo Yoga",

                eventStatus = EventStatus.FULL,
                verificationStatus = VerificationStatus.APPROVED,

                interestCount = 15,
                commentsCount = 3,

                ownerId = "2",
                moderationDate = null
            ),

            Event(
                id = "3",
                title = "Feria Gastronómica y Cultural",
                description = "Disfruta de los mejores platos típicos de nuestra región, música en vivo y artesanías locales.",
                category = Category.CULTURA,
                imageUrl = "https://images.unsplash.com/photo-1533777857889-4be7c70b33f7?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6110, -74.0830),

                startDate = "2026-05-31 10:00",
                endDate = "2026-06-01 20:00",

                maxAttendees = 200,
                currentAttendees = 85,

                organizerName = "Alcaldía Municipal",

                eventStatus = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.PENDING,

                interestCount = 45,
                commentsCount = 12,

                ownerId = "3",
                moderationDate = null
            ),

            Event(
                id = "4",
                title = "Jornada de Limpieza Comunitaria",
                description = "Únete a la limpieza del parque del barrio. Se entregarán bolsas y guantes.",
                category = Category.VOLUNTARIADO,
                imageUrl = "https://images.unsplash.com/photo-1509099836639-18ba1795216d?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6125, -74.0845),

                startDate = "2026-06-02 08:00",
                endDate = "2026-06-02 12:00",

                maxAttendees = null,
                currentAttendees = 10,

                organizerName = "Fundación Verde",

                eventStatus = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.REJECTED,

                interestCount = 5,
                commentsCount = 1,

                ownerId = "4",
                moderationDate = null
            )
        )
    }

    override suspend fun save(event: Event) {
        _events.value += event
    }

    override suspend fun findById(id: String): Event? {
        return _events.value.find { it.id == id }
    }

    override suspend fun delete(id: String) {
        _events.value = _events.value.filterNot { it.id == id }
    }

    override suspend fun update(event: Event) {
        _events.value = _events.value.map {
            if (it.id == event.id) event else it
        }
    }

    override suspend fun getPendingEvents(): List<Event> {
        return _events.value.filter { it.verificationStatus == VerificationStatus.PENDING }
    }

    override suspend fun approveEvent(eventId: String) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(verificationStatus = VerificationStatus.APPROVED) else it
        }
    }

    override suspend fun rejectEvent(eventId: String, reason: String) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(
                verificationStatus = VerificationStatus.REJECTED,
                rejectionReason = reason
            ) else it
        }
    }

    override fun getEventsByVerificationStatus(status: VerificationStatus): Flow<List<Event>> {
        return _events.map { list -> list.filter { it.verificationStatus == status } }
    }

    override suspend fun markAsFinished(eventId: String) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(eventStatus = EventStatus.FINISHED) else it
        }
    }

    override suspend fun updateEventStatus(eventId: String) {
        // Implementación lógica según sea necesario
    }

    override suspend fun getEventsByCategory(category: Category): List<Event> {
        return _events.value.filter { it.category == category }
    }

    override suspend fun getEventsNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event> {
        // Implementación simplificada
        return _events.value
    }

    override suspend fun getEventsByUser(userId: String): List<Event> {
        return _events.value.filter { it.ownerId == userId }
    }

    override suspend fun addInterest(eventId: String) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(interestCount = it.interestCount + 1) else it
        }
    }

    override suspend fun removeInterest(eventId: String) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(interestCount = (it.interestCount - 1).coerceAtLeast(0)) else it
        }
    }

    override suspend fun updateAttendeesCount(eventId: String, count: Int) {
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(currentAttendees = count) else it
        }
    }
}