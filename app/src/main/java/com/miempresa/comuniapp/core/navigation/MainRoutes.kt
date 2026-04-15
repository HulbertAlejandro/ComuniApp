package com.miempresa.comuniapp.core.navigation

import kotlinx.serialization.Serializable

sealed class MainRoutes {

    @Serializable
    data object Home : MainRoutes()

    @Serializable
    data object Login : MainRoutes()

    @Serializable
    data object Register : MainRoutes()


    @Serializable
    data object ForgotPassword : MainRoutes()

    @Serializable
    data object ResetPassword : MainRoutes()

    @Serializable
    data object HomeUser : MainRoutes()

    @Serializable
    data object HomeAdmin : MainRoutes()

    @Serializable
    data object Profile : MainRoutes()

    @Serializable
    data class ManagePublications(val filter: String = "ALL") : MainRoutes()

    @Serializable
    data class EventDetail(val id: String) : MainRoutes()

    @Serializable
    data object ModerationHistory : MainRoutes()
}