package com.miempresa.comuniapp.features.dashboard.navigation

import kotlinx.serialization.Serializable

sealed class DashboardRoutes {

    // ── Rutas de usuario ──────────────────────────────────────────────────────
    @Serializable data object EventList    : DashboardRoutes()
    @Serializable data object Map          : DashboardRoutes()
    @Serializable data object CreateEvent  : DashboardRoutes()
    @Serializable data object Notifications: DashboardRoutes()
    @Serializable data object Profile      : DashboardRoutes()
    @Serializable data class  EventDetail(val eventId: String) : DashboardRoutes()
    @Serializable data object UserEdit     : DashboardRoutes()
    @Serializable data object MyEvents     : DashboardRoutes()
    @Serializable data object SavedEvents  : DashboardRoutes()
    @Serializable data object Achievements : DashboardRoutes()
    @Serializable data class  EditEvent(val eventId: String) : DashboardRoutes()
    @Serializable data object History      : DashboardRoutes()

    // ── Rutas administrativas ─────────────────────────────────────────────────
    @Serializable data object AdminDashboard     : DashboardRoutes()
    @Serializable data object ManagePublications : DashboardRoutes()
    @Serializable data object ModerationHistory  : DashboardRoutes()
    @Serializable data class  AdminEventDetail(val eventId: String) : DashboardRoutes()
}