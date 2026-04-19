package com.miempresa.comuniapp.features.admin.publications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.domain.model.Badge
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.ReputationPoints
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class PublicationFilter { ALL, PENDING, APPROVED, REJECTED }

@HiltViewModel
class ManagePublicationsViewModel @Inject constructor(
    private val eventRepository : EventRepository,
    private val userRepository  : UserRepository,
    private val resources       : ResourceProvider
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val organizersMap: StateFlow<Map<String, String>> =
        userRepository.users
            .map { users -> users.associate { it.id to it.name } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _activeFilter = MutableStateFlow(PublicationFilter.ALL)
    val activeFilter: StateFlow<PublicationFilter> = _activeFilter.asStateFlow()

    val filteredPublications: StateFlow<List<Event>> = combine(
        eventRepository.events,
        _activeFilter
    ) { events, filter ->
        when (filter) {
            PublicationFilter.ALL      -> events

            PublicationFilter.PENDING  -> events.filter {
                it.verificationStatus == VerificationStatus.PENDING
            }

            // ✅ APPROVED solo muestra eventos activos o con cupo lleno.
            // FINISHED se excluye — ya no requieren gestión del admin.
            PublicationFilter.APPROVED -> events.filter {
                it.verificationStatus == VerificationStatus.APPROVED &&
                        it.eventStatus        != EventStatus.FINISHED
            }

            PublicationFilter.REJECTED -> events.filter {
                it.verificationStatus == VerificationStatus.REJECTED
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun onFilterSelected(filter: PublicationFilter) {
        _activeFilter.value = filter
    }

    // ── Aprobar ───────────────────────────────────────────────────────────────

    fun approveEvent(eventId: String) {
        viewModelScope.launch {
            val today = LocalDate.now().format(dateFormatter)
            val event = eventRepository.findById(eventId) ?: return@launch

            // Solo aprobar si estaba pendiente para no duplicar puntos
            if (event.verificationStatus != VerificationStatus.PENDING) return@launch

            // 1. Actualizar estado del evento
            eventRepository.update(
                event.copy(
                    verificationStatus = VerificationStatus.APPROVED,
                    eventStatus        = EventStatus.ACTIVE,
                    moderationDate     = today
                )
            )

            // 2. Sumar puntos al creador
            val ownerId = event.ownerId
            userRepository.addPoints(ownerId, ReputationPoints.EVENT_APPROVED)
            userRepository.updateLevel(ownerId)

            // 3. Evaluar insignias
            grantBadgesOnApproval(ownerId)
        }
    }

    // ── Rechazar ──────────────────────────────────────────────────────────────

    fun rejectEvent(eventId: String, reason: String) {
        if (reason.isBlank()) return
        viewModelScope.launch {
            val today = LocalDate.now().format(dateFormatter)
            val event = eventRepository.findById(eventId) ?: return@launch
            eventRepository.update(
                event.copy(
                    verificationStatus = VerificationStatus.REJECTED,
                    rejectionReason    = reason,
                    moderationDate     = today
                )
            )
        }
    }

    // ── Finalizar ─────────────────────────────────────────────────────────────

    fun finishEvent(eventId: String) {
        viewModelScope.launch {
            // markAsFinished solo cambia eventStatus a FINISHED,
            // el verificationStatus queda APPROVED — correcto para el historial
            eventRepository.markAsFinished(eventId)
        }
    }

    // ── Estrella del Mes ──────────────────────────────────────────────────────

    /**
     * Llamar al finalizar el mes (manualmente o desde un job programado).
     * Premia al creador del evento APPROVED con más "Me interesa".
     */
    fun grantStarOfTheMonth() {
        viewModelScope.launch {
            val topEvent = eventRepository.events.value
                .filter  { it.verificationStatus == VerificationStatus.APPROVED }
                .maxByOrNull { it.interestCount }
                ?: return@launch

            if (topEvent.interestCount == 0) return@launch

            val badgeId = "badge_star_${System.currentTimeMillis()}"
            userRepository.addBadge(
                topEvent.ownerId,
                Badge(
                    id          = badgeId,
                    name        = resources.getString(R.string.badge_star_of_the_month_name),
                    description = resources.getFormattedString(R.string.badge_star_of_the_month_description, topEvent.title),
                    achievedAt  = System.currentTimeMillis()
                )
            )
            userRepository.addPoints(topEvent.ownerId, 50)
            userRepository.updateLevel(topEvent.ownerId)
        }
    }

    // ── Lógica de insignias ───────────────────────────────────────────────────

    /**
     * El ViewModel tiene acceso a ambos repositorios, por lo que calcula el
     * conteo de aprobados aquí y evita la dependencia circular
     * UserRepository → EventRepository.
     */
    private suspend fun grantBadgesOnApproval(ownerId: String) {
        val user = userRepository.findById(ownerId) ?: return

        // Contamos directamente desde el StateFlow — no necesitamos suspend call extra
        val approvedCount = eventRepository.events.value.count { event ->
            event.ownerId              == ownerId &&
                    event.verificationStatus   == VerificationStatus.APPROVED
        }

        val existingBadgeIds = user.reputation.badges.map { it.id }.toSet()

        // Insignia "Pionero" — primer evento aprobado
        if (approvedCount == 1 && "badge_pionero" !in existingBadgeIds) {
            userRepository.addBadge(
                ownerId,
                Badge(
                    id          = "badge_pionero",
                    name        = resources.getString(R.string.badge_pionero_name),
                    description = resources.getString(R.string.badge_pionero_description),
                    achievedAt  = System.currentTimeMillis()
                )
            )
        }

        // Insignia "Constante" — 10 eventos aprobados
        if (approvedCount >= 10 && "badge_constante" !in existingBadgeIds) {
            userRepository.addBadge(
                ownerId,
                Badge(
                    id          = "badge_constante",
                    name        = resources.getString(R.string.badge_constante_name),
                    description = resources.getString(R.string.badge_constante_description),
                    achievedAt  = System.currentTimeMillis()
                )
            )
        }
    }
}