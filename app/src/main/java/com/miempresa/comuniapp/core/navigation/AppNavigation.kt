package com.miempresa.comuniapp.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.miempresa.comuniapp.data.model.UserSession
import com.miempresa.comuniapp.domain.model.UserRole
import com.miempresa.comuniapp.features.dashboard.user.UserScreen
import com.miempresa.comuniapp.features.dashboard.admin.AdminScreen
import com.miempresa.comuniapp.features.home.HomeScreen
import com.miempresa.comuniapp.features.login.LoginScreen
import com.miempresa.comuniapp.features.password.ForgetPasswordScreen
import com.miempresa.comuniapp.features.password.ResetPasswordScreen
import com.miempresa.comuniapp.features.register.RegisterScreen

@Composable
fun AppNavigation(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = sessionState) {

            is SessionState.Loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SessionState.NotAuthenticated -> AuthNavigation()

            is SessionState.Authenticated -> MainNavigation(
                session = state.session,
                onLogout = sessionViewModel::logout
            )
        }
    }
}

@Composable
private fun AuthNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainRoutes.Home
    ) {

        composable<MainRoutes.Home> {
            HomeScreen(
                onLoginClick = {
                    navController.navigate(MainRoutes.Login)
                }
            )
        }

        composable<MainRoutes.Login> {
            LoginScreen(
                onRegisterClick = {
                    navController.navigate(MainRoutes.Register)
                },
                onForgotPasswordClick = {
                    navController.navigate(MainRoutes.ForgotPassword)
                }
            )
        }

        composable<MainRoutes.Register> {
            RegisterScreen(
                onNavigateToBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<MainRoutes.ForgotPassword> {
            ForgetPasswordScreen(
                onNavigateToReset = {
                    navController.navigate(MainRoutes.ResetPassword)
                }
            )
        }

        composable<MainRoutes.ResetPassword> {
            ResetPasswordScreen(
                onPasswordResetSuccess = {
                    // Vuelve al login limpio
                    navController.navigate(MainRoutes.Login) {
                        popUpTo(MainRoutes.Home) { inclusive = false }
                    }
                }
            )
        }
    }
}

@Composable
private fun MainNavigation(
    session: UserSession,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    val startDestination: Any = when (session.role) {
        UserRole.MODERATOR -> MainRoutes.HomeAdmin
        UserRole.USER -> MainRoutes.HomeUser
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable<MainRoutes.HomeUser> {
            UserScreen(
                onLogout = onLogout
            )
        }

        composable<MainRoutes.HomeAdmin> {
            AdminScreen(
                onLogout = onLogout
            )
        }
    }
}