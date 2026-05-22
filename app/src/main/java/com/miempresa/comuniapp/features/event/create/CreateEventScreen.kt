package com.miempresa.comuniapp.features.event.create

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.component.MapBox
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
    val context           = LocalContext.current
    val result            by viewModel.result.collectAsState()
    val selectedImageUris by viewModel.selectedImageUris.collectAsState()
    val selectedLocation  by viewModel.selectedLocation.collectAsState()
    val suggestionState   by viewModel.suggestionState.collectAsState()  // ← nuevo
    val snackbarHostState = remember { SnackbarHostState() }

    var showImageSourceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDatePicker     by remember { mutableStateOf(false) }
    var showTimePicker     by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var pickingForStart    by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> -> viewModel.onGalleryImagesSelected(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean -> viewModel.onCameraImageCaptured(success) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createTempCameraUri(context)
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(result) {
        when (val r = result) {
            is RequestResult.Success -> {
                snackbarHostState.showSnackbar(r.message)
                viewModel.resetResult()
                onEventCreated()
            }
            is RequestResult.Failure -> {
                snackbarHostState.showSnackbar(r.errorMessage)
                viewModel.resetResult()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.create_event_button),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF7F7F7),
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ══ SECCIÓN 1: IMÁGENES ══════════════════════════════════════
            SectionCard(title = stringResource(R.string.create_event_section_image)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(selectedImageUris) { index, uri ->
                        ImageThumbnail(uri = uri, onRemove = { viewModel.removeImage(index) })
                    }
                    item { AddImageButton(onClick = { showImageSourceSheet = true }) }
                }
                if (selectedImageUris.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text     = stringResource(R.string.validation_error_image_url_required),
                        color    = MaterialTheme.colorScheme.error,
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ══ SECCIÓN 2: INFORMACIÓN ════════════════════════════════════
            SectionCard(title = stringResource(R.string.create_event_section_details)) {

                LabelText(stringResource(R.string.create_event_title_label))
                CustomTextField(
                    value         = viewModel.title.value,
                    onValueChange = {
                        viewModel.title.onChange(it)
                    },
                    placeholder = stringResource(R.string.create_event_title_placeholder)
                )

                Spacer(Modifier.height(12.dp))
                LabelText(stringResource(R.string.create_event_category_label))

                // ── Selector de categoría con badge de sugerencia ─────────
                OutlinedCard(
                    onClick  = { showCategoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text     = viewModel.selectedCategory?.name
                                ?: stringResource(R.string.create_event_category_select),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── BANNER DE SUGERENCIA DE IA ────────────────────────────
                // AnimatedVisibility hace que el banner aparezca/desaparezca
                // con una animación suave en lugar de un salto brusco
                AnimatedVisibility(
                    visible = suggestionState !is SuggestionState.Idle,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    AiSuggestionBanner(
                        state        = suggestionState,
                        onAccept     = { viewModel.acceptSuggestion() },
                        onDismiss    = { viewModel.dismissSuggestion() }
                    )
                }

                Spacer(Modifier.height(12.dp))
                LabelText(stringResource(R.string.create_event_description_label))
                CustomTextField(
                    value         = viewModel.description.value,
                    onValueChange = {
                        viewModel.description.onChange(it)
                    },
                    placeholder = stringResource(R.string.create_event_description_placeholder),
                    isSingle    = false,
                    minLines    = 4
                )
            }

            // ══ SECCIÓN 3: FECHA Y HORA ══════════════════════════════════
            SectionCard(title = stringResource(R.string.create_event_section_datetime)) {
                DateTimeRow(
                    stringResource(R.string.create_event_start),
                    viewModel.startDateMillis,
                    { pickingForStart = true; showDatePicker = true },
                    { pickingForStart = true; showTimePicker = true }
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp,
                    color     = Color.LightGray
                )
                DateTimeRow(
                    stringResource(R.string.create_event_end),
                    viewModel.endDateMillis,
                    { pickingForStart = false; showDatePicker = true },
                    { pickingForStart = false; showTimePicker = true }
                )
            }

            // ══ SECCIÓN 4: UBICACIÓN ═════════════════════════════════════
            SectionCard(title = stringResource(R.string.create_event_section_location)) {
                MapBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    activateClick        = true,
                    showMyLocationButton = true,
                    initialPoint         = null,
                    onMapClickListener   = { point -> viewModel.onMapPointSelected(point) }
                )
                if (selectedLocation == null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.create_event_location_required),
                        color    = MaterialTheme.colorScheme.error,
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "📍 %.5f, %.5f".format(
                            selectedLocation!!.latitude,
                            selectedLocation!!.longitude
                        ),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ══ SECCIÓN 5: CAPACIDAD ═════════════════════════════════════
            SectionCard(title = stringResource(R.string.create_event_section_capacity)) {
                LabelText(stringResource(R.string.create_event_capacity_label))
                CustomTextField(
                    viewModel.maxAttendees.value,
                    { viewModel.maxAttendees.onChange(it) },
                    stringResource(R.string.create_event_capacity_placeholder)
                )
            }

            Button(
                onClick  = { viewModel.createEvent() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape   = RoundedCornerShape(16.dp),
                enabled = viewModel.isFormValid && result !is RequestResult.Loading
            ) {
                if (result is RequestResult.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        stringResource(R.string.create_event_button),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.create_event_cancel_button), color = Color.Gray)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ══ MODALS Y DIALOGS (sin cambios) ═══════════════════════════════════

    if (showImageSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            sheetState       = sheetState
        ) {
            ImageSourceBottomSheetContent(
                onCameraClick = {
                    showImageSourceSheet = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onGalleryClick = {
                    showImageSourceSheet = false
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(stringResource(R.string.create_event_category_dialog_title)) },
            text = {
                Column {
                    Category.entries.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat.name) },
                            modifier = Modifier.clickable {
                                viewModel.onCategorySelected(cat)  // ← usa el nuevo método
                                showCategoryDialog = false
                            }
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
                    viewModel.updateDateTime(
                        pickingForStart,
                        datePickerState.selectedDateMillis,
                        12, 0
                    )
                    showDatePicker = false
                }) { Text(stringResource(R.string.create_event_date_dialog_confirm)) }
            }
        ) { DatePicker(datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val current = if (pickingForStart) viewModel.startDateMillis
                    else viewModel.endDateMillis
                    viewModel.updateDateTime(
                        pickingForStart, current,
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    showTimePicker = false
                }) { Text(stringResource(R.string.create_event_time_dialog_ok)) }
            },
            text = { TimePicker(timePickerState) }
        )
    }
}

// ── Componente banner de sugerencia ──────────────────────────────────────────

/**
 * Banner sutil que muestra el estado de la sugerencia de IA debajo
 * del selector de categoría.
 *
 * Tres estados visuales:
 * - [SuggestionState.Loading] → spinner pequeño + texto "Analizando..."
 * - [SuggestionState.Success] → nombre de la categoría + botones Aceptar/Descartar
 * - [SuggestionState.Error]   → mensaje de error en tono apagado
 *
 * @param state    Estado actual de la sugerencia.
 * @param onAccept Callback cuando el usuario acepta la categoría sugerida.
 * @param onDismiss Callback cuando el usuario descarta la sugerencia.
 */
@Composable
private fun AiSuggestionBanner(
    state: SuggestionState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    // Contenedor con borde izquierdo de acento para distinguirlo visualmente
    // del resto del formulario sin ser demasiado intrusivo
    Surface(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        color     = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            when (state) {

                // ── CARGANDO ────────────────────────────────────────────
                is SuggestionState.Loading -> {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text  = "✨ Analizando con IA...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── SUGERENCIA DISPONIBLE ────────────────────────────────
                is SuggestionState.Success -> {
                    // Ícono de IA
                    Icon(
                        imageVector        = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                    // Texto con la categoría sugerida
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "Sugerencia de IA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text       = state.category.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Botón Aceptar
                    TextButton(
                        onClick      = onAccept,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Aplicar",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Botón Descartar
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Descartar sugerencia",
                            modifier           = Modifier.size(16.dp),
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── ERROR ────────────────────────────────────────────────
                is SuggestionState.Error -> {
                    Icon(
                        imageVector        = Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Text(
                        text     = state.message,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            modifier           = Modifier.size(16.dp),
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SuggestionState.Idle -> Unit // AnimatedVisibility oculta este estado
            }
        }
    }
}

// ── Componentes sin cambios (se mantienen igual) ─────────────────────────────

@Composable
private fun ImageThumbnail(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = uri, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick  = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.create_event_remove_image),
                tint               = Color.White,
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AddImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEEEEEE))
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
            Text(
                stringResource(R.string.create_event_add_image_label),
                fontSize = 10.sp, color = Color.Gray
            )
        }
    }
}

@Composable
private fun ImageSourceBottomSheetContent(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.create_event_image_source_title),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedButton(
            onClick = onCameraClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.create_event_take_photo))
        }
        Button(
            onClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.create_event_choose_gallery))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DateTimeRow(label: String, millis: Long?, onDate: () -> Unit, onTime: () -> Unit) {
    val formatter     = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateStr = millis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(formatter)
    } ?: stringResource(R.string.create_event_select_date)
    val timeStr = millis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(timeFormatter)
    } ?: stringResource(R.string.create_event_time_placeholder)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(60.dp), fontWeight = FontWeight.Bold,
            fontSize = 12.sp, color = Color.Gray)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(onClick = onDate, modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(8.dp), color = Color(0xFFF0F0F0)
            ) { Text(dateStr, Modifier.padding(12.dp), fontSize = 12.sp) }
            Surface(onClick = onTime, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp), color = Color(0xFFF0F0F0)
            ) { Text(timeStr, Modifier.padding(12.dp), fontSize = 12.sp) }
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = Color.Gray,
        fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun CustomTextField(
    value: String, onValueChange: (String) -> Unit,
    placeholder: String, isSingle: Boolean = true, minLines: Int = 1
) {
    TextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        placeholder = { Text(placeholder, color = Color.LightGray) },
        singleLine = isSingle, minLines = minLines,
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = Color(0xFFF9F9F9),
            unfocusedContainerColor = Color(0xFFF9F9F9),
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}