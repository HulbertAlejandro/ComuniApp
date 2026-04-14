package com.miempresa.comuniapp.features.user.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.ui.components.ConfirmDialog
import com.miempresa.comuniapp.ui.theme.*

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradientBackground)
    ) {
        user?.let { u ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🔝 HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mi Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextMain
                    )
                }

                // 👤 AVATAR
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(InputBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (u.profilePictureUrl.isNotBlank()) {
                            AsyncImage(
                                model = u.profilePictureUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = u.name.take(1),
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextGray
                            )
                        }
                    }

                    // 📷 Icono cámara
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = u.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextMain
                    )

                    Text(
                        text = "@${u.email.substringBefore("@")}",
                        color = TextGray
                    )

                    Text(
                        text = "Nivel ${u.reputation.level.ordinal + 1} - ${
                            u.reputation.level.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        }",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ✏️ EDITAR PERFIL
                Button(
                    onClick = onEditProfile,
                    colors = appPrimaryButtonColors(),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Editar perfil")
                }

                Spacer(Modifier.height(16.dp))

                // 📊 STATS
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("12", "CREADOS")
                        StatItem("45", "ASISTIDOS")
                        StatItem("8", "GUARDADOS")
                        StatItem(u.reputation.points.toString(), "PUNTOS")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 📋 OPCIONES
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        OptionItem("Mis eventos", Icons.Outlined.Event)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        OptionItem("Eventos guardados", Icons.Outlined.BookmarkBorder)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        OptionItem("Logros", Icons.Outlined.StarBorder)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 🚪 LOGOUT
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.1f),
                        contentColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }

                Spacer(Modifier.height(30.dp))
            }

        } ?: Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }

    // ✅ Confirm Dialog
    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Cerrar sesión",
            text = "¿Seguro que deseas salir?",
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                viewModel.logout()
                onLogout()
            }
        )
    }
}

// ───────────────── COMPONENTES ─────────────────

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = TextMain,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextLightGray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun OptionItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray)
        Spacer(Modifier.width(12.dp))

        Text(
            text = text,
            color = TextMain,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextGray
        )
    }
}