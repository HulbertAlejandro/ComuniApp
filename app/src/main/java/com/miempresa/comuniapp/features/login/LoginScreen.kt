package com.miempresa.comuniapp.features.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

/**
 * Pantalla de inicio de sesión.
 *
 * Observa el estado [LoginViewModel.loginResult] para:
 * - Mostrar un [CircularProgressIndicator] mientras la operación está en curso.
 * - Navegar hacia el home al recibir [RequestResult.Success].
 * - Mostrar un Snackbar de error al recibir [RequestResult.Failure].
 *
 * @param onLoginSuccess        Callback que ejecuta la navegación al home.
 * @param onRegisterClick       Callback hacia la pantalla de registro.
 * @param onForgotPasswordClick Callback hacia la pantalla de recuperación.
 * @param viewModel             ViewModel inyectado por Hilt.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val loginResult by viewModel.loginResult.collectAsState()

    /**
     * Reacciona a cada cambio en [loginResult]:
     * - Loading: no interrumpe con Snackbar; el botón ya muestra el spinner.
     * - Success: navega al home y luego limpia el estado.
     * - Failure: muestra el mensaje de error en el Snackbar y limpia el estado.
     */
    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is RequestResult.Success -> {
                onLoginSuccess()
                viewModel.resetLoginResult()
            }
            is RequestResult.Failure -> {
                snackbarHostState.showSnackbar(result.errorMessage)
                viewModel.resetLoginResult()
            }
            // Loading y null no requieren acción aquí;
            // el indicador visual se maneja directamente en el botón.
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
                painter = painterResource(id = R.drawable.logo_comunidad),
                contentDescription = stringResource(R.string.home_logo_description),
                modifier = Modifier.size(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(40.dp))

            AppTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.onChange(it) },
                label = stringResource(R.string.login_email_label),
                icon = Icons.Default.Email,
                error = viewModel.email.error
            )

            AppPasswordField(
                value = viewModel.password.value,
                onValueChange = { viewModel.password.onChange(it) },
                label = stringResource(R.string.login_password_label),
                icon = Icons.Default.Lock,
                error = viewModel.password.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            /**
             * El botón se deshabilita durante la carga para evitar
             * múltiples llamadas simultáneas al repositorio.
             * Mientras [loginResult] es [RequestResult.Loading],
             * muestra un [CircularProgressIndicator] en lugar del texto.
             */
            Button(
                onClick = { viewModel.login() },
                enabled = viewModel.isFormValid && loginResult !is RequestResult.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors()
            ) {
                if (loginResult is RequestResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onForgotPasswordClick) {
                Text(stringResource(R.string.login_forgot_password))
            }

            TextButton(onClick = onRegisterClick) {
                Text(stringResource(R.string.login_no_account))
            }
        }
    }
}