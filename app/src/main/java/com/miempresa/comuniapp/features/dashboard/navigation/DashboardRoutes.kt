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

    // Rutas de Administración
    @Serializable
    data object HomeAdmin : DashboardRoutes()

    @Serializable
    data class ManagePublications(val filter: String = "ALL") : DashboardRoutes()

    @Serializable
    data class PublicationDetail(val eventId: String) : DashboardRoutes()

    @Serializable
    data object ModerationHistory : DashboardRoutes()
}