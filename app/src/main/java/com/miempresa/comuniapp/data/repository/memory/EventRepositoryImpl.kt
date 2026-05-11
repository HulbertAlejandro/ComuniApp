package com.miempresa.comuniapp.data.repository.memory

import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class EventRepositoryImpl @Inject constructor() : EventRepository {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    init {
        _events.value = seedEvents()
    }

    override suspend fun save(event: Event) {
        // Ahora el evento ya trae su lista de imageUris desde el ViewModel
        _events.value += event.copy(
            eventStatus = EventStatus.CREATED,
            verificationStatus = VerificationStatus.PENDING
        )
    }

    override suspend fun findById(id: String): Event? =
        _events.value.find { it.id == id }

    override suspend fun update(event: Event) {
        _events.value = _events.value.map { if (it.id == event.id) event else it }
    }

    override suspend fun delete(id: String) {
        _events.value = _events.value.filterNot { it.id == id }
    }

    // =============================
    // Moderación
    // =============================

    override suspend fun getPendingEvents(): List<Event> =
        _events.value.filter { it.verificationStatus == VerificationStatus.PENDING }

    override suspend fun approveEvent(eventId: String) {
        updateStatus(eventId, VerificationStatus.APPROVED, null)
    }

    override suspend fun rejectEvent(eventId: String, reason: String) {
        updateStatus(eventId, VerificationStatus.REJECTED, reason)
    }

    override fun getEventsByVerificationStatus(status: VerificationStatus): Flow<List<Event>> =
        _events.map { list -> list.filter { it.verificationStatus == status } }

    private suspend fun updateStatus(eventId: String, status: VerificationStatus, reason: String?) {
        val event = findById(eventId) ?: return
        update(event.copy(verificationStatus = status, rejectionReason = reason))
    }

    // =============================
    // Estados
    // =============================

    override suspend fun markAsFinished(eventId: String) {
        val event = findById(eventId) ?: return
        update(event.copy(eventStatus = EventStatus.FINISHED))
    }

    override suspend fun updateEventStatus(eventId: String) {
        val event = findById(eventId) ?: return
        val newStatus = when {
            event.maxAttendees != null &&
                    event.currentAttendees >= event.maxAttendees -> EventStatus.FULL
            else -> EventStatus.ACTIVE
        }
        update(event.copy(eventStatus = newStatus))
    }

    // =============================
    // Filtros
    // =============================

    override suspend fun getEventsByCategory(category: Category): List<Event> =
        _events.value.filter { it.category == category }

    override suspend fun getEventsNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event> = _events.value.filter {
        distanceKm(latitude, longitude, it.eventLocation.latitude, it.eventLocation.longitude) <= radiusKm
    }

    override suspend fun getEventsByUser(userId: String): List<Event> =
        _events.value.filter { it.ownerId == userId }

    override suspend fun getEventsByCreator(userId: String): List<Event> =
        _events.value.filter { it.ownerId == userId }

    // =============================
    // Interacción
    // =============================

    override suspend fun addInterest(eventId: String) {
        val event = findById(eventId) ?: return
        update(event.copy(interestCount = event.interestCount + 1))
    }

    override suspend fun removeInterest(eventId: String) {
        val event = findById(eventId) ?: return
        update(event.copy(interestCount = maxOf(0, event.interestCount - 1)))
    }

    // =============================
    // Cupo
    // =============================

    override suspend fun updateAttendeesCount(eventId: String, count: Int) {
        val event = findById(eventId) ?: return
        update(event.copy(currentAttendees = count))
        updateEventStatus(eventId)
    }

    // =============================
    // Helpers
    // =============================

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun seedEvents(): List<Event> {
        return listOf(
            Event(
                id = "1",
                title = "Torneo de Fútbol Comunitario",
                description = "Participa en nuestro torneo local y gana premios. Contaremos con hidratación y arbitraje profesional.",
                category = Category.DEPORTES,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=800",
                    "https://images.unsplash.com/photo-1517466787929-bc90951d0974?q=80&w=800",
                    "https://images.unsplash.com/photo-1510566337590-2fc1f21d0faa?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5393, -75.6728),
                startDate = "2026-05-25 08:00",
                endDate   = "2026-05-25 17:00",
                maxAttendees     = 50,
                currentAttendees = 32,
                ownerId            = "1",
                organizerName      = "Junta de Acción Comunal",
                eventStatus        = EventStatus.CREATED,
                verificationStatus = VerificationStatus.PENDING
            ),

            Event(
                id = "2",
                title = "Clase de Yoga al Aire Libre",
                description = "Relájate y conecta con la naturaleza en el Parque de la Vida. Trae tu propio mat.",
                category = Category.DEPORTES,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=800",
                    "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=800",
                    "https://images.unsplash.com/photo-1552196563-55cd4e45efb3?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5490, -75.6650),
                startDate = "2026-05-26 07:00",
                endDate   = "2026-05-26 09:00",
                maxAttendees     = 20,
                currentAttendees = 15,
                ownerId            = "1",
                organizerName      = "Camilo Yoga",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "3",
                title = "Feria Gastronómica Armenia",
                description = "Comida típica quindiana, música y cultura local. ¡No te pierdas el concurso del mejor sudado!",
                category = Category.CULTURA,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1533777857889-4be7c70b33f7?q=80&w=800",
                    "https://images.unsplash.com/photo-1504674900247-0877df9cc836?q=80&w=800",
                    "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5300, -75.6700),
                startDate = "2026-05-31 10:00",
                endDate   = "2026-06-01 20:00",
                maxAttendees     = 200,
                currentAttendees = 85,
                ownerId            = "1",
                organizerName      = "Alcaldía de Armenia",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "4",
                title = "Jornada de Limpieza del Parque",
                description = "Ayúdanos a cuidar el Parque Metropolitano La Secreta. Nosotros ponemos las bolsas y guantes.",
                category = Category.VOLUNTARIADO,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1509099836639-18ba1795216d?q=80&w=800",
                    "https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?q=80&w=800",
                    "https://images.unsplash.com/photo-1618477461853-cf6ed80fafa5?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5200, -75.6850),
                startDate = "2026-06-02 08:00",
                endDate   = "2026-06-02 12:00",
                maxAttendees     = null,
                currentAttendees = 10,
                ownerId            = "4",
                organizerName      = "Fundación Verde Quindío",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "5",
                title = "Taller de Programación Kotlin",
                description = "Aprende desarrollo Android desde cero en el campus. Trae tu laptop.",
                category = Category.ACADEMICO,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800",
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=800",
                    "https://images.unsplash.com/photo-1498050108023-c5249f4df085?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5550, -75.6580),
                startDate = "2026-06-05 18:00",
                endDate   = "2026-06-05 21:00",
                maxAttendees     = 30,
                currentAttendees = 12,
                ownerId            = "2",
                organizerName      = "Centro de Programación UQ",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "6",
                title = "Ciclopaseo Nocturno Armenia",
                description = "Recorrido en bicicleta por la ciudad cafetera. Indispensable casco y luces.",
                category = Category.DEPORTES,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1508973378895-8d1f2d4e94c6?q=80&w=800",
                    "https://images.unsplash.com/photo-1471506480208-8a93a68634b7?q=80&w=800",
                    "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5340, -75.6950),
                startDate = "2026-06-07 19:00",
                endDate   = "2026-06-07 22:00",
                maxAttendees     = 100,
                currentAttendees = 60,
                ownerId            = "1",
                organizerName      = "Asociación Ciclista Quindío",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "7",
                title = "Charla de Emprendimiento",
                description = "Aprende a crear tu propio negocio en el Quindío con expertos del sector.",
                category = Category.ACADEMICO,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1552664730-d307ca884978?q=80&w=800",
                    "https://images.unsplash.com/photo-1557804506-669a67965ba0?q=80&w=800",
                    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5150, -75.6750),
                startDate = "2026-06-10 17:00",
                endDate   = "2026-06-10 20:00",
                maxAttendees     = 40,
                currentAttendees = 25,
                ownerId            = "3",
                organizerName      = "Cámara de Comercio Armenia",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "8",
                title = "Feria de Emprendedores Locales",
                description = "Apoya negocios locales y productos artesanales del eje cafetero.",
                category = Category.SOCIAL,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1521334884684-d80222895322?q=80&w=800",
                    "https://images.unsplash.com/photo-1531050171651-61afc2834d75?q=80&w=800",
                    "https://images.unsplash.com/photo-1475483768296-6163e08872a1?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5100, -75.7100),
                startDate = "2026-06-12 09:00",
                endDate   = "2026-06-12 18:00",
                maxAttendees     = 150,
                currentAttendees = 90,
                ownerId            = "2",
                organizerName      = "Asociación de Emprendedores UQ",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "9",
                title = "Cine Comunitario al Aire Libre",
                description = "Película gratuita para toda la familia en el campus. Trae tu manta y snacks.",
                category = Category.CULTURA,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=800",
                    "https://images.unsplash.com/photo-1517604931442-7e0c8ed0963c?q=80&w=800",
                    "https://images.unsplash.com/photo-1524712245354-2c4e5e7121c0?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5650, -75.6500),
                startDate = "2026-06-15 19:00",
                endDate   = "2026-06-15 22:00",
                maxAttendees     = 80,
                currentAttendees = 40,
                ownerId            = "1",
                organizerName      = "Centro Cultural UQ",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            ),

            Event(
                id = "10",
                title = "Campaña de Donación de Ropa",
                description = "Dona ropa en buen estado para familias del barrio Brasilia.",
                category = Category.VOLUNTARIADO,
                imageUris = listOf(
                    "https://images.unsplash.com/photo-1593113630400-ea4288922497?q=80&w=800",
                    "https://images.unsplash.com/photo-1578358371191-20320622ff44?q=80&w=800",
                    "https://images.unsplash.com/photo-1544027993-37dbfe43562a?q=80&w=800"
                ),
                eventLocation = EventLocation(4.5450, -75.6850),
                startDate = "2026-06-18 09:00",
                endDate   = "2026-06-18 16:00",
                maxAttendees     = null,
                currentAttendees = 15,
                ownerId            = "3",
                organizerName      = "Cruz Roja Quindío",
                eventStatus        = EventStatus.ACTIVE,
                verificationStatus = VerificationStatus.APPROVED
            )
        )
    }

    override suspend fun getEventsByIds(ids: List<String>): List<Event> =
        _events.value.filter { it.id in ids }
}