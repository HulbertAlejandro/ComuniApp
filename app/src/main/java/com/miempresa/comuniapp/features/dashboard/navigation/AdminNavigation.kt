package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.miempresa.comuniapp.features.admin.dashboard.AdminDashboardScreen
import com.miempresa.comuniapp.features.admin.history.ModerationHistoryScreen
import com.miempresa.comuniapp.features.admin.publications.ManagePublicationsScreen
import com.miempresa.comuniapp.features.event.detail.EventDetailScreen
import com.miempresa.comuniapp.features.event.list.EventListScreen
import com.miempresa.comuniapp.features.user.achievements.AchievementsScreen
import com.miempresa.comuniapp.features.user.edit.UserEditScreen
import com.miempresa.comuniapp.features.user.myevents.MyEventsScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen
import com.miempresa.comuniapp.features.user.savedevents.SavedEventsScreen

@Composable
fun AdminNavigation(
    navController : NavHostController,
    padding       : PaddingValues,
    onLogout      : () -> Unit
) {
    // ✅ Extraemos solo el padding inferior (altura de la BottomBar).
    // Las pantallas con Scaffold propio necesitan este valor para que
    // su LazyColumn no quede tapado por la barra de navegación inferior.
    val bottomPadding = PaddingValues(bottom = padding.calculateBottomPadding())

    NavHost(
        navController    = navController,
        startDestination = DashboardRoutes.AdminDashboard
    ) {
        composable<DashboardRoutes.AdminDashboard> {
            AdminDashboardScreen(
                paddingValues = padding           // recibe el padding completo
            )
        }

        composable<DashboardRoutes.ManagePublications> {
            ManagePublicationsScreen(
                onNavigateBack  = { navController.popBackStack() },
                onViewDetail    = { eventId ->
                    navController.navigate(DashboardRoutes.AdminEventDetail(eventId))
                },
                bottomPadding   = bottomPadding   // ← solo el bottom para el LazyColumn
            )
        }

        composable<DashboardRoutes.ModerationHistory> {
            ModerationHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                bottomPadding  = bottomPadding    // ← solo el bottom para el LazyColumn
            )
        }

        composable<DashboardRoutes.AdminEventDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<DashboardRoutes.AdminEventDetail>()
            EventDetailScreen(
                eventId        = args.eventId,
                paddingValues  = PaddingValues(),  // tiene su propio Scaffold
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DashboardRoutes.EventList> {
            EventListScreen(
                paddingValues = padding,
                onEventClick  = { eventId ->
                    navController.navigate(DashboardRoutes.EventDetail(eventId))
                }
            )
        }

        composable<DashboardRoutes.EventDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<DashboardRoutes.EventDetail>()
            EventDetailScreen(
                eventId        = args.eventId,
                paddingValues  = PaddingValues(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                paddingValues  = padding,
                onLogout       = onLogout,
                isAdmin        = true,      // ← admin ve perfil simplificado
                onEditProfile  = { navController.navigate(DashboardRoutes.UserEdit)},
                onMyEvents     = { },
                onSavedEvents  = { },
                onAchievements = { },
                onHistory      = { }
            )
        }

        composable<DashboardRoutes.MyEvents> {
            MyEventsScreen(
                paddingValues = padding,
                onEventClick  = { navController.navigate(DashboardRoutes.EventDetail(it)) },
                onEditEvent   = { }
            )
        }

        composable<DashboardRoutes.SavedEvents> {
            SavedEventsScreen(
                paddingValues = padding,
                onEventClick  = { navController.navigate(DashboardRoutes.EventDetail(it)) }
            )
        }

        composable<DashboardRoutes.Achievements> {
            AchievementsScreen(paddingValues = padding)
        }

        // Asegúrate de tener esto también en AdminNavigation.kt
        composable<DashboardRoutes.UserEdit> {
            UserEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}