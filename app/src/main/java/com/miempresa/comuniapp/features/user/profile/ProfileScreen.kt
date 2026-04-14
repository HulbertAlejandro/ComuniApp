package com.miempresa.comuniapp.features.user.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {

    val user by viewModel.user.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()

    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        user?.let {
            name = it.name
            city = it.phoneNumber
            address = it.phoneNumber
            phone = it.phoneNumber ?: ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        user?.let { currentUser ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),

                // 👇 CLAVE: alineación como profesor
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 👇 FOTO CENTRADA (como profesor)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser.profilePictureUrl.isNotEmpty()) {
                        AsyncImage(
                            model = currentUser.profilePictureUrl,
                            contentDescription = stringResource(R.string.profile_picture_description),
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // EMAIL (solo lectura)
                OutlinedTextField(
                    value = currentUser.email,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false
                )

                // NOMBRE
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditMode
                )

                // CIUDAD
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.city_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditMode
                )

                // DIRECCIÓN
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditMode
                )

                // TELÉFONO
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditMode
                )

                // ROL
                Text(
                    text = stringResource(R.string.role_prefix) + " ${currentUser.role.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BOTONES COMO PROFESOR
                if (isEditMode) {

                    Button(
                        onClick = {
                            val updatedUser = currentUser.copy(
                                name = name,
                                phoneNumber = phone
                            )
                            viewModel.updateUser(updatedUser)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = appPrimaryButtonColors()
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.save_button), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.cancel_button), fontWeight = FontWeight.Bold)
                    }

                } else {

                    Button(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = appPrimaryButtonColors()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.edit_profile_button))
                    }
                }

                // LOGOUT
                Button(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.logout_button))
                }
            }

        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}