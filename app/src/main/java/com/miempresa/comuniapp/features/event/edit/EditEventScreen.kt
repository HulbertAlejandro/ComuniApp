package com.miempresa.comuniapp.features.event.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.features.event.create.*
import com.miempresa.comuniapp.core.component.MapBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EditEventViewModel = hiltViewModel()
) {
    val result by viewModel.result.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados para diálogos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageUrlDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingForStart by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    val initialPoint = remember(eventId) { viewModel.initialMapPoint }

    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    LaunchedEffect(result) {
        result?.let {
            if (it is RequestResult.Success) {
                snackbarHostState.showSnackbar(it.message)
                onBack()
            } else if (it is RequestResult.Failure) {
                snackbarHostState.showSnackbar(it.errorMessage)
            }
            viewModel.resetResult()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_event_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.edit_event_back_button_description))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F7F7),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECCIÓN 1: IMAGEN
            SectionCard(title = stringResource(R.string.edit_event_image_section)) {
                AsyncImage(
                    model = viewModel.imageUrl.value.ifBlank { null },
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentScale = ContentScale.Crop
                )
                Button(
                    onClick = { showImageUrlDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color.Black)
                ) { Text(stringResource(R.string.edit_event_image_edit_button)) }
            }

            // SECCIÓN 2: INFORMACIÓN
            SectionCard(title = stringResource(R.string.edit_event_details_section)) {
                LabelText(stringResource(R.string.edit_event_title_label))
                CustomTextField(viewModel.title.value, { viewModel.title.onChange(it) }, stringResource(R.string.edit_event_title_placeholder))

                Spacer(Modifier.height(12.dp))
                LabelText(stringResource(R.string.edit_event_category_label))
                OutlinedCard(onClick = { showCategoryDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(viewModel.category.name, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                Spacer(Modifier.height(12.dp))
                LabelText(stringResource(R.string.edit_event_description_label))
                CustomTextField(viewModel.description.value, { viewModel.description.onChange(it) }, stringResource(R.string.edit_event_description_placeholder), false, 4)
            }

            // SECCIÓN 3: FECHA Y HORA
            SectionCard(title = stringResource(R.string.edit_event_datetime_section)) {
                DateTimeRow(stringResource(R.string.edit_event_start_label), viewModel.startDateMillis,
                    { pickingForStart = true; showDatePicker = true },
                    { pickingForStart = true; showTimePicker = true })
                Divider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                DateTimeRow(stringResource(R.string.edit_event_end_label), viewModel.endDateMillis,
                    { pickingForStart = false; showDatePicker = true },
                    { pickingForStart = false; showTimePicker = true })
            }

            // SECCIÓN 4: UBICACIÓN — reemplazar el bloque completo ──────────────

            SectionCard(title = stringResource(R.string.edit_event_location_section)) {

                MapBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    activateClick        = true,
                    showMyLocationButton = true,
                    // ✅ Marcador precargado con la ubicación actual del evento
                    initialPoint         = initialPoint,
                    onMapClickListener   = { point ->
                        viewModel.onMapPointSelected(point)
                    }
                )

                Spacer(Modifier.height(6.dp))

                val selectedLocation by viewModel.selectedLocation.collectAsState()
                selectedLocation?.let { loc ->
                    Text(
                        text  = "📍 %.5f, %.5f".format(loc.latitude, loc.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // SECCIÓN 5: CAPACIDAD
            SectionCard(title = stringResource(R.string.edit_event_capacity_section)) {
                LabelText(stringResource(R.string.edit_event_capacity_label))
                CustomTextField(viewModel.maxAttendees, { viewModel.maxAttendees = it }, stringResource(R.string.edit_event_capacity_placeholder))
            }

            // BOTONES DE ACCIÓN
            Button(
                onClick = { viewModel.updateEvent() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.isFormValid
            ) {
                Text(stringResource(R.string.edit_event_save_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // BOTÓN ELIMINAR
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
            ) {
                Text(stringResource(R.string.edit_event_delete_button_text), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // DIALOGS
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_event_delete_dialog_title_text)) },
            text = { Text(stringResource(R.string.edit_event_delete_dialog_message_text)) },
            confirmButton = {
                Button(onClick = { viewModel.deleteEvent() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text(stringResource(R.string.edit_event_delete_confirm_text))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.edit_event_cancel)) }
            }
        )
    }

    if (showImageUrlDialog) {
        AlertDialog(
            onDismissRequest = { showImageUrlDialog = false },
            title = { Text(stringResource(R.string.edit_event_image_url_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = viewModel.imageUrl.value,
                    onValueChange = { viewModel.imageUrl.onChange(it) },
                    placeholder = { Text(stringResource(R.string.edit_event_image_url_placeholder)) }
                )
            },
            confirmButton = { Button(onClick = { showImageUrlDialog = false }) { Text(stringResource(R.string.edit_event_image_url_accept)) } }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(stringResource(R.string.edit_event_category_dialog_title)) },
            text = {
                Column {
                    Category.entries.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat.name) },
                            modifier = Modifier.clickable { viewModel.category = cat; showCategoryDialog = false }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Date/Time pickers omitidos por brevedad (usar la misma lógica que en CreateEventScreen)
}