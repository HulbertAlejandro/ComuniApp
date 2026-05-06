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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.R
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
    val event by viewModel.event.collectAsState()
    val organizer by viewModel.organizer.collectAsState()
    val interestedEventIds by viewModel.interestedEventIds.collectAsState()
    val isAttending by viewModel.isAttending.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsCount by viewModel.commentsCount.collectAsState()
    val commentAuthorsMap by viewModel.commentAuthorsMap.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState() // ✅ Permisos de admin

    var newCommentText by remember { mutableStateOf("") }

    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    if (event == null) {
        Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        return
    }

    val ev = event!!
    val isInterested = interestedEventIds.contains(ev.id)

    // Fecha formateada
    val dateFormatted = try {
        val dt = LocalDateTime.parse(ev.startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        dt.format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", Locale("es", "ES")))
            .replaceFirstChar { it.uppercase() }
    } catch (e: Exception) { ev.startDate }

    val attendeesProgress = if ((ev.maxAttendees ?: 0) > 0)
        ev.currentAttendees.toFloat() / (ev.maxAttendees ?: 1).toFloat()
    else 0f

    val isFull = ev.maxAttendees?.let { ev.currentAttendees >= it } ?: false

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.event_detail_title), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.event_detail_back_button_description), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
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
            Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                AsyncImage(
                    model = ev.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryBadge(ev.category)
                    StatusBadge(ev.eventStatus)
                }
            }

            // ── 2. Título y meta-info ─────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(text = ev.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text(text = dateFormatted, fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(text = "${"%.4f".format(ev.eventLocation.latitude)}, ${"%.4f".format(ev.eventLocation.longitude)}", fontSize = 14.sp, color = TextSecondary)
            }

            HorizontalDivider(color = Divider)

            // ── 3. Fila de acciones (Lógica condicional por Rol) ──────────────

            // ✅ El admin nunca ve los botones de interacción social (Asistir/Interés).
            if (!isAdmin &&
                ev.eventStatus != EventStatus.CREATED &&
                ev.eventStatus != EventStatus.FINISHED
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.toggleAttendance() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isAttending) GreenLight else Color.White,
                            contentColor = if (isAttending) GreenPrimary else TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAttending) GreenPrimary else Color(0xFFBDBDBD)),
                        enabled = !isFull || isAttending,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(if (isAttending) stringResource(R.string.event_detail_attending) else if (isFull) stringResource(R.string.event_detail_full) else stringResource(R.string.event_detail_attend), fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(if (isInterested) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.event_detail_interested) + " ${ev.interestCount}", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Comment, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.event_detail_comments) + " $commentsCount", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(color = Divider)
            }

            // ✅ Si es admin, mostrar solo el contador de comentarios en modo lectura
            if (isAdmin) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Comment, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.event_detail_comments) + " $commentsCount", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(color = Divider)
            }

            // ── 4. Descripción ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle(stringResource(R.string.event_detail_description_section))
                Spacer(Modifier.height(8.dp))
                Text(text = ev.description, fontSize = 15.sp, color = TextPrimary, lineHeight = 22.sp)
            }

            HorizontalDivider(color = Divider)

            // ── 5. Cupos ──────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle(stringResource(R.string.event_detail_capacity_section))
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { attendeesProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (isFull) Color(0xFFC62828) else GreenPrimary,
                    trackColor = Color(0xFFE0E0E0)
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.event_detail_attendees_count, ev.currentAttendees, ev.maxAttendees ?: "∞"), fontSize = 13.sp, color = if (isFull) Color(0xFFC62828) else TextSecondary)
            }

            HorizontalDivider(color = Divider)

            // ── 6. Organizador ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                OrganizerRow(organizer = organizer, ownerId = ev.ownerId)
            }

            HorizontalDivider(color = Divider)

            // ── 7. Ubicación ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionTitle(stringResource(R.string.event_detail_location_section))
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEEEEE)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFC62828), modifier = Modifier.size(36.dp))
                        Text("${"%.5f".format(ev.eventLocation.latitude)}, ${"%.5f".format(ev.eventLocation.longitude)}", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }

            HorizontalDivider(color = Divider)

            // ── 8. Publicar comentario (Solo Usuarios) ────────────────────────
            // ✅ El admin nunca puede escribir comentarios
            if (!isAdmin &&
                ev.eventStatus != EventStatus.CREATED &&
                ev.eventStatus != EventStatus.FINISHED
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    SectionTitle(stringResource(R.string.event_detail_comment_section))
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text(stringResource(R.string.event_detail_comment_placeholder), fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = Divider)
                        )
                        Button(
                            onClick = { viewModel.postComment(newCommentText); newCommentText = "" },
                            enabled = newCommentText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(stringResource(R.string.event_detail_comment_send), fontSize = 13.sp)
                        }
                    }
                }
                HorizontalDivider(color = Divider)
            }

            // ── 9. Comentarios (Lista) ────────────────────────────────────────
            // ✅ El admin SÍ ve comentarios aunque el evento sea PENDING o CREATED
            if (isAdmin || ev.eventStatus != EventStatus.CREATED) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    SectionTitle(stringResource(R.string.event_detail_comments_featured))
                    Spacer(Modifier.height(12.dp))

                    if (comments.isEmpty()) {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBg).border(1.dp, Divider, RoundedCornerShape(10.dp)).padding(20.dp), Alignment.Center) {
                            Text(stringResource(R.string.event_detail_no_comments), fontSize = 14.sp, color = TextSecondary)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            comments.forEach { comment ->
                                CommentItem(comment = comment, authorsMap = commentAuthorsMap)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes Internos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
}

@Composable
private fun CategoryBadge(category: Category) {
    Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(4.dp)) {
        Text(category.name.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun StatusBadge(status: EventStatus) {
    val (color, label) = when (status) {
        EventStatus.ACTIVE   -> Color(0xFF2E7D32) to stringResource(R.string.event_detail_status_active)
        EventStatus.FULL     -> Color(0xFFC62828) to stringResource(R.string.event_detail_status_full)
        EventStatus.CREATED  -> Color(0xFFE65100) to stringResource(R.string.event_detail_status_pending)
        EventStatus.FINISHED -> Color(0xFF424242) to stringResource(R.string.event_detail_status_finished)
    }
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun OrganizerRow(organizer: User?, ownerId: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), Alignment.Center) {
            if (!organizer?.profilePictureUrl.isNullOrBlank()) {
                AsyncImage(model = organizer?.profilePictureUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(organizer?.name ?: stringResource(R.string.event_detail_unknown_user), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(stringResource(R.string.event_detail_organizer_label), fontSize = 12.sp, color = TextSecondary)
        }
        val rating = organizer?.reputation?.points?.let { pts -> (pts.coerceAtMost(600) / 600f * 5f).let { "%.1f".format(it) } } ?: "–"
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
            Text(rating, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun CommentItem(comment: com.miempresa.comuniapp.domain.model.Comment, authorsMap: Map<String, User>) {
    val author = authorsMap[comment.authorId]
    val userName = author?.name ?: stringResource(R.string.event_detail_unknown_user)
    val (key, args) = formatTimeAgo(comment.timestamp)

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBg).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0E0E0)), Alignment.Center) {
            if (!author?.profilePictureUrl.isNullOrBlank()) {
                AsyncImage(model = author?.profilePictureUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
            }
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(userName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                Text(stringResource(key, *args.toTypedArray()), fontSize = 11.sp, color = TextSecondary)
            }
            Text(comment.content, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
        }
    }
}

private fun formatTimeAgo(timestamp: Long): Pair<Int, List<Any>> {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> Pair(R.string.event_detail_time_ago_now, emptyList())
        minutes < 60 -> Pair(R.string.event_detail_time_ago_minutes, listOf(minutes))
        hours < 24 -> Pair(R.string.event_detail_time_ago_hours, listOf(hours))
        else -> Pair(R.string.event_detail_time_ago_days, listOf(days))
    }
}