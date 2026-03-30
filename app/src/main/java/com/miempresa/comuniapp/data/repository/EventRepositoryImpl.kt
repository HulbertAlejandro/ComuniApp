package com.miempresa.comuniapp.data.repository

import com.miempresa.comuniapp.domain.model.Event
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
                locationName = "Cancha Sintética Municipal",
                latitude = 4.6097,
                longitude = -74.0817,
                startDate = "Sábado, 25 de Mayo - 08:00 AM",
                endDate = "Sábado, 25 de Mayo - 05:00 PM",
                maxAttendees = 50,
                currentAttendees = 32,
                organizerName = "Junta de Acción Comunal",
                organizerLevel = "Líder Comunitario",
                status = "ACTIVO",
                interestCount = 24,
                commentsCount = 8
            ),
            Event(
                id = "2",
                title = "Sesión de Yoga al Aire Libre",
                description = "Inicia tu domingo con energía y paz mental. Clase apta para todos los niveles.",
                category = "BIENESTAR",
                imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=800&auto=format&fit=crop",
                locationName = "Parque del Lago",
                latitude = 4.6100,
                longitude = -74.0820,
                startDate = "Domingo, 26 de Mayo - 07:00 AM",
                endDate = "Domingo, 26 de Mayo - 09:00 AM",
                maxAttendees = 20,
                currentAttendees = 20,
                organizerName = "Camilo Yoga",
                organizerLevel = "Organizador",
                status = "LLENO",
                interestCount = 15,
                commentsCount = 3
            ),
            Event(
                id = "3",
                title = "Feria Gastronómica y Cultural",
                description = "Disfruta de los mejores platos típicos de nuestra región, música en vivo y artesanías locales.",
                category = "CULTURAL",
                imageUrl = "https://images.unsplash.com/photo-1533777857889-4be7c70b33f7?q=80&w=800&auto=format&fit=crop",
                locationName = "Plaza Central",
                latitude = 4.6110,
                longitude = -74.0830,
                startDate = "Viernes, 31 de Mayo - 10:00 AM",
                endDate = "Sábado, 1 de Junio - 08:00 PM",
                maxAttendees = 200,
                currentAttendees = 85,
                organizerName = "Alcaldía Municipal",
                organizerLevel = "Líder Comunitario",
                status = "ACTIVO",
                interestCount = 45,
                commentsCount = 12
            )
        )
    }

    override fun getEventById(id: String): Event? {
        return _events.value.find { it.id == id }
    }
}
