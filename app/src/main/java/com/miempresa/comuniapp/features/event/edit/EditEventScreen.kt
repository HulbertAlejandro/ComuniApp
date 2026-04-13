package com.miempresa.comuniapp.features.event.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.ui.components.AppTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    eventId: String,
    onSuccess: () -> Unit,
    viewModel: EditEventViewModel = hiltViewModel()
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewModel.result.collectAsState()

    // Cargar evento
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    // Manejo de resultado
    LaunchedEffect(result) {
        result?.let {
            val msg = when (it) {
                is RequestResult.Success -> it.message
                is RequestResult.Failure -> it.errorMessage
                is RequestResult.Loading -> "Actualizando..."
            }

            snackbarHostState.showSnackbar(msg)

            if (it is RequestResult.Success) {
                onSuccess()
            }

            viewModel.resetResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AppTextField(
                value = viewModel.title.value,
                onValueChange = { viewModel.title.onChange(it) },
                label = "Título",
                error = viewModel.title.error
            )

            AppTextField(
                value = viewModel.description.value,
                onValueChange = { viewModel.description.onChange(it) },
                label = "Descripción",
                error = viewModel.description.error
            )

            // Categoría simple
            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = {}
            ) {
                OutlinedTextField(
                    value = viewModel.category.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppTextField(
                value = viewModel.maxAttendees,
                onValueChange = { viewModel.maxAttendees = it },
                label = "Máx asistentes (opcional)",
                error = null
            )

            Button(
                onClick = { viewModel.updateEvent() },
                enabled = viewModel.isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar cambios")
            }
        }
    }
}