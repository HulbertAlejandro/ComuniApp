package com.miempresa.comuniapp.features.dashboard.navigation

import kotlinx.serialization.Serializable

sealed class DashboardRoutes {

    @Serializable
    data object EventList : DashboardRoutes()

    @Serializable
    data object Map : DashboardRoutes()

    @Serializable
    data object CreateEvent : DashboardRoutes()

    @Serializable
    data object Notifications : DashboardRoutes()

    @Serializable
    data object Profile : DashboardRoutes()

    @Serializable
    data class EventDetail(val eventId: String) : DashboardRoutes()

    // Pantalla de edición de perfil
    @Serializable
    data object UserEdit : DashboardRoutes()

    // Nuevas pantallas del perfil
    @Serializable
    data object MyEvents : DashboardRoutes()

    @Serializable
    data object SavedEvents : DashboardRoutes()

    @Serializable
    data object Achievements : DashboardRoutes()

    // Pantalla de edición de evento
    @Serializable
    data class EditEvent(val eventId: String) : DashboardRoutes()
}