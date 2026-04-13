package com.miempresa.comuniapp.features.event.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventCard(
    event: Event,
    organizer: User?,
    hasVoted: Boolean,          // true = el usuario ya marcó interés
    onInterestedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Imagen con badges ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge categoría
                    Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = event.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    // Badge estado
                    val (statusColor, statusLabel) = when (event.eventStatus) {
                        EventStatus.ACTIVE   -> Color(0xFF2E7D32) to "Activo"
                        EventStatus.FULL     -> Color(0xFFC62828) to "Lleno"
                        EventStatus.CREATED  -> Color(0xFFE65100) to "Pendiente"
                        EventStatus.FINISHED -> Color(0xFF424242) to "Finalizado"
                    }
                    Surface(color = statusColor, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = statusLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ── Contenido ─────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp, fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF212121),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Fecha formateada
                val dateText = try {
                    val dt = LocalDateTime.parse(
                        event.startDate,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    )
                    dt.format(
                        DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", Locale("es", "ES"))
                    ).replaceFirstChar { it.uppercase() }
                } catch (e: Exception) { event.startDate }

                Text(text = dateText, fontSize = 13.sp, color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 2.dp))

                // Asistentes · organizador · nivel
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isFull = event.maxAttendees?.let { event.currentAttendees >= it } ?: false
                    Text(
                        text = "${event.currentAttendees} / ${event.maxAttendees ?: "∞"} asistentes",
                        color = if (isFull) Color(0xFFC62828) else Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(" · ", color = Color(0xFF9E9E9E))
                    Text(
                        text = "Org: ${organizer?.name ?: "Usuario"}",
                        fontSize = 12.sp, color = Color(0xFF616161)
                    )
                    val levelName = organizer?.reputation?.level?.name
                        ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Nivel"
                    Text(" · ", color = Color(0xFF9E9E9E))
                    Text(text = levelName, color = Color(0xFFE65100),
                        fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // ── Acciones ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón "Me interesa" — toggle completo, siempre habilitado
                    OutlinedButton(
                        onClick = onInterestedClick,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (hasVoted) Color(0xFF1565C0) else Color(0xFFBDBDBD)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (hasVoted) Color(0xFFE3F2FD) else Color.White,
                            contentColor   = if (hasVoted) Color(0xFF1565C0) else Color(0xFF212121)
                        )
                    ) {
                        Icon(
                            imageVector = if (hasVoted) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (hasVoted) Color(0xFF1565C0) else Color(0xFF757575)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (hasVoted) "Me interesa ✓" else "Me interesa",
                            fontSize = 13.sp
                        )
                    }

                    // Contadores
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔥", fontSize = 14.sp)
                            Text("${event.interestCount}", fontSize = 14.sp, color = Color(0xFFFF6F00))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF9E9E9E)
                            )
                            Text("${event.commentsCount}", fontSize = 14.sp, color = Color(0xFF9E9E9E))
                        }
                    }
                }
            }
        }
    }
}