package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.miempresa.comuniapp.features.event.create.CreateEventScreen
import com.miempresa.comuniapp.features.event.detail.EventDetailScreen
import com.miempresa.comuniapp.features.event.list.EventListScreen
import com.miempresa.comuniapp.features.user.edit.UserEditScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen

@Composable
fun UserNavigation(
    navController: NavHostController,
    padding: PaddingValues,
    onLogout: () -> Unit
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

        composable<DashboardRoutes.Map> {
            Text("Mapa (pendiente)")
        }

        composable<DashboardRoutes.CreateEvent> {
            CreateEventScreen(
                onBack = { navController.popBackStack() },
                onEventCreated = { navController.popBackStack() }
            )
        }

        composable<DashboardRoutes.Notifications> {
            Text("Notificaciones (pendiente)")
        }

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                paddingValues = padding,
                onLogout = onLogout,
                onEditProfile = {
                    navController.navigate(DashboardRoutes.UserEdit)
                }
            )
        }

        composable<DashboardRoutes.EventDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<DashboardRoutes.EventDetail>()
            EventDetailScreen(
                eventId = args.eventId,
                paddingValues = padding,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DashboardRoutes.UserEdit> {
            UserEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}