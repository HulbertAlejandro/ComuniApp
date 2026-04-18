package com.miempresa.comuniapp.core.navigation

import kotlinx.serialization.Serializable

sealed class MainRoutes {
    @Serializable data object Home           : MainRoutes()
    @Serializable data object Login          : MainRoutes()
    @Serializable data object Register       : MainRoutes()
    @Serializable data object ForgotPassword : MainRoutes()
    @Serializable data object ResetPassword  : MainRoutes()
}