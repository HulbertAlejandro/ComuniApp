package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AdminNavigation(
    navController: NavHostController,
    padding: PaddingValues
) {

    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.HomeAdmin
    ) {

    }

}