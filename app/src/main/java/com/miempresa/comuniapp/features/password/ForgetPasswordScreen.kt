package com.miempresa.comuniapp.features.password

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

/**
 * Pantalla de "¿Olvidaste tu contraseña?".
 *
 * Permite al usuario ingresar su email para recibir instrucciones de recuperación.
 *
 * Manejo de estados:
 * - [RequestResult.Loading]: spinner en el botón, botón deshabilitado.
 * - [RequestResult.Success]: Snackbar con mensaje y navegación al login.
 * - [RequestResult.Failure]: Snackbar con error, sin navegación.
 *
 * @param onNavigateToBack Callback para volver a la pantalla de login.
 * @param viewModel        ViewModel inyectado por Hilt.
 */
@Composable
fun ForgetPasswordScreen(
    onNavigateToBack: () -> Unit = {},
    viewModel: ForgetPasswordViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewModel.result.collectAsState()

    /**
     * Reacciona a cada cambio en [result]:
     * - [RequestResult.Success]: muestra el mensaje y navega al reset.
     * - [RequestResult.Failure]: muestra el error en Snackbar.
     * - [RequestResult.Loading]: no interrumpe con Snackbar; el botón lo gestiona.
     */
    LaunchedEffect(result) {
        when (val r = result) {
            is RequestResult.Success -> {
                snackbarHostState.showSnackbar(r.message)
                viewModel.resetResult()
                onNavigateToBack()  // Vuelve al login, no a ResetPassword
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
                painter = painterResource(id = R.drawable.logo_comunidad),
                contentDescription = stringResource(R.string.home_logo_description),
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.password_forget_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(30.dp))

            AppTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.onChange(it) },
                label = stringResource(R.string.password_forget_email_label),
                icon = Icons.Default.Email,
                error = viewModel.email.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            /**
             * Botón de verificación:
             * - Se deshabilita si el formulario no es válido o si hay una operación en curso.
             * - Muestra [CircularProgressIndicator] mientras [result] es [RequestResult.Loading].
             */
            Button(
                onClick = { viewModel.sendRecoveryEmail() },
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
                    Text(stringResource(R.string.password_forget_button))
                }
            }
        }
    }
}