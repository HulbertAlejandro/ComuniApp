package com.miempresa.comuniapp.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Worker que busca eventos próximos (dentro de las próximas 24 horas)
 * y envía recordatorios push a los usuarios que confirmaron asistencia.
 *
 * Se programa para ejecutarse una vez al día usando WorkManager.
 * Si el dispositivo está apagado o sin conexión en el momento programado,
 * WorkManager lo reintenta automáticamente cuando sea posible.
 *
 * ── @HiltWorker ──────────────────────────────────────────────────────────────
 * Permite inyección de dependencias en Workers con Hilt.
 * Requiere agregar HiltWorkerFactory en el Application o en el módulo de Hilt.
 *
 * @param context             Contexto de Android.
 * @param params              Parámetros del Worker.
 * @param eventRepository     Para obtener eventos próximos aprobados.
 * @param attendanceRepository Para obtener los asistentes confirmados.
 * @param notificationSender  Para solicitar el envío de las notificaciones.
 */
@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
    private val notificationSender: NotificationSender
) : CoroutineWorker(context, params) {

    companion object {
        /** Nombre único del trabajo para evitar duplicados en WorkManager. */
        const val WORK_NAME = "event_reminder_worker"

        /** Ventana de tiempo para considerar un evento como "próximo" (24 horas). */
        private const val VENTANA_HORAS = 24L
    }

    /**
     * Lógica principal del Worker.
     *
     * Busca eventos aprobados y activos que comiencen en las próximas 24 horas.
     * Para cada uno, obtiene los asistentes confirmados y les envía un recordatorio.
     *
     * @return [Result.success] siempre; los errores individuales son silenciosos
     *         para no reintentar todo el trabajo si falla un evento específico.
     */
    override suspend fun doWork(): Result {
        android.util.Log.d("EventReminder", "Worker iniciado: buscando eventos próximos")

        val ahora        = System.currentTimeMillis()
        val en24Horas    = ahora + (VENTANA_HORAS * 60 * 60 * 1000)
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        try {
            // Obtiene todos los eventos del StateFlow local
            val eventosAprobados = eventRepository.events.value.filter { evento ->
                evento.verificationStatus == VerificationStatus.APPROVED &&
                        evento.eventStatus == EventStatus.ACTIVE
            }

            // Filtra los que comienzan en las próximas 24 horas
            val eventosPróximos = eventosAprobados.filter { evento ->
                try {
                    val fechaInicio = formatoFecha.parse(evento.startDate)?.time ?: 0L
                    fechaInicio in (ahora + 1)..en24Horas
                } catch (e: Exception) {
                    false
                }
            }

            android.util.Log.d(
                "EventReminder",
                "Eventos próximos encontrados: ${eventosPróximos.size}"
            )

            // Para cada evento próximo, notifica a sus asistentes confirmados
            eventosPróximos.forEach { evento ->
                runCatching {
                    val asistentes = attendanceRepository.getAttendanceByEvent(evento.id)
                    asistentes.forEach { asistencia ->
                        notificationSender.enviar(
                            destinatarioId = asistencia.userId,
                            tipo           = NotificationType.EVENT_REMINDER,
                            titulo         = "⏰ Recordatorio de evento",
                            cuerpo         = "\"${evento.title}\" comienza mañana. ¡No lo olvides!",
                            relatedEventId = evento.id
                        )
                    }
                }.onFailure { e ->
                    android.util.Log.e(
                        "EventReminder",
                        "Error al procesar recordatorio para ${evento.id}: ${e.message}"
                    )
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("EventReminder", "Error general en Worker: ${e.message}")
        }

        return Result.success()
    }
}