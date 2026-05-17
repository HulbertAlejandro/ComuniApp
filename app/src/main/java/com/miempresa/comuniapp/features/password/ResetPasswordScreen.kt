package com.miempresa.comuniapp.features.password

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

/**
 * Pantalla de restablecimiento de contraseña.
 *
 * Recibe el email verificado en la pantalla anterior y permite al usuario
 * definir y confirmar una nueva contraseña.
 *
 * Manejo de estados:
 * - [RequestResult.Loading]: spinner en el botón, botón deshabilitado.
 * - [RequestResult.Success]: Snackbar de confirmación y navegación al login.
 * - [RequestResult.Failure]: Snackbar de error, sin navegación.
 *
 * @param emailVerificado      Email confirmado en [ForgetPasswordScreen].
 * @param onPasswordResetSuccess Callback hacia la pantalla de login.
 * @param viewModel            ViewModel inyectado por Hilt.
 */
@Composable
fun ResetPasswordScreen(
    emailVerificado: String = "",                  // ✅ Recibe el email de la pantalla anterior
    onPasswordResetSuccess: () -> Unit = {},
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewModel.result.collectAsState()

    /**
     * Asigna el email verificado al ViewModel en cuanto la pantalla se compone.
     * Esto conecta el flujo de ForgetPassword → ResetPassword sin un ViewModel compartido.
     */
    LaunchedEffect(Unit) {
        viewModel.email = emailVerificado
    }

    /**
     * Reacciona a cada cambio en [result]:
     * - [RequestResult.Success]: muestra confirmación y navega al login.
     * - [RequestResult.Failure]: muestra el error en Snackbar.
     * - [RequestResult.Loading]: el botón gestiona el indicador visual.
     */
    LaunchedEffect(result) {
        when (val r = result) {
            is RequestResult.Success -> {
                snackbarHostState.showSnackbar(r.message)
                viewModel.resetResult()
                onPasswordResetSuccess()
            }
            is RequestResult.Failure -> {
                snackbarHostState.showSnackbar(r.errorMessage)
                viewModel.resetResult()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(R.drawable.logo_comunidad),
                contentDescription = stringResource(R.string.home_logo_description),
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.password_reset_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(30.dp))

            AppPasswordField(
                value = viewModel.newPassword.value,
                onValueChange = { viewModel.newPassword.onChange(it) },
                label = stringResource(R.string.password_reset_new_password_label),
                error = viewModel.newPassword.error
            )

            AppPasswordField(
                value = viewModel.confirmPassword.value,
                onValueChange = { viewModel.confirmPassword.onChange(it) },
                label = stringResource(R.string.password_reset_confirm_password_label),
                error = viewModel.confirmPassword.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            /**
             * Botón de confirmación:
             * - Se deshabilita si el formulario no es válido o si hay operación en curso.
             * - Muestra [CircularProgressIndicator] durante [RequestResult.Loading].
             */
            Button(
                onClick = { viewModel.resetPassword() },
                enabled = viewModel.isFormValid && result !is RequestResult.Loading,
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                if (result is RequestResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.password_reset_button))
                }
            }
        }
    }
}