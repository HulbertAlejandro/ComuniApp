package com.miempresa.comuniapp.features.dashboard.admin

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.features.dashboard.components.BottomNavigationBar
import com.miempresa.comuniapp.features.dashboard.components.Destination
import com.miempresa.comuniapp.features.dashboard.components.TopAppBar
import com.miempresa.comuniapp.features.dashboard.navigation.AdminNavigation
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes

@Composable
fun AdminScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val defaultTitle = stringResource(R.string.admin_dashboard_title)
    var title         by remember { mutableStateOf(defaultTitle) }
    var showBars      by remember { mutableStateOf(true) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val dest = backStackEntry.destination

            // Ocultar barras en pantallas de detalle (tienen su propio TopAppBar)
            showBars = !dest.hasRoute<DashboardRoutes.AdminEventDetail>() &&
                    !dest.hasRoute<DashboardRoutes.EventDetail>()
        }
    }

    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title      = title,
                    showLogout = true,
                    onLogout   = onLogout
                )
            }
        },
        bottomBar = {
            if (showBars) {
                BottomNavigationBar(
                    navController = navController,
                    titleTopBar   = { title = it },
                    items         = Destination.adminItems  // ✅ barra exclusiva del admin
                )
            }
        }
    ) { padding ->
        AdminNavigation(
            navController = navController,
            padding       = padding,
            onLogout      = onLogout
        )
    }
}