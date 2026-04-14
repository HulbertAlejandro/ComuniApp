package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.miempresa.comuniapp.features.event.detail.EventDetailScreen
import com.miempresa.comuniapp.features.event.list.EventListScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen

@Composable
fun AdminNavigation(
    navController: NavHostController,
    padding: PaddingValues
) {

    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.EventList
    ) {

        composable<DashboardRoutes.EventList> {
            EventListScreen(
                paddingValues = padding,
                onEventClick = { eventId ->
                    navController.navigate(DashboardRoutes.EventDetail(eventId))
                }
            )
        }

        composable<DashboardRoutes.EventDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<DashboardRoutes.EventDetail>()

            EventDetailScreen(
                eventId = args.eventId,
                paddingValues = padding,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                paddingValues = padding,
                onLogout = { /* opcional manejar */ },
                onEditProfile = TODO(),
                viewModel = TODO()
            )
        }
        
    }
}