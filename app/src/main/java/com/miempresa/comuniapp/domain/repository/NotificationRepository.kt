package com.miempresa.comuniapp.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

interface NotificationRepository {

    val notifications: StateFlow<List<Notification>>

    suspend fun sendNotification(notification: Notification)

    suspend fun getNotificationsByUser(userId: String): List<Notification>

    suspend fun markAsRead(notificationId: String)
}