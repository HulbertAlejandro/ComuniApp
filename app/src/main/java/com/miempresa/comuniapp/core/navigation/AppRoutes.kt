package com.miempresa.comuniapp.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoutes {
    @Serializable
    data object Home : AppRoutes()
    
    @Serializable
    data object Login : AppRoutes()
    
    @Serializable
    data object Register : AppRoutes()
    
    @Serializable
    data object ForgotPassword : AppRoutes()
    
    @Serializable
    data object ResetPassword : AppRoutes()
    
    @Serializable
    data object Dashboard : AppRoutes()
}
