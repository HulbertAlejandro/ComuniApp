package com.miempresa.comuniapp.features.admin.publications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.notifications.NotificationSender
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.domain.model.Badge
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.model.ReputationPoints
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class PublicationFilter { ALL, PENDING, APPROVED, REJECTED }

/**
 * ViewModel del panel de gestión de publicaciones del administrador.
 *
 * Responsabilidades:
 * - Filtrar y exponer la lista de eventos según su estado de verificación.
 * - Ejecutar las acciones de moderación: aprobar, rechazar, finalizar.
 * - Otorgar puntos de reputación e insignias al organizador tras la aprobación.
 * - Enviar notificaciones push al organizador tras cada acción de moderación.
 * - Evaluar y otorgar la insignia "Estrella del Mes".
 *
 * ── Por qué NotificationSender aquí y no en el repositorio ───────────────────
 * El repositorio solo conoce operaciones CRUD. La decisión de "notificar al
 * organizador cuando se aprueba un evento" es lógica de negocio/presentación
 * que pertenece al ViewModel. El repositorio no debe conocer a NotificationSender.
 *
 * @param eventRepository      Repositorio de eventos (Firestore).
 * @param userRepository       Repositorio de usuarios (Firestore).
 * @param notificationSender   Componente de solicitud de envío de push.
 * @param resources            Proveedor de strings localizados.
 */
