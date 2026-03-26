package com.miempresa.comuniapp.features.password

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

@Composable
fun ResetPasswordScreen(
    onPasswordResetSuccess: () -> Unit = {},
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewModel.result.collectAsState()

    LaunchedEffect(result) {
        result?.let {
            val message = when (it) {
                is RequestResult.Success -> it.message
                is RequestResult.Failure -> it.errorMessage
            }

            snackbarHostState.showSnackbar(message)

            if (it is RequestResult.Success) {
                onPasswordResetSuccess()
            }

            viewModel.resetResult()
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
                contentDescription = "Logo",
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Nueva Contraseña",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(30.dp))

            AppPasswordField(
                value = viewModel.newPassword.value,
                onValueChange = { viewModel.newPassword.onChange(it) },
                label = "Nueva contraseña",
                error = viewModel.newPassword.error
            )

            AppPasswordField(
                value = viewModel.confirmPassword.value,
                onValueChange = { viewModel.confirmPassword.onChange(it) },
                label = "Confirmar contraseña",
                error = viewModel.confirmPassword.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.resetPassword() },
                enabled = viewModel.isFormValid,
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text(
                    "Actualizar contraseña",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}