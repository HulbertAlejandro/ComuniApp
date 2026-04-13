package com.miempresa.comuniapp.features.event.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Colores del tema
// ─────────────────────────────────────────────────────────────────────────────
private val GreenPrimary  = Color(0xFF2E7D32)
private val GreenLight    = Color(0xFFE8F5E9)
private val TextPrimary   = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val Divider       = Color(0xFFEEEEEE)
private val CardBg        = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val event    by viewModel.event.collectAsState()
    val organizer by viewModel.organizer.collectAsState()
    val interestedEventIds by viewModel.interestedEventIds.collectAsState() // ✅ CAMBIAR NOMBRE

    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    if (event == null) {
        Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        return
    }

    val ev = event!!

    val isInterested = interestedEventIds.contains(ev.id) // ✅ USAR interestedEventIds

    // Fecha formateada: "Sáb 8 Mar · 9:00 AM"
    val dateFormatted = try {
        val dt = LocalDateTime.parse(ev.startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        dt.format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", Locale("es", "ES")))
            .replaceFirstChar { it.uppercase() }
    } catch (e: Exception) { ev.startDate }

    // Progreso de cupo
    val attendeesProgress = if ((ev.maxAttendees ?: 0) > 0)
        ev.currentAttendees.toFloat() / (ev.maxAttendees ?: 1).toFloat()
    else 0f

    val isFull = ev.maxAttendees?.let { ev.currentAttendees >= it } ?: false

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Detalle del evento",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── 1. Imagen con badges ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                AsyncImage(
                    model = ev.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Badges superpuestos (esquina top-left)
                Row(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryBadge(ev.category)
                    StatusBadge(ev.eventStatus)
                }
            }

            // ── 2. Título, fecha y lugar ──────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = ev.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateFormatted,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                // Lugar: usamos lat/lon hasta implementar geocoding
                Text(
                    text = "${"%.4f".format(ev.location.latitude)}, ${"%.4f".format(ev.location.longitude)}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = Divider)

            // ── 3. Fila de acciones: Asistir · Me interesa · Comentarios ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Asistir (placeholder — sin implementar aún)
                OutlinedButton(
                    onClick = { /* TODO: implementar asistencia */ },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Asistir", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Me interesa (SOLO VISUAL - DESHABILITADO)
                OutlinedButton(
                    onClick = {}, // no hace nada
                    enabled = false, // 👈 ESTO ES CLAVE
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isInterested) GreenLight else Color.White,
                        contentColor   = if (isInterested) GreenPrimary else TextPrimary,
                        disabledContainerColor = if (isInterested) GreenLight else Color.White,
                        disabledContentColor   = if (isInterested) GreenPrimary else TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isInterested) GreenPrimary else Color(0xFFBDBDBD)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = if (isInterested) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Me interesa ${ev.interestCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Comentarios (placeholder)
                OutlinedButton(
                    onClick = { /* TODO: ir a comentarios */ },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Comentarios ${ev.commentsCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = Divider)

            // ── 4. Descripción ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle("DESCRIPCIÓN")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ev.description,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }

            HorizontalDivider(color = Divider)

            // ── 5. Cupos disponibles ──────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle("CUPOS DISPONIBLES")
                Spacer(Modifier.height(10.dp))

                // Barra de progreso
                LinearProgressIndicator(
                    progress = { attendeesProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isFull) Color(0xFFC62828) else GreenPrimary,
                    trackColor = Color(0xFFE0E0E0)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${ev.currentAttendees} / ${ev.maxAttendees ?: "∞"} asistentes",
                    fontSize = 13.sp,
                    color = if (isFull) Color(0xFFC62828) else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = Divider)

            // ── 6. Organizador ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                OrganizerRow(organizer = organizer, ownerId = ev.ownerId)
            }

            HorizontalDivider(color = Divider)

            // ── 7. Ubicación (lat/lon hasta implementar mapa) ─────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle("UBICACIÓN")
                Spacer(Modifier.height(10.dp))

                // Placeholder del mapa con coordenadas visibles
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEEEE))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${"%.5f".format(ev.location.latitude)}, ${"%.5f".format(ev.location.longitude)}",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Botones debajo del mapa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: abrir mapa */ },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD))
                    ) {
                        Text("Ver en mapa", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { /* TODO: abrir navegación */ },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD))
                    ) {
                        Text("Cómo llegar", fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(color = Divider)

            // ── 8. Comentarios destacados (cajón placeholder) ─────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle("COMENTARIOS DESTACADOS")
                Spacer(Modifier.height(12.dp))

                if (ev.commentsCount == 0) {
                    // Estado vacío — cajón listo para cuando se implementen comentarios
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBg)
                            .border(1.dp, Divider, RoundedCornerShape(10.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sé el primero en comentar este evento",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    // Placeholder con comentarios de ejemplo
                    // (reemplazar por lista real cuando se implemente el módulo de comentarios)
                    repeat(minOf(ev.commentsCount, 2)) { index ->
                        CommentPlaceholderItem(
                            userName = if (index == 0) "Laura M." else "Pedro G.",
                            timeAgo  = if (index == 0) "Hace 2h" else "Hace 5h",
                            text     = if (index == 0)
                                "¿Saben si se puede llevar equipo propio o ellos lo proveen todo?"
                            else
                                "Excelente iniciativa, nos vamos ahí con el equipo."
                        )
                        if (index == 0) Spacer(Modifier.height(8.dp))
                    }

                    if (ev.commentsCount > 2) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = { /* TODO: navegar a lista completa de comentarios */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                "Ver los ${ev.commentsCount} comentarios",
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes internos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp
    )
}

@Composable
private fun CategoryBadge(category: Category) {
    Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(4.dp)) {
        Text(
            text = category.name.uppercase(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: com.miempresa.comuniapp.domain.model.EventStatus) {
    val (color, label) = when (status) {
        com.miempresa.comuniapp.domain.model.EventStatus.ACTIVE   -> Color(0xFF2E7D32) to "ACTIVO"
        com.miempresa.comuniapp.domain.model.EventStatus.FULL     -> Color(0xFFC62828) to "LLENO"
        com.miempresa.comuniapp.domain.model.EventStatus.CREATED  -> Color(0xFFE65100) to "PENDIENTE"
        com.miempresa.comuniapp.domain.model.EventStatus.FINISHED -> Color(0xFF424242) to "FINALIZADO"
    }
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun OrganizerRow(organizer: User?, ownerId: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            if (!organizer?.profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = organizer?.profilePictureUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Nombre y rol
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = organizer?.name ?: "ID: $ownerId",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Organizador",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Rating (puntos de reputación convertidos a escala 5.0)
        val rating = organizer?.reputation?.points?.let { pts ->
            // Escala: 0–600+ pts → 0.0–5.0
            (pts.coerceAtMost(600) / 600f * 5f)
                .let { "%.1f".format(it) }
        } ?: "–"

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null,
                tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
            Text(text = rating, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun CommentPlaceholderItem(userName: String, timeAgo: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar pequeño
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null,
                tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
        }

        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(userName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                Text(timeAgo, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(2.dp))
            Text(text, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
        }
    }
}