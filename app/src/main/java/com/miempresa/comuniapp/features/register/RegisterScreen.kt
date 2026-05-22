package com.miempresa.comuniapp.features.register

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel  // ✅ Corregido: era androidx.hilt.lifecycle.viewmodel.compose
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.components.ConfirmDialog
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors
import java.io.File

/**
 * Pantalla de registro de nuevos usuarios.
 *
 * Maneja:
 * - Selección de foto de perfil (cámara o galería).
 * - Validación reactiva de cada campo del formulario.
 * - Selección de categorías de interés con [FilterChip].
 * - Navegación hacia atrás con diálogo de confirmación ([BackHandler]).
 * - Estados de [RequestResult]: spinner en el botón, Snackbar de error,
 *   y navegación automática al éxito.
 *
 * @param onNavigateToBack Callback para volver a la pantalla anterior.
 * @param viewModel        ViewModel inyectado por Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    onNavigateToBack: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var showExitDialog by remember { mutableStateOf(false) }
    var showImageOptions by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var photo by remember { mutableStateOf("") }

    BackHandler { showExitDialog = true }

    val snackbarHostState = remember { SnackbarHostState() }
    val registerResult by viewModel.registerResult.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()

    // 📷 Lanzador de cámara: actualiza la foto si la captura fue exitosa
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempCameraUri?.let { photo = it.toString() }
    }

    // 🖼️ Lanzador de galería: actualiza la foto con la URI seleccionada
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { photo = it.toString() }
    }

    // 🔐 Lanzador de permiso de cámara: abre la cámara si se concede
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tempCameraUri = createTempImageUri(context)
            tempCameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    /**
     * Reacciona a los cambios en [registerResult]:
     * - [RequestResult.Success]: navega hacia atrás tras mostrar el mensaje.
     * - [RequestResult.Failure]: muestra el error en el Snackbar.
     * - [RequestResult.Loading]: no interrumpe con Snackbar; el botón muestra el spinner.
     */
    LaunchedEffect(registerResult) {
        when (val result = registerResult) {
            is RequestResult.Success -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.resetRegisterResult()
                onNavigateToBack()
            }
            is RequestResult.Failure -> {
                snackbarHostState.showSnackbar(result.errorMessage)
                viewModel.resetRegisterResult()
            }
            // Loading y null se gestionan visualmente en el botón
            else -> Unit
        }
    }

    Scaffold(
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
                contentDescription = null,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { showImageOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (photo.isNotBlank()) {
                    // URI local seleccionada: Coil la carga directamente desde el dispositivo
                    AsyncImage(
                        model              = Uri.parse(photo),
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    // Sin foto seleccionada: muestra ícono genérico
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = null,
                        modifier           = Modifier.size(48.dp),
                        tint               = Color.Gray
                    )
                }
                Icon(
                    imageVector        = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier           = Modifier.align(Alignment.BottomEnd),
                    tint               = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AppTextField(
                value = viewModel.name.value,
                onValueChange = { viewModel.name.onChange(it) },
                label = stringResource(R.string.register_name_label),
                icon = Icons.Default.Person,
                error = viewModel.name.error
            )

            AppTextField(
                value = viewModel.phone.value,
                onValueChange = { viewModel.phone.onChange(it) },
                label = stringResource(R.string.register_phone_label),
                icon = Icons.Default.Person,
                error = viewModel.phone.error
            )

            AppTextField(
                value = viewModel.direccion.value,
                onValueChange = { viewModel.direccion.onChange(it) },
                label = stringResource(R.string.register_address_label),
                icon = Icons.Default.LocationOn,
                error = viewModel.direccion.error
            )

            AppTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.onChange(it) },
                label = stringResource(R.string.register_email_label),
                icon = Icons.Default.Email,
                error = viewModel.email.error
            )

            AppPasswordField(
                value = viewModel.password.value,
                onValueChange = { viewModel.password.onChange(it) },
                label = stringResource(R.string.register_password_label),
                icon = Icons.Default.Lock,
                error = viewModel.password.error
            )

            AppPasswordField(
                value = viewModel.confirmPassword.value,
                onValueChange = { viewModel.confirmPassword.onChange(it) },
                label = stringResource(R.string.register_confirm_password_label),
                icon = Icons.Default.Lock,
                error = viewModel.confirmPassword.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.register_categories_label),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips de selección de categorías de interés
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
                                text = category.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp
                            )
                        },
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            /**
             * Botón de registro:
             * - Se deshabilita si el formulario no es válido o si hay una operación en curso.
             * - Muestra [CircularProgressIndicator] durante [RequestResult.Loading].
             */
            Button(
                onClick = { viewModel.register(photo) },
                enabled = viewModel.isFormValid && registerResult !is RequestResult.Loading,
                colors = appPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                if (registerResult is RequestResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.register_button))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToBack) {
                Text(stringResource(R.string.common_back))
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // 📷 BottomSheet para elegir fuente de la foto de perfil
    if (showImageOptions) {
        ModalBottomSheet(onDismissRequest = { showImageOptions = false }) {
            Column {
                TextButton(onClick = {
                    showImageOptions = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(stringResource(R.string.register_take_photo))
                }
                TextButton(onClick = {
                    showImageOptions = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text(stringResource(R.string.register_choose_gallery))
                }
            }
        }
    }

    // 🚪 Diálogo de confirmación al presionar atrás con el formulario activo
    if (showExitDialog) {
        ConfirmDialog(
            title = stringResource(R.string.register_exit_dialog_title),
            text = stringResource(R.string.register_exit_dialog_message),
            onDismiss = { showExitDialog = false },
            onConfirm = {
                viewModel.resetForm()
                onNavigateToBack()
            }
        )
    }
}

/**
 * Crea un archivo temporal en la caché de la app y retorna su URI
 * compatible con [FileProvider], necesaria para que la cámara pueda
 * escribir la foto capturada.
 */
private fun createTempImageUri(context: Context): Uri {
    val file = File.createTempFile("photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}