package com.miempresa.comuniapp.data.repository

import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                category = "DEPORTES",
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6097, -74.0817),

                startDate = "2026-05-25 08:00",
                endDate = "2026-05-25 17:00",

                maxAttendees = 50,
                currentAttendees = 32,

                organizerName = "Junta de Acción Comunal",
                organizerLevel = "Líder Comunitario",

                eventStatus = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED,

                interestCount = 24,
                commentsCount = 8,

                ownerId = "1"
            ),

            Event(
                id = "2",
                title = "Sesión de Yoga al Aire Libre",
                description = "Inicia tu domingo con energía y paz mental. Clase apta para todos los niveles.",
                category = "BIENESTAR",
                imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6100, -74.0820),

                startDate = "2026-05-26 07:00",
                endDate = "2026-05-26 09:00",

                maxAttendees = 20,
                currentAttendees = 20,

                organizerName = "Camilo Yoga",
                organizerLevel = "Organizador",

                eventStatus = EventStatus.FULL,
                verificationStatus = VerificationStatus.APPROVED,

                interestCount = 15,
                commentsCount = 3,

                ownerId = "2"
            ),

            Event(
                id = "3",
                title = "Feria Gastronómica y Cultural",
                description = "Disfruta de los mejores platos típicos de nuestra región, música en vivo y artesanías locales.",
                category = "CULTURAL",
                imageUrl = "https://images.unsplash.com/photo-1533777857889-4be7c70b33f7?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6110, -74.0830),

                startDate = "2026-05-31 10:00",
                endDate = "2026-06-01 20:00",

                maxAttendees = 200,
                currentAttendees = 85,

                organizerName = "Alcaldía Municipal",
                organizerLevel = "Líder Comunitario",

                eventStatus = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.PENDING,

                interestCount = 45,
                commentsCount = 12,

                ownerId = "3"
            ),

            Event(
                id = "4",
                title = "Jornada de Limpieza Comunitaria",
                description = "Únete a la limpieza del parque del barrio. Se entregarán bolsas y guantes.",
                category = "VOLUNTARIADO",
                imageUrl = "https://images.unsplash.com/photo-1509099836639-18ba1795216d?q=80&w=800&auto=format&fit=crop",

                location = Location(4.6125, -74.0845),

                startDate = "2026-06-02 08:00",
                endDate = "2026-06-02 12:00",

                maxAttendees = null,
                currentAttendees = 10,

                organizerName = "Fundación Verde",
                organizerLevel = "Organizador",

                eventStatus = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.REJECTED,

                interestCount = 5,
                commentsCount = 1,

                ownerId = "4"
            )
        )
    }

    override fun save(event: Event) {
        _events.value += event
    }

    override fun findById(id: String): Event? {
        return _events.value.find { it.id == id }
    }

    override fun delete(id: String) {
        _events.value = _events.value.filterNot { it.id == id }
    }

    override fun update(event: Event) {
        _events.value = _events.value.map {
            if (it.id == event.id) event else it
        }
    }
}