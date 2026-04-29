package com.miempresa.comuniapp.features.user.edit

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.domain.model.Category
import java.io.File

private val BgScreen    = Color(0xFFF7F7F7)
private val FieldBg     = Color(0xFFEAEAEA)
private val ChipActive  = Color(0xFF000000)
private val ChipInactive = Color(0xFFE0E0E0)
private val SaveBtn     = Color(0xFFD6D6D6)
private val TextDark    = Color(0xFF1A1A1A)
private val TextMuted   = Color(0xFF9E9E9E)
private val LabelColor  = Color(0xFF555555)
private val DeleteRed   = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val user by viewModel.user.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageOptions by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

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

    // Cargar datos iniciales
    LaunchedEffect(user) {
        user?.let { u ->
            name = u.name
            phone = u.phoneNumber
            photo = u.profilePictureUrl
        }
    }

    // Eventos (SNACKBAR + navegación)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UserEditUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UserEditUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgScreen,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.user_edit_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgScreen
                )
            )
        }
    ) { innerPadding ->

        user?.let { u ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 👤 FOTO (CLICKABLE)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(FieldBg)
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
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    )
                }

                PillField(
                    label = stringResource(R.string.user_edit_name_label),
                    value = name,
                    onValueChange = { name = it }
                )

                PillField(
                    label = stringResource(R.string.user_edit_phone_label),
                    value = phone,
                    onValueChange = { phone = it }
                )

                PillField(
                    label = stringResource(R.string.user_edit_email_label),
                    value = u.email,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false
                )

                // 📂 Categorías
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.user_edit_categories_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LabelColor
                    )

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
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChipActive,
                                    selectedLabelColor = Color.White,
                                    containerColor = ChipInactive,
                                    labelColor = TextDark
                                ),
                                shape = RoundedCornerShape(50.dp),
                                border = null
                            )
                        }
                    }
                }

                // 💾 Guardar
                Button(
                    onClick = { viewModel.saveUser(name, phone, photo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaveBtn,
                        contentColor = TextDark
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextDark
                        )
                    } else {
                        Text(
                            stringResource(R.string.user_edit_save_button_label),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ❌ Eliminar cuenta
                TextButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Text(
                        stringResource(R.string.user_edit_delete_account_button),
                        color = DeleteRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        } ?: Box(
            Modifier.fillMaxSize(),
            Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // 🧾 BottomSheet (imagen)
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

    // ⚠️ Dialog eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.user_edit_delete_dialog_title)) },
            text = { Text(stringResource(R.string.user_edit_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    }
                ) {
                    Text(
                        stringResource(R.string.user_edit_delete_account_button_label),
                        color = DeleteRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.user_edit_cancel_button))
                }
            }
        )
    }
}

// 📁 URI temporal
private fun createTempImageUri(context: Context): Uri {
    val file = File.createTempFile("photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

@Composable
private fun PillField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = LabelColor
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            enabled = enabled,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                color = if (enabled) TextDark else TextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldBg, RoundedCornerShape(50.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        )
    }
}