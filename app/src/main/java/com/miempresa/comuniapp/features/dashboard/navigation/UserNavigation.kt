package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.miempresa.comuniapp.features.report.detail.ReportDetailScreen
import com.miempresa.comuniapp.features.report.list.ReportListScreen
import com.miempresa.comuniapp.features.user.detail.UserDetailScreen
import com.miempresa.comuniapp.features.user.list.UserListScreen
import com.miempresa.comuniapp.features.user.profile.ProfileScreen
import com.miempresa.comuniapp.features.user.search.SearchScreen

@Composable
fun UserNavigation(
    navController: NavHostController,
    padding: PaddingValues,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
){
    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.HomeUser
    ) {

        composable<DashboardRoutes.HomeUser> {
            UserListScreen(
                paddingValues = padding,
                onUserClick = { userId ->
                    navController.navigate(DashboardRoutes.UserDetail(userId))
                }
            )
        }

        composable<DashboardRoutes.Search> {
            SearchScreen(
                paddingValues = padding,
                onUserClick = { userId ->
                    navController.navigate(DashboardRoutes.UserDetail(userId))
                }
            )
        }

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                paddingValues = padding,
                onEditProfile = onEditProfile,
                onLogout = onLogout
            )
        }

        composable<DashboardRoutes.UserDetail> {
            val args = it.toRoute<DashboardRoutes.UserDetail>()

            UserDetailScreen(
                userId = args.userId,
                paddingValues = padding,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<DashboardRoutes.ReportList> {
            ReportListScreen(
                paddingValues = padding,
                onReportClick = { reportId ->
                    navController.navigate(DashboardRoutes.ReportDetail(reportId))
                }
            )
        }

        composable<DashboardRoutes.ReportDetail> {
            val args = it.toRoute<DashboardRoutes.ReportDetail>()

            ReportDetailScreen(
                reportId = args.reportId,
                paddingValues = padding,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}