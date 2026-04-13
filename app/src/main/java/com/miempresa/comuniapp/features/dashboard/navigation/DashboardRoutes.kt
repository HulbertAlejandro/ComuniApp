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

    @Serializable
    data object Search : DashboardRoutes()

    @Serializable
    data class UserDetail(val userId: String) : DashboardRoutes()

}
