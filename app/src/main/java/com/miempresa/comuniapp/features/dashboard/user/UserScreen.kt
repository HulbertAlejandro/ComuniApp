package com.miempresa.comuniapp.features.dashboard.user

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.miempresa.comuniapp.features.dashboard.components.BottomNavigationBar
import com.miempresa.comuniapp.features.dashboard.components.TopAppBar
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes
import com.miempresa.comuniapp.features.dashboard.navigation.UserNavigation

@Composable
fun UserScreen(
    onLogout: () -> Unit
) {

    val navController = rememberNavController()
    var title by remember { mutableStateOf("Inicio usuario") }
    var showBars by remember { mutableStateOf(true) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val destination = backStackEntry.destination
            // Ocultar barras en el detalle de usuario o detalle de evento
            showBars = !destination.hasRoute<DashboardRoutes.UserDetail>() &&
                       !destination.hasRoute<DashboardRoutes.EventDetail>()
        }
    }

    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = title,
                    logout = onLogout
                )
            }
        },
        bottomBar = {
            if (showBars) {
                BottomNavigationBar(
                    navController = navController,
                    titleTopBar = {
                        title = it
                    }
                )
            }
        }
    ) { padding ->

        UserNavigation(
            navController = navController,
            padding = padding,
            onLogout = onLogout
        )

    }

}
