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
        distanceKm(latitude, longitude, it.location.latitude, it.location.longitude) <= radiusKm
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

    // =============================
    // Seed
    // =============================
    //
    // Usuario 1 (Juan) está en: 4.6097, -74.0817  ← referencia para el filtro
    //
    // DENTRO del radio (≤ 5 km) → aparecen con "Cerca de mí":
    //   id1  Torneo Fútbol          4.6097, -74.0817   ~0.0 km  (mismo barrio)
    //   id2  Yoga al Aire Libre     4.6120, -74.0840   ~0.3 km  (barrio contiguo)
    //   id5  Taller Kotlin          4.6080, -74.0800   ~0.2 km  (a pocas cuadras)
    //
    // FUERA del radio (> 5 km) → ocultos con "Cerca de mí":
    //   id3  Feria Gastronómica     4.6600, -74.0550   ~8.5 km  (norte lejano)
    //   id4  Jornada Limpieza       4.5500, -74.1200  ~12.0 km  (suroccidente)
    //   id6  Ciclopaseo Nocturno    4.7000, -74.1100  ~11.5 km  (norte)
    //   id7  Charla Emprendimiento  4.5300, -74.0700  ~9.1 km   (sur)
    //   id8  Feria Emprendedores    4.7400, -74.0600  ~15.2 km  (norte lejano)
    //   id9  Cine al Aire Libre     4.4900, -74.1300  ~17.0 km  (suroeste)
    //   id10 Campaña Donación Ropa  4.7800, -74.1500  ~23.0 km  (noroeste)
    //
    private fun seedEvents(): List<Event> {
        return listOf(

            // ── DENTRO (~0 km) ─────────────────────────────────────────────
            Event(
                id = "1",
                title = "Torneo de Fútbol Comunitario",
                description = "Participa en nuestro torneo local y gana premios.",
                category = Category.DEPORTES,
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=800&auto=format&fit=crop",
                location = Location(4.6097, -74.0817),   // mismo punto que Usuario 1
                startDate = "2026-05-25 08:00",
                endDate   = "2026-05-25 17:00",
                maxAttendees     = 50,
                currentAttendees = 32,
                ownerId             = "1",
                eventStatus         = EventStatus.CREATED,
                verificationStatus  = VerificationStatus.PENDING
            ),

            // ── DENTRO (~0.3 km) ───────────────────────────────────────────
            Event(
                id = "2",
                title = "Clase de Yoga al Aire Libre",
                description = "Relájate y conecta con la naturaleza.",
                category = Category.DEPORTES,
                imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=800&auto=format&fit=crop",
                location = Location(4.6120, -74.0840),   // ~0.3 km al noroccidente
                startDate = "2026-05-26 07:00",
                endDate   = "2026-05-26 09:00",
                maxAttendees     = 20,
                currentAttendees = 20,
                ownerId             = "1",
                eventStatus         = EventStatus.FULL,
                verificationStatus  = VerificationStatus.APPROVED
            ),

            // ── FUERA (~8.5 km) ────────────────────────────────────────────
            Event(
                id = "3",
                title = "Feria Gastronómica",
                description = "Comida típica, música y cultura local.",
                category = Category.CULTURA,
                imageUrl = "https://images.unsplash.com/photo-1533777857889-4be7c70b33f7?q=80&w=800&auto=format&fit=crop",
                location = Location(4.6600, -74.0550),   // ~8.5 km al norte
                startDate = "2026-05-31 10:00",
                endDate   = "2026-06-01 20:00",
                maxAttendees     = 200,
                currentAttendees = 85,
                ownerId             = "1",
                eventStatus         = EventStatus.FINISHED,
                verificationStatus  = VerificationStatus.APPROVED
            ),

            // ── FUERA (~12 km) ─────────────────────────────────────────────
            Event(
                id = "4",
                title = "Jornada de Limpieza del Parque",
                description = "Ayúdanos a cuidar nuestro entorno.",
                category = Category.VOLUNTARIADO,
                imageUrl = "https://images.unsplash.com/photo-1509099836639-18ba1795216d?q=80&w=800&auto=format&fit=crop",
                location = Location(4.5500, -74.1200),   // ~12 km al suroccidente
                startDate = "2026-06-02 08:00",
                endDate   = "2026-06-02 12:00",
                maxAttendees     = null,
                currentAttendees = 10,
                ownerId             = "4",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.REJECTED,
                rejectionReason     = "Evento duplicado"
            ),

            // ── DENTRO (~0.2 km) ───────────────────────────────────────────
            Event(
                id = "5",
                title = "Taller de Programación Kotlin",
                description = "Aprende desarrollo Android desde cero.",
                category = Category.ACADEMICO,
                imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=800&auto=format&fit=crop",
                location = Location(4.6080, -74.0800),   // ~0.2 km al suroriente
                startDate = "2026-06-05 18:00",
                endDate   = "2026-06-05 21:00",
                maxAttendees     = 30,
                currentAttendees = 12,
                ownerId             = "2",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.APPROVED
            ),

            // ── FUERA (~11.5 km) ───────────────────────────────────────────
            Event(
                id = "6",
                title = "Ciclopaseo Nocturno",
                description = "Recorrido en bicicleta por la ciudad.",
                category = Category.DEPORTES,
                imageUrl = "https://images.unsplash.com/photo-1508973378895-8d1f2d4e94c6?q=80&w=800&auto=format&fit=crop",
                location = Location(4.7000, -74.1100),   // ~11.5 km al norte
                startDate = "2026-06-07 19:00",
                endDate   = "2026-06-07 22:00",
                maxAttendees     = 100,
                currentAttendees = 60,
                ownerId             = "1",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.APPROVED
            ),

            // ── FUERA (~9.1 km) ────────────────────────────────────────────
            Event(
                id = "7",
                title = "Charla de Emprendimiento",
                description = "Aprende a crear tu propio negocio.",
                category = Category.ACADEMICO,
                imageUrl = "https://images.unsplash.com/photo-1552664730-d307ca884978?q=80&w=800&auto=format&fit=crop",
                location = Location(4.5300, -74.0700),   // ~9.1 km al sur
                startDate = "2026-06-10 17:00",
                endDate   = "2026-06-10 20:00",
                maxAttendees     = 40,
                currentAttendees = 25,
                ownerId             = "3",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.PENDING
            ),

            // ── FUERA (~15.2 km) ───────────────────────────────────────────
            Event(
                id = "8",
                title = "Feria de Emprendedores Locales",
                description = "Apoya negocios locales y productos artesanales.",
                category = Category.SOCIAL,
                imageUrl = "https://images.unsplash.com/photo-1521334884684-d80222895322?q=80&w=800&auto=format&fit=crop",
                location = Location(4.7400, -74.0600),   // ~15.2 km al norte
                startDate = "2026-06-12 09:00",
                endDate   = "2026-06-12 18:00",
                maxAttendees     = 150,
                currentAttendees = 90,
                ownerId             = "2",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.APPROVED
            ),

            // ── FUERA (~17 km) ─────────────────────────────────────────────
            Event(
                id = "9",
                title = "Cine Comunitario al Aire Libre",
                description = "Película gratuita para toda la familia.",
                category = Category.CULTURA,
                imageUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=800&auto=format&fit=crop",
                location = Location(4.4900, -74.1300),   // ~17 km al suroeste
                startDate = "2026-06-15 19:00",
                endDate   = "2026-06-15 22:00",
                maxAttendees     = 80,
                currentAttendees = 40,
                ownerId             = "1",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.APPROVED
            ),

            // ── FUERA (~23 km) ─────────────────────────────────────────────
            Event(
                id = "10",
                title = "Campaña de Donación de Ropa",
                description = "Dona ropa para familias necesitadas.",
                category = Category.VOLUNTARIADO,
                imageUrl = "https://images.unsplash.com/photo-1593113630400-ea4288922497?q=80&w=800&auto=format&fit=crop",
                location = Location(4.7800, -74.1500),   // ~23 km al noroeste
                startDate = "2026-06-18 09:00",
                endDate   = "2026-06-18 16:00",
                maxAttendees     = null,
                currentAttendees = 15,
                ownerId             = "3",
                eventStatus         = EventStatus.ACTIVE,
                verificationStatus  = VerificationStatus.PENDING
            )
        )
    }

    override suspend fun getEventsByIds(ids: List<String>): List<Event> =
        _events.value.filter { it.id in ids }
}