package com.miempresa.comuniapp.features.user.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Soluciona: Unresolved reference 'clip'
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Soluciona el error de Deprecated
import com.miempresa.comuniapp.domain.model.Badge
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.UserLevel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    paddingValues: PaddingValues, // Ahora se usa en el Modifier.padding
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val badges by viewModel.badges.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Text(
                    text = "Logros",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        // Combinamos el innerPadding del Scaffold con el paddingValues que recibe la función
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            user?.let { u ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        ReputationCard(user = u)
                    }

                    item {
                        Text(
                            text = "Insignias Obtenidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (badges.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No has obtenido insignias aún",
                                    fontSize = 15.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    } else {
                        items(badges) { badge ->
                            BadgeItem(badge)
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ReputationCard(user: User) {
    val points = user.reputation.points
    val level = user.reputation.level
    val nextLevel = nextLevelPoints(level)
    val progress = if (nextLevel > 0) points.toFloat() / nextLevel.toFloat() else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${levelEmoji(level)} ${level.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "$points pts",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2E7D32),
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(Modifier.height(4.dp))
            if (nextLevel > 0) {
                Text(
                    text = "$points / $nextLevel pts para el siguiente nivel",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
            } else {
                Text(
                    text = "Nivel máximo alcanzado",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BadgeItem(badge: Badge) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = badgeEmoji(badge.id),
                fontSize = 28.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = badge.description,
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatBadgeDate(badge.achievedAt),
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────

private fun badgeEmoji(badgeId: String): String = when {
    badgeId.startsWith("badge_pionero") -> "🚀"
    badgeId.startsWith("badge_constante") -> "🔥"
    badgeId.startsWith("badge_star") -> "⭐"
    else -> "🏆"
}

private fun levelEmoji(level: UserLevel): String = when (level) {
    UserLevel.ESPECTADOR -> "👀"
    UserLevel.PARTICIPANTE -> "🙌"
    UserLevel.ORGANIZADOR -> "📋"
    UserLevel.LIDER_COMUNITARIO -> "🌟"
}

private fun nextLevelPoints(level: UserLevel): Int = when (level) {
    UserLevel.ESPECTADOR -> 100
    UserLevel.PARTICIPANTE -> 300
    UserLevel.ORGANIZADOR -> 600
    UserLevel.LIDER_COMUNITARIO -> 0
}

private fun formatBadgeDate(timestamp: Long): String {
    // Corregido Locale para evitar el constructor deprecated
    val locale = Locale.forLanguageTag("es-ES")
    val sdf = java.text.SimpleDateFormat("d MMM yyyy", locale)
    return "Obtenido el ${sdf.format(java.util.Date(timestamp))}"
}