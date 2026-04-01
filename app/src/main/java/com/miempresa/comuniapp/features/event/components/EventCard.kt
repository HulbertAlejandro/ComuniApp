package com.miempresa.comuniapp.features.event.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus

@Composable
fun EventCard(
    event: Event,
    onInterestedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Imagen del Evento con Overlay de Chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    error = painterResource(id = R.drawable.ic_launcher_background)
                )

                // Chips superiores en la misma fila (Overlay)
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = event.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )

                    val isFull = event.eventStatus == EventStatus.FULL
                    val statusText = when(event.eventStatus) {
                        EventStatus.ACTIVE -> "ACTIVO"
                        EventStatus.FULL -> "LLENO"
                        EventStatus.FINISHED -> "FINALIZADO"
                    }
                    val statusColor = if (isFull) Color.Red else Color(0xFF4CAF50)
                    
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = statusColor.copy(alpha = 0.9f),
                            labelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Título
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fecha
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.startDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ubicación (Coordenadas ya que no hay locationName)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lat: ${event.location.latitude}, Lon: ${event.location.longitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Asistentes y Organizador en una sola línea
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val isFull = event.eventStatus == EventStatus.FULL
                    val attendeesText = if (isFull) "CUPOS LLENOS" else "${event.currentAttendees} / ${event.maxAttendees ?: "∞"} asistentes"
                    val attendeesColor = if (isFull) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant

                    Text(
                        text = attendeesText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = attendeesColor
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Org: ${event.organizerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones Inferiores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFull = event.eventStatus == EventStatus.FULL
                    Button(
                        onClick = onInterestedClick,
                        enabled = !isFull,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Me interesa", style = MaterialTheme.typography.labelLarge)
                    }

                    // Interacciones (Fuego y Comentarios)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${event.interestCount}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💬", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${event.commentsCount}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