@HiltViewModel
class ManagePublicationsViewModel @Inject constructor(
    private val eventRepository    : EventRepository,
    private val userRepository     : UserRepository,
    private val notificationSender : NotificationSender,
    private val resources          : ResourceProvider
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val organizersMap: StateFlow<Map<String, String>> =
        userRepository.users
            .map { users -> users.associate { it.id to it.name } }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyMap()
            )
    private val _activeFilter = MutableStateFlow(PublicationFilter.ALL)
    val activeFilter: StateFlow<PublicationFilter> = _activeFilter.asStateFlow()

    val filteredPublications: StateFlow<List<Event>> =
        kotlinx.coroutines.flow.combine(
            eventRepository.events,
            _activeFilter
        ) { events, filter ->
            when (filter) {
                PublicationFilter.ALL      -> events
                PublicationFilter.PENDING  -> events.filter {
                    it.verificationStatus == VerificationStatus.PENDING
                }
                PublicationFilter.APPROVED -> events.filter {
                    it.verificationStatus == VerificationStatus.APPROVED &&
                            it.eventStatus        != EventStatus.FINISHED
                }
                PublicationFilter.REJECTED -> events.filter {
                    it.verificationStatus == VerificationStatus.REJECTED
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    fun onFilterSelected(filter: PublicationFilter) {
        _activeFilter.value = filter
    }

    // ── Aprobar ───────────────────────────────────────────────────────────────

    /**
     * Aprueba un evento y notifica al organizador.
     *
     * Orden de operaciones:
     * 1. Usa [EventRepository.approveEvent] (no update directo) para respetar
     *    el método específico del repositorio que también registra moderationDate.
     * 2. Suma puntos de reputación al organizador.
     * 3. Evalúa si el organizador merece insignias.
     * 4. Envía notificación push al organizador.
     *
     * Los pasos 2, 3 y 4 son silenciosos con [runCatching]: si fallan,
     * la aprobación ya ocurrió y no debe revertirse.
     *
     * @param eventId ID del evento a aprobar.
     */
    fun approveEvent(eventId: String) {
        viewModelScope.launch {
            val event = eventRepository.findById(eventId) ?: return@launch

            // Solo si estaba pendiente para no duplicar puntos
            if (event.verificationStatus != VerificationStatus.PENDING) return@launch

            // 1. Aprueba en Firestore via método específico del repositorio
            runCatching {
                eventRepository.approveEvent(eventId)
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error al aprobar: ${e.message}")
                return@launch
            }

            val ownerId = event.ownerId

            // 2. Reputación (silencioso)
            runCatching {
                userRepository.addPoints(ownerId, ReputationPoints.EVENT_APPROVED)
                userRepository.updateLevel(ownerId)
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error en reputación: ${e.message}")
            }

            // 3. Insignias (silencioso)
            runCatching {
                evaluarInsigniasAprobacion(ownerId)
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error en insignias: ${e.message}")
            }

            // 4. Notificación push al organizador (silencioso)
            runCatching {
                notificationSender.enviar(
                    destinatarioId = ownerId,
                    tipo           = NotificationType.EVENT_APPROVED,
                    titulo         = "🎉 Evento aprobado",
                    cuerpo         = "\"${event.title}\" ya es visible para la comunidad.",
                    relatedEventId = eventId
                )
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error al notificar aprobación: ${e.message}")
            }
        }
    }

    // ── Rechazar ──────────────────────────────────────────────────────────────

    /**
     * Rechaza un evento y notifica al organizador con el motivo.
     *
     * Usa [EventRepository.rejectEvent] para registrar el motivo y
     * la fecha de moderación de forma atómica.
     *
     * @param eventId ID del evento a rechazar.
     * @param reason  Motivo de rechazo visible al organizador.
     */
    fun rejectEvent(eventId: String, reason: String) {
        if (reason.isBlank()) return

        viewModelScope.launch {
            val event = eventRepository.findById(eventId) ?: return@launch

            // 1. Rechazar en Firestore via método específico del repositorio
            runCatching {
                eventRepository.rejectEvent(eventId, reason)
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error al rechazar: ${e.message}")
                return@launch
            }

            // 2. Notificación push al organizador (silencioso)
            runCatching {
                notificationSender.enviar(
                    destinatarioId = event.ownerId,
                    tipo           = NotificationType.EVENT_REJECTED,
                    titulo         = "❌ Evento rechazado",
                    cuerpo         = "\"${event.title}\" no fue aprobado. Motivo: $reason",
                    relatedEventId = eventId,
                    extraData      = mapOf("motivo" to reason)
                )
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error al notificar rechazo: ${e.message}")
            }
        }
    }

    // ── Finalizar ─────────────────────────────────────────────────────────────

    /**
     * Marca un evento como finalizado por acción del moderador y envía
     * una notificación push al usuario creador (organizador).
     *
     * @param eventId ID del evento a dar por concluido.
     */
    fun finishEvent(eventId: String) {
        viewModelScope.launch {
            // Buscamos el evento para conocer el título y el ID del organizador destino
            val event = eventRepository.findById(eventId) ?: return@launch

            // 1. Finalizar en Firestore
            runCatching {
                eventRepository.markAsFinished(eventId)
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error al finalizar evento: ${e.message}")
                return@launch
            }

            // 2. Notificación push al organizador (silencioso)
            runCatching {
                // Nota: Si en tu enum "NotificationType" no existe un tipo específico como
                // EVENT_FINISHED, puedes reutilizar otro o mapearlo según corresponda.
                notificationSender.enviar(
                    destinatarioId = event.ownerId,
                    tipo           = NotificationType.EVENT_APPROVED,
                    titulo         = "🏁 Evento finalizado por moderación",
                    cuerpo         = "El moderador ha dado por concluido tu evento: \"${event.title}\".",
                    relatedEventId = eventId
                )
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error al notificar finalización: ${e.message}")
            }
        }
    }

    // ── Estrella del Mes ──────────────────────────────────────────────────────

    /**
     * Premia al creador del evento aprobado con más intereses en el mes.
     * Debe llamarse manualmente desde la UI del admin o desde un job programado.
     */
    fun grantStarOfTheMonth() {
        viewModelScope.launch {
            val topEvent = eventRepository.events.value
                .filter  { it.verificationStatus == VerificationStatus.APPROVED }
                .maxByOrNull { it.interestCount }
                ?: return@launch

            if (topEvent.interestCount == 0) return@launch

            val badgeId = "badge_star_${System.currentTimeMillis()}"
            runCatching {
                userRepository.addBadge(
                    topEvent.ownerId,
                    Badge(
                        id          = badgeId,
                        name        = resources.getString(R.string.badge_star_of_the_month_name),
                        description = resources.getFormattedString(
                            R.string.badge_star_of_the_month_description,
                            topEvent.title
                        ),
                        achievedAt = System.currentTimeMillis()
                    )
                )
                userRepository.addPoints(topEvent.ownerId, 50)
                userRepository.updateLevel(topEvent.ownerId)
            }.onFailure { e ->
                android.util.Log.e("ManagePublications", "Error en Estrella del Mes: ${e.message}")
            }
        }
    }

    // ── Evaluación de insignias ───────────────────────────────────────────────

    /**
     * Evalúa y otorga insignias al organizador basándose en el número
     * de eventos aprobados acumulados.
     *
     * Se ejecuta después de cada aprobación. Es silenciosa: si falla,
     * no afecta el flujo principal de aprobación.
     *
     * @param ownerId ID del organizador a evaluar.
     */
    private suspend fun evaluarInsigniasAprobacion(ownerId: String) {
        val user = userRepository.findById(ownerId) ?: return

        val approvedCount = eventRepository.events.value.count { event ->
            event.ownerId            == ownerId &&
                    event.verificationStatus == VerificationStatus.APPROVED
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