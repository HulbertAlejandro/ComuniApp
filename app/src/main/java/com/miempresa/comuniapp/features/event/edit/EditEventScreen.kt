package com.miempresa.comuniapp.features.event.edit

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.miempresa.comuniapp.features.event.create.*

/**
 * Pantalla de edición de un evento existente.
 *
 * Carga el evento desde Firestore al componerse y permite editar:
 * - Imágenes (agregar desde cámara/galería, eliminar existentes).
 * - Título, descripción y categoría.
 * - Fechas y horas de inicio y fin.
 * - Ubicación en el mapa.
 * - Capacidad máxima.
 *
 * También permite eliminar el evento con confirmación mediante diálogo.
 *
 * Manejo de estados:
 * - [RequestResult.Loading]: botones deshabilitados con spinner.
 * - [RequestResult.Success]: Snackbar de confirmación y navegación hacia atrás.
 * - [RequestResult.Failure]: Snackbar de error, sin navegación.
 *
 * @param eventId   ID del evento a editar.
 * @param onBack    Callback para regresar al feed o pantalla anterior.
 * @param viewModel ViewModel inyectado por Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EditEventViewModel = hiltViewModel()
) {
    val context   = LocalContext.current
    val result    by viewModel.result.collectAsState()
    val imageUris by viewModel.imageUris.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog     by remember { mutableStateOf(false) }
    var showCategoryDialog   by remember { mutableStateOf(false) }
    var showImageSourceSheet by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }
    var showTimePicker       by remember { mutableStateOf(false) }
    var pickingForStart      by remember { mutableStateOf(true) }

    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    val initialPoint = remember(eventId) { viewModel.initialMapPoint }

    // ── Launchers (misma lógica que CreateEventScreen) ───────────────────

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

    // ── Carga inicial del evento ─────────────────────────────────────────
    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    /**
     * Reacciona a cada cambio en [result]:
     * - [RequestResult.Success]: muestra mensaje y navega hacia atrás.
     * - [RequestResult.Failure]: muestra el error en Snackbar.
     * - [RequestResult.Loading]: los botones gestionan el indicador visual.
     */
    LaunchedEffect(result) {
        when (val r = result) {
            is RequestResult.Success -> {
                snackbarHostState.showSnackbar(r.message)
                viewModel.resetResult()
                onBack()
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
                        stringResource(R.string.edit_event_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                R.string.edit_event_back_button_description
                            )
                        )
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
            SectionCard(title = stringResource(R.string.edit_event_image_section)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(imageUris) { index, uri ->
                        EditImageThumbnail(
                            uri      = uri,
                            onRemove = { viewModel.removeImage(index) }
                        )
                    }
                    item {
                        AddImageButton(onClick = { showImageSourceSheet = true })
                    }
                }
                if (imageUris.isEmpty()) {
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
            SectionCard(title = stringResource(R.string.edit_event_details_section)) {
                LabelText(stringResource(R.string.edit_event_title_label))
                CustomTextField(
                    viewModel.title.value,
                    { viewModel.title.onChange(it) },
                    stringResource(R.string.edit_event_title_placeholder)
                )
                Spacer(Modifier.height(12.dp))
                LabelText(stringResource(R.string.edit_event_category_label))
                OutlinedCard(
                    onClick  = { showCategoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(viewModel.category.name, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LabelText(stringResource(R.string.edit_event_description_label))
                CustomTextField(
                    viewModel.description.value,
                    { viewModel.description.onChange(it) },
                    stringResource(R.string.edit_event_description_placeholder),
                    isSingle = false,
                    minLines = 4
                )
            }

            // ══ SECCIÓN 3: FECHA Y HORA ═══════════════════════════════════
            SectionCard(title = stringResource(R.string.edit_event_datetime_section)) {
                DateTimeRow(
                    stringResource(R.string.edit_event_start_label),
                    viewModel.startDateMillis,
                    { pickingForStart = true; showDatePicker = true },
                    { pickingForStart = true; showTimePicker = true }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                DateTimeRow(
                    stringResource(R.string.edit_event_end_label),
                    viewModel.endDateMillis,
                    { pickingForStart = false; showDatePicker = true },
                    { pickingForStart = false; showTimePicker = true }
                )
            }

            // ══ SECCIÓN 4: UBICACIÓN ══════════════════════════════════════
            SectionCard(title = stringResource(R.string.edit_event_location_section)) {
                MapBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    activateClick        = true,
                    showMyLocationButton = true,
                    initialPoint         = initialPoint,
                    onMapClickListener   = { point -> viewModel.onMapPointSelected(point) }
                )
                Spacer(Modifier.height(6.dp))
                val selectedLocation by viewModel.selectedLocation.collectAsState()
                selectedLocation?.let { loc ->
                    Text(
                        text     = "📍 %.5f, %.5f".format(loc.latitude, loc.longitude),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ══ SECCIÓN 5: CAPACIDAD ══════════════════════════════════════
            SectionCard(title = stringResource(R.string.edit_event_capacity_section)) {
                LabelText(stringResource(R.string.edit_event_capacity_label))
                CustomTextField(
                    viewModel.maxAttendees,
                    { viewModel.maxAttendees = it },
                    stringResource(R.string.edit_event_capacity_placeholder)
                )
            }

            /**
             * Botón de guardar cambios:
             * - Se deshabilita si el formulario no es válido o hay operación en curso.
             * - Muestra spinner durante [RequestResult.Loading].
             */
            Button(
                onClick  = { viewModel.updateEvent() },
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
                        stringResource(R.string.edit_event_save_button),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }

            /**
             * Botón de eliminar:
             * - Se deshabilita durante [RequestResult.Loading] para evitar
             *   eliminar mientras hay una operación de guardado en progreso.
             */
            TextButton(
                onClick  = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled  = result !is RequestResult.Loading,
                colors   = ButtonDefaults.textButtonColors(
                    contentColor         = Color(0xFFD32F2F),
                    disabledContentColor = Color(0xFFD32F2F).copy(alpha = 0.4f)
                )
            ) {
                Text(
                    stringResource(R.string.edit_event_delete_button_text),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ══ MODAL BOTTOM SHEET ═══════════════════════════════════════════════
    if (showImageSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            sheetState       = sheetState
        ) {
            EditImageSourceContent(
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

    // ══ DIALOGS ══════════════════════════════════════════════════════════

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_event_delete_dialog_title_text)) },
            text  = { Text(stringResource(R.string.edit_event_delete_dialog_message_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteEvent()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(stringResource(R.string.edit_event_delete_confirm_text)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.edit_event_cancel))
                }
            }
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
                            modifier = Modifier.clickable {
                                viewModel.category = cat
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

// ── Componentes privados ─────────────────────────────────────────────────────

/**
 * Miniatura editable con botón X.
 * Diseño idéntico al de [CreateEventScreen] para consistencia visual.
 *
 * @param uri      URI de la imagen (local o remota).
 * @param onRemove Callback al tocar el botón de eliminar.
 */
@Composable
private fun EditImageThumbnail(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model              = uri,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
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
                contentDescription = stringResource(R.string.edit_event_remove_image),
                tint               = Color.White,
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Contenido del ModalBottomSheet para la pantalla de edición.
 * Separado de [CreateEventScreen] para permitir strings distintos si es necesario.
 *
 * @param onCameraClick  Callback para abrir la cámara.
 * @param onGalleryClick Callback para abrir la galería.
 */
@Composable
private fun EditImageSourceContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = stringResource(R.string.edit_event_image_source_title),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(bottom = 8.dp)
        )
        OutlinedButton(
            onClick  = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.edit_event_take_photo))
        }
        Button(
            onClick  = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.edit_event_choose_gallery))
        }
        Spacer(Modifier.height(16.dp))
    }
}