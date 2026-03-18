package com.miempresa.comuniapp.features.dashboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
            // La pantalla principal de la sección de usuarios que muestra la lista de usuarios
            UserListScreen(
                onUserClick = {
                    navController.navigate(DashboardRoutes.UserDetail(it))
                }
            )
        }

        composable<DashboardRoutes.Search> {
            SearchScreen(
                onUserClick = {
                    navController.navigate(DashboardRoutes.UserDetail(it))
                }
            ) // Debe crear este composable en el paquete user/search
        }

        composable<DashboardRoutes.Profile> {
            ProfileScreen(
                onEditProfile = onEditProfile,
                onLogout = onLogout
            ) // Debe crear este composable en el paquete user/profile
        }

        composable<DashboardRoutes.UserDetail> {
            val args = it.toRoute<DashboardRoutes.UserDetail>()
            UserDetailScreen(
                userId = args.userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
    
}
