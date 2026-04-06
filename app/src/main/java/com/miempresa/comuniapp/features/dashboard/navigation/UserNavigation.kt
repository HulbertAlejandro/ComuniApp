package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.miempresa.comuniapp.features.event.detail.EventDetailScreen
import com.miempresa.comuniapp.features.event.list.EventListScreen
import com.miempresa.comuniapp.features.user.detail.UserDetailScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen
import com.miempresa.comuniapp.features.user.search.SearchScreen

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
                    navController.navigate(DashboardRoutes.EventDetail(eventId = eventId))
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

        composable<DashboardRoutes.Search> {
            SearchScreen(
                paddingValues = padding,
                onUserClick = { userId ->
                    navController.navigate(DashboardRoutes.UserDetail(userId = userId))
                }
            )
        }

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                paddingValues = padding,
                onLogout = onLogout
            )
        }

        composable<DashboardRoutes.UserDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<DashboardRoutes.UserDetail>()
            UserDetailScreen(
                userId = args.userId,
                paddingValues = padding,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
