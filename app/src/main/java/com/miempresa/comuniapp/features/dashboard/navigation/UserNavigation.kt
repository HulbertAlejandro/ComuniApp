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
import com.miempresa.comuniapp.features.event.edit.EditEventScreen
import com.miempresa.comuniapp.features.event.list.EventListScreen
import com.miempresa.comuniapp.features.map.MapScreen
import com.miempresa.comuniapp.features.user.achievements.AchievementsScreen
import com.miempresa.comuniapp.features.user.edit.UserEditScreen
import com.miempresa.comuniapp.features.user.history.HistoryScreen
import com.miempresa.comuniapp.features.user.myevents.MyEventsScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen
import com.miempresa.comuniapp.features.user.savedevents.SavedEventsScreen

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
            MapScreen(
                paddingValues = padding,
                onEventClick  = { eventId ->
                    // ✅ Navega al detalle del evento al presionar la card
                    navController.navigate(DashboardRoutes.EventDetail(eventId))
                }
            )
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
                },
                onMyEvents = { navController.navigate(DashboardRoutes.MyEvents) },
                onSavedEvents = { navController.navigate(DashboardRoutes.SavedEvents) },
                onAchievements = { navController.navigate(DashboardRoutes.Achievements) },
                onHistory = { navController.navigate(DashboardRoutes.History) }
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

        composable<DashboardRoutes.Achievements> {
            AchievementsScreen(
                paddingValues = padding
            )
        }

        composable<DashboardRoutes.MyEvents> {
            MyEventsScreen(
                paddingValues = padding,
                onEventClick = { eventId ->
                    navController.navigate(DashboardRoutes.EventDetail(eventId))
                },
                onEditEvent = { eventId ->
                    navController.navigate(DashboardRoutes.EditEvent(eventId))
                }
            )
        }

        composable<DashboardRoutes.SavedEvents> {
            SavedEventsScreen(
                paddingValues = padding,
                onEventClick = { eventId ->
                    navController.navigate(DashboardRoutes.EventDetail(eventId))
                }
            )
        }

        composable<DashboardRoutes.EditEvent> { backStackEntry ->
            val args = backStackEntry.toRoute<DashboardRoutes.EditEvent>()

            EditEventScreen(
                eventId = args.eventId,
                onBack = {
                    // Esto se ejecutará tanto si el usuario cancela,
                    // como si el evento se actualiza o se elimina con éxito.
                    navController.popBackStack()
                }
            )
        }

        composable<DashboardRoutes.History> {
            HistoryScreen(
                paddingValues = padding,
                onEventClick = { eventId ->
                    navController.navigate(DashboardRoutes.EventDetail(eventId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}