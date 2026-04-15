package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.miempresa.comuniapp.features.dashboard.admin.AdminScreen

@Composable
fun AdminNavigation(
    navController: NavHostController,
    padding: PaddingValues,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.HomeAdmin
    ) {
        composable<DashboardRoutes.HomeAdmin> {
            AdminScreen(
                onLogout = onLogout,
                onManagePublications = {
                    // TODO: navController.navigate(DashboardRoutes.ManagePublications)
                },
                onModerationHistory = {
                    // TODO: navController.navigate(DashboardRoutes.ModerationHistory)
                }
            )
        }
    }
}