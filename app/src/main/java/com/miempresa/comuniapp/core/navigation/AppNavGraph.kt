package com.miempresa.comuniapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.miempresa.comuniapp.features.dashboard.UserScreen
import com.miempresa.comuniapp.features.home.HomeScreen
import com.miempresa.comuniapp.features.login.LoginScreen
import com.miempresa.comuniapp.features.password.ForgetPasswordScreen
import com.miempresa.comuniapp.features.password.ResetPasswordScreen
import com.miempresa.comuniapp.features.register.RegisterScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home
    ) {

        composable<AppRoutes.Home> {
            HomeScreen(
                onLoginClick = {
                    navController.navigate(AppRoutes.Login)
                }
            )
        }

        composable<AppRoutes.Login> {
            LoginScreen(
                onRegisterClick = {
                    navController.navigate(AppRoutes.Register)
                },
                onForgotPasswordClick = {
                    navController.navigate(AppRoutes.ForgotPassword)
                },
                onLoginSuccess = {
                    navController.navigate(AppRoutes.Dashboard) {
                        popUpTo(AppRoutes.Home) { inclusive = true }
                    }
                }
            )
        }

        composable<AppRoutes.Register> {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AppRoutes.ForgotPassword> {
            ForgetPasswordScreen(
                onNavigateToReset = {
                    navController.navigate(AppRoutes.ResetPassword)
                }
            )
        }

        composable<AppRoutes.ResetPassword> {
            ResetPasswordScreen(
                onPasswordResetSuccess = {
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(AppRoutes.Login) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<AppRoutes.Dashboard> {
            UserScreen(
                onLogout = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Home) { inclusive = true }
                    }
                },
                onEditProfile = {
                    // TODO: Navigate to edit profile screen
                    // For now, this can be empty or show a toast
                }
            )
        }
    }
}
