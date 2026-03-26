package com.miempresa.comuniapp.features.user.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onUserClick: (String) -> Unit,
    viewModel: UserListViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Usuarios") }
            )
        }
    ) { padding ->
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(users) { user ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(user.email)
                                Text(
                                    text = "${user.city} - ${user.address}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(user.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Divider()
                }
            }
        }
    }
}
