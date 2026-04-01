package com.miempresa.comuniapp.features.dashboard.navigation

import kotlinx.serialization.Serializable

sealed class DashboardRoutes {

    @Serializable
    data object EventList : DashboardRoutes()

    @Serializable
    data object Search : DashboardRoutes()

    @Serializable
    data object Profile : DashboardRoutes()

    @Serializable
    data class UserDetail(val userId: String) : DashboardRoutes()

    @Serializable
    data class EventDetail(val eventId: String) : DashboardRoutes()
    
    // Antiguas para compatibilidad si es necesario borrar luego
    @Serializable
    data object HomeUser : DashboardRoutes()

    @Serializable
    data object HomeAdmin : DashboardRoutes()
}
