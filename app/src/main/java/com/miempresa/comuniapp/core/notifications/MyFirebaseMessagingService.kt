package com.miempresa.comuniapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.miempresa.comuniapp.MainActivity
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.repository.NotificationRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio que intercepta los mensajes de Firebase Cloud Messaging (FCM).
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var auth: FirebaseAuth

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID          = "comuniapp_channel"
        const val CHANNEL_NAME        = "ComuniApp"
        const val CHANNEL_DESCRIPTION = "Notificaciones de eventos y actividad de ComuniApp"
    }

    /**
     * Llamado cuando la app está en primer plano y llega un mensaje FCM.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val titulo = message.notification?.title
            ?: message.data["title"]
            ?: "ComuniApp"

        val cuerpo = message.notification?.body
            ?: message.data["body"]
            ?: ""

        // ✅ Campos extraídos con las llaves correctas en inglés sincronizadas con el backend
        val relatedEventId = message.data["relatedEventId"] ?: ""
        val type = message.data["type"] ?: "GENERAL"

        mostrarNotificacionNativa(
            titulo = titulo,
            cuerpo = cuerpo,
            relatedEventId = relatedEventId,
            type = type
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM", "Token renovado: $token")

        val userId = auth.currentUser?.uid ?: run {
            android.util.Log.d("FCM", "Token renovado sin sesión activa. Se actualizará al login.")
            return
        }

        serviceScope.launch {
            runCatching {
                userRepository.updateFcmToken(userId, token)
            }.onFailure { e ->
                android.util.Log.e("FCM", "Error al actualizar token FCM: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private fun mostrarNotificacionNativa(
        titulo: String,
        cuerpo: String,
        relatedEventId: String,
        type: String
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(canal)
        }

        // ✅ Se añade tanto el ID del evento como el tipo de notificación al Intent de navegación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("relatedEventId", relatedEventId)
            putExtra("type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notificacion)
    }

    private fun parseTipo(tipo: String?): NotificationType =
        try {
            NotificationType.valueOf(tipo ?: "GENERAL")
        } catch (e: IllegalArgumentException) {
            NotificationType.GENERAL
        }
}