package com.miempresa.comuniapp.features.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.components.ConfirmDialog
import com.miempresa.comuniapp.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

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
                value = viewModel.name.value,
                onValueChange = { viewModel.name.onChange(it) },
                label = "Nombre",
                icon = Icons.Default.Person,
                error = viewModel.name.error
            )

            AppTextField(
                value = viewModel.phone.value,
                onValueChange = { viewModel.phone.onChange(it) },
                label = "Teléfono",
                icon = Icons.Default.Person,
                error = viewModel.phone.error
            )

            AppTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.onChange(it) },
                label = "Email",
                icon = Icons.Default.Email,
                error = viewModel.email.error
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

            // ── Categorías favoritas (opcional) ──────────────────────────────────────
            val selectedCategories by viewModel.selectedCategories.collectAsState()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Categorías favoritas (opcional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // FlowRow de chips de categorías
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.entries.forEach { category ->
                    val isSelected = selectedCategories.contains(category)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleCategory(category) },
                        label = {
                            Text(
                                text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF000000),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFE0E0E0),
                            labelColor = Color(0xFF212121)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    Text("Registrarse")
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