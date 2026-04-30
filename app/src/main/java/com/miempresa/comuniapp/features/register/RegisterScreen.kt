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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.components.ConfirmDialog
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors
import kotlinx.coroutines.delay
import java.io.File

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

    // 📷 Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { photo = it.toString() }
        }
    }

    // 🖼️ Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { photo = it.toString() }
    }

    // 🔐 Permiso cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tempCameraUri = createTempImageUri(context)
            tempCameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    val loadingMessage = stringResource(R.string.register_loading)

    LaunchedEffect(registerResult) {
        registerResult?.let { result ->

            val message = when (result) {
                is RequestResult.Success -> result.message
                is RequestResult.Failure -> result.errorMessage
                is RequestResult.Loading -> loadingMessage
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

            // 👤 FOTO
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { showImageOptions = true },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo.ifBlank { "https://i.pravatar.cc/300" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd)
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
                                category.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp
                            )
                        },
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.register(photo) },
                enabled = viewModel.isFormValid,
                colors = appPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                if (registerResult is RequestResult.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
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

    // 📷 BottomSheet
    if (showImageOptions) {
        ModalBottomSheet(
            onDismissRequest = { showImageOptions = false }
        ) {
            Column {
                TextButton(onClick = {
                    showImageOptions = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Tomar foto")
                }

                TextButton(onClick = {
                    showImageOptions = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Elegir de galería")
                }
            }
        }
    }

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

private fun createTempImageUri(context: Context): Uri {
    val file = File.createTempFile("photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}