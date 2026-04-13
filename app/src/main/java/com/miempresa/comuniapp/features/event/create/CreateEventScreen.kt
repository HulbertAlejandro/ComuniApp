package com.miempresa.comuniapp.features.event.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Category
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onEventCreated: () -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val result by viewModel.result.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados de Dialogs
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showImageUrlDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var pickingForStart by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    LaunchedEffect(result) {
        result?.let {
            if (it is RequestResult.Success) {
                // Mostramos el mensaje antes de salir
                snackbarHostState.showSnackbar(it.message)
                // Pequeña espera para que el ojo humano capte el mensaje
                kotlinx.coroutines.delay(800)
                onEventCreated()
            } else if (it is RequestResult.Failure) {
                snackbarHostState.showSnackbar(it.errorMessage)
            }
            viewModel.resetResult()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear Evento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
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
            SectionCard(title = "Imagen") {
                AsyncImage(
                    model = viewModel.imageUrl.value.ifBlank { null },
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEEEEE)),
                    contentScale = ContentScale.Crop
                )
                Button(
                    onClick = { showImageUrlDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color.Black)
                ) { Text("Configurar URL de Imagen") }
            }

            // SECCIÓN 2: INFORMACIÓN
            SectionCard(title = "Detalles del Evento") {
                LabelText("TÍTULO DEL EVENTO")
                CustomTextField(viewModel.title.value, { viewModel.title.onChange(it) }, "Ej: Festival de Verano")

                Spacer(Modifier.height(12.dp))
                LabelText("CATEGORÍA")
                OutlinedCard(onClick = { showCategoryDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(viewModel.selectedCategory?.name ?: "Seleccionar...", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                Spacer(Modifier.height(12.dp))
                LabelText("DESCRIPCIÓN")
                CustomTextField(viewModel.description.value, { viewModel.description.onChange(it) }, "Cuéntanos más...", false, 4)
            }

            // SECCIÓN 3: FECHA Y HORA
            SectionCard(title = "Fecha y Hora") {
                DateTimeRow("INICIO", viewModel.startDateMillis, { pickingForStart = true; showDatePicker = true }, { pickingForStart = true; showTimePicker = true })
                Divider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
                DateTimeRow("FIN", viewModel.endDateMillis, { pickingForStart = false; showDatePicker = true }, { pickingForStart = false; showTimePicker = true })
            }

            // SECCIÓN 4: UBICACIÓN
            SectionCard(title = "Ubicación") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        LabelText("LATITUD")
                        CustomTextField(viewModel.latitude.value, { viewModel.onLatitudeChange(it) }, "0.0")
                    }
                    Column(Modifier.weight(1f)) {
                        LabelText("LONGITUD")
                        CustomTextField(viewModel.longitude.value, { viewModel.onLongitudeChange(it) }, "0.0")
                    }
                }
            }

            // SECCIÓN 5: CAPACIDAD
            SectionCard(title = "Asistentes") {
                LabelText("CUPOS DISPONIBLES")
                CustomTextField(viewModel.maxAttendees.value, { viewModel.maxAttendees.onChange(it) }, "100")
            }

            // ACCIONES FINALES
            Button(
                onClick = { viewModel.createEvent() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.isFormValid
            ) {
                Text("Crear Evento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = Color.Gray)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // --- DIALOGS (CÓDIGO DE APOYO) ---
    if (showImageUrlDialog) {
        AlertDialog(
            onDismissRequest = { showImageUrlDialog = false },
            title = { Text("URL de la Imagen") },
            text = { OutlinedTextField(viewModel.imageUrl.value, { viewModel.onImageUrlChange(it) }, label = { Text("http://...") }) },
            confirmButton = { Button(onClick = { showImageUrlDialog = false }) { Text("Listo") } }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Seleccionar Categoría") },
            text = {
                Column {
                    Category.entries.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat.name) },
                            modifier = Modifier.clickable { viewModel.onCategorySelected(cat); showCategoryDialog = false }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDateTime(pickingForStart, datePickerState.selectedDateMillis, 12, 0)
                    showDatePicker = false
                }) { Text("Confirmar") }
            }
        ) { DatePicker(datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val current = if (pickingForStart) viewModel.startDateMillis else viewModel.endDateMillis
                    viewModel.updateDateTime(pickingForStart, current, timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(timePickerState) }
        )
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DateTimeRow(label: String, millis: Long?, onDate: () -> Unit, onTime: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateStr = millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(formatter) } ?: "Seleccionar fecha"
    val timeStr = millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(timeFormatter) } ?: "Hora"

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(60.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(onClick = onDate, Modifier.weight(1.5f), shape = RoundedCornerShape(8.dp), color = Color(0xFFF0F0F0)) {
                Text(dateStr, Modifier.padding(12.dp), fontSize = 12.sp)
            }
            Surface(onClick = onTime, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = Color(0xFFF0F0F0)) {
                Text(timeStr, Modifier.padding(12.dp), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, isSingle: Boolean = true, minLines: Int = 1) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        placeholder = { Text(placeholder, color = Color.LightGray) },
        singleLine = isSingle,
        minLines = minLines,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9F9F9),
            unfocusedContainerColor = Color(0xFFF9F9F9),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}