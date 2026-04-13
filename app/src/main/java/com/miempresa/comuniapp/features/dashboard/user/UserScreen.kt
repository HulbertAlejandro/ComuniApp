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
    var showTopBar by remember { mutableStateOf(true) }
    var showBottomBar by remember { mutableStateOf(true) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val destination = backStackEntry.destination
            
            // La Home (EventList) y CreateEvent manejan su propia TopAppBar según el diseño
            showTopBar = !destination.hasRoute<DashboardRoutes.EventList>() &&
                         !destination.hasRoute<DashboardRoutes.CreateEvent>() &&
                         !destination.hasRoute<DashboardRoutes.UserDetail>() &&
                         !destination.hasRoute<DashboardRoutes.EventDetail>()
            
            // Ocultar barra inferior en pantallas de detalle o creación
            showBottomBar = !destination.hasRoute<DashboardRoutes.UserDetail>() &&
                            !destination.hasRoute<DashboardRoutes.EventDetail>() &&
                            !destination.hasRoute<DashboardRoutes.CreateEvent>()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = title,
                    logout = onLogout
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
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
