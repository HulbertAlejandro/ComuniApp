package com.miempresa.comuniapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
        startDestination = AppRoutes.HOME
    ) {

        composable(AppRoutes.HOME) {
            HomeScreen(
                onLoginClick = {
                    navController.navigate(AppRoutes.LOGIN)
                }
            )
        }

        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onRegisterClick = {
                    navController.navigate(AppRoutes.REGISTER)
                },
                onForgotPasswordClick = {
                    navController.navigate(AppRoutes.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    // luego aquí limpiamos stack
                }
            )
        }

        composable(AppRoutes.REGISTER) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoutes.FORGOT_PASSWORD) {
            ForgetPasswordScreen(
                onNavigateToReset = {
                    navController.navigate(AppRoutes.RESET_PASSWORD)
                }
            )
        }

        composable(AppRoutes.RESET_PASSWORD) {
            ResetPasswordScreen(
                onPasswordResetSuccess = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}