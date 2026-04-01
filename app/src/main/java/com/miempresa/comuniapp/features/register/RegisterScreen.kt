package com.miempresa.comuniapp.features.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.components.ConfirmDialog
import com.miempresa.comuniapp.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    onNavigateToBack: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val registerResult by viewModel.registerResult.collectAsState()

    LaunchedEffect(registerResult) {
        registerResult?.let { result ->

            val message = when (result) {
                is RequestResult.Success -> result.message
                is RequestResult.Failure -> result.errorMessage
                is RequestResult.Loading -> "Registrando usuario..."
            }

            snackbarHostState.showSnackbar(message)

            if (result is RequestResult.Success) {
                delay(1000)
                onNavigateToBack()
            }

            viewModel.resetRegisterResult()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Crear Cuenta",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(30.dp))

            AppTextField(
                viewModel.name.value,
                { viewModel.name.onChange(it) },
                "Nombre",
                Icons.Default.Person,
                viewModel.name.error
            )

            AppTextField(
                viewModel.city.value,
                { viewModel.city.onChange(it) },
                "Ciudad",
                Icons.Default.LocationOn,
                viewModel.city.error
            )

            AppTextField(
                viewModel.address.value,
                { viewModel.address.onChange(it) },
                "Dirección",
                Icons.Default.Home,
                viewModel.address.error
            )

            AppTextField(
                viewModel.email.value,
                { viewModel.email.onChange(it) },
                "Email",
                Icons.Default.Email,
                viewModel.email.error
            )

            AppPasswordField(
                value = viewModel.password.value,
                onValueChange = { viewModel.password.onChange(it) },
                label = "Contraseña",
                icon = Icons.Default.Lock,
                error = viewModel.password.error
            )

            AppPasswordField(
                value = viewModel.confirmPassword.value,
                onValueChange = { viewModel.confirmPassword.onChange(it) },
                label = "Confirmar contraseña",
                icon = Icons.Default.Lock,
                error = viewModel.confirmPassword.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.register() },
                enabled = viewModel.isFormValid,
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {

                if (registerResult is RequestResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Registrarse",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToBack) {
                Text("Volver")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showExitDialog) {
        ConfirmDialog(
            title = "¿Está seguro de salir?",
            text = "Si sale, perderá todos los datos del formulario.",
            onDismiss = { showExitDialog = false },
            onConfirm = {
                viewModel.resetForm()
                onNavigateToBack()
            }
        )
    }
}