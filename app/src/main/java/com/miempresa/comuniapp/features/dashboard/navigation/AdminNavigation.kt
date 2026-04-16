package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.miempresa.comuniapp.features.user.edit.UserEditScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen
import com.miempresa.comuniapp.features.dashboard.admin.AdminScreen
import com.miempresa.comuniapp.features.dashboard.admin.history.ModerationHistoryScreen
import com.miempresa.comuniapp.features.dashboard.admin.publications.ManagePublicationsScreen
import com.miempresa.comuniapp.features.dashboard.admin.publications.detail.AdminEventDetailScreen

@Composable
fun AdminNavigation(
    navController: NavHostController,
    padding: PaddingValues,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.HomeAdmin,
        modifier = Modifier.padding(padding)
    ) {
        composable<DashboardRoutes.HomeAdmin> {
            AdminScreen(
                onLogout = onLogout,
                onManagePublications = { filter ->
                    navController.navigate(DashboardRoutes.ManagePublications(filter))
                },
                onModerationHistory = {
                    navController.navigate(DashboardRoutes.ModerationHistory)
                },
                onNavigateToProfile = {
                    navController.navigate(DashboardRoutes.Profile)
                }
            )
        }

        composable<DashboardRoutes.ManagePublications> { backStackEntry ->
            val route: DashboardRoutes.ManagePublications = backStackEntry.toRoute()
            ManagePublicationsScreen(
                initialFilter = route.filter,
                onNavigateBack = { navController.popBackStack() },
                onViewDetail = { eventId ->
                    navController.navigate(DashboardRoutes.PublicationDetail(eventId))
                }
            )
        }

        composable<DashboardRoutes.PublicationDetail> { backStackEntry ->
            val route: DashboardRoutes.PublicationDetail = backStackEntry.toRoute()
            AdminEventDetailScreen(
                eventId = route.eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DashboardRoutes.ModerationHistory> {
            ModerationHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                paddingValues = PaddingValues(),
                onLogout = onLogout,
                onEditProfile = {
                    navController.navigate(DashboardRoutes.UserEdit)
                }
            )
        }

        composable<DashboardRoutes.UserEdit> {
            UserEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}