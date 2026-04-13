package com.miempresa.comuniapp.features.dashboard.admin

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.miempresa.comuniapp.features.dashboard.components.BottomNavigationBar
import com.miempresa.comuniapp.features.dashboard.components.TopAppBar
import com.miempresa.comuniapp.features.dashboard.navigation.AdminNavigation
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes

@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {

    val navController = rememberNavController()
    var title by remember { mutableStateOf("Panel Admin") }
    var showBars by remember { mutableStateOf(true) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val destination = backStackEntry.destination

            // Oculta barras en detalles (igual que usuario)
            showBars = !destination.hasRoute<DashboardRoutes.EventDetail>() &&
                    !destination.hasRoute<DashboardRoutes.UserDetail>()
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

        AdminNavigation(
            navController = navController,
            padding = padding
        )

    }
}