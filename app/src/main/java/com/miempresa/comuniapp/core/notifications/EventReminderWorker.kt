package com.miempresa.comuniapp.core.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Worker que envía recordatorios de eventos próximos.
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
        const val WORK_NAME = "event_reminder_worker"
        private const val VENTANA_HORAS = 24L
    }

    override suspend fun doWork(): Result {

        Log.d("EventReminder", "Worker iniciado")

        return try {

            val ahora = System.currentTimeMillis()
            val en24Horas = ahora + (VENTANA_HORAS * 60 * 60 * 1000)

            val formatoFecha = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            )

            /**
             * IMPORTANTE:
             * Usamos first() sobre el Flow del repositorio para forzar
             * una lectura real desde Firestore.
             */
            val eventosAprobados = eventRepository
                .getEventsByVerificationStatus(VerificationStatus.APPROVED)
                .first()
                .filter { evento ->
                    evento.eventStatus == EventStatus.ACTIVE
                }

            val eventosProximos = eventosAprobados.filter { evento ->

                runCatching {

                    val fechaInicio = formatoFecha
                        .parse(evento.startDate)
                        ?.time ?: 0L

                    fechaInicio in (ahora + 1)..en24Horas

                }.getOrDefault(false)
            }

            Log.d(
                "EventReminder",
                "Eventos proximos encontrados: ${eventosProximos.size}"
            )

            eventosProximos.forEach { evento ->

                runCatching {

                    val asistentes = attendanceRepository
                        .getAttendanceByEvent(evento.id)
                        .filter { asistencia ->
                            asistencia.status == AttendanceStatus.CONFIRMED
                        }

                    asistentes.forEach { asistencia ->

                        notificationSender.enviar(
                            destinatarioId = asistencia.userId,
                            tipo = NotificationType.EVENT_REMINDER,
                            titulo = "⏰ Recordatorio",
                            cuerpo = "\"${evento.title}\" comienza pronto.",
                            relatedEventId = evento.id
                        )
                    }

                    Log.d(
                        "EventReminder",
                        "Recordatorios enviados para evento: ${evento.id}"
                    )

                }.onFailure { e ->

                    Log.e(
                        "EventReminder",
                        "Error procesando evento ${evento.id}: ${e.message}"
                    )
                }
            }

            Result.success()

        } catch (e: Exception) {

            Log.e(
                "EventReminder",
                "Error general en worker: ${e.message}"
            )

            Result.retry()
        }
    }
}