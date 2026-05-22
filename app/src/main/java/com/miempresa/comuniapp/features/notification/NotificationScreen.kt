package com.miempresa.comuniapp.features.notification

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.NotificationItem
import com.miempresa.comuniapp.domain.model.NotificationType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Paleta de colores local ──────────────────────────────────────────────────
private val VerdePrimario   = Color(0xFF2E7D32)
private val VerdeClaro      = Color(0xFFE8F5E9)
private val AzulInfo        = Color(0xFF1565C0)
private val AzulClaro       = Color(0xFFE3F2FD)
private val NaranjaAlerta   = Color(0xFFE65100)
private val NaranjaClaro    = Color(0xFFFFF3E0)
private val RojoError       = Color(0xFFC62828)
private val RojoClaro       = Color(0xFFFFEBEE)
private val GrisFondo       = Color(0xFFF7F7F7)
private val GrisTexto       = Color(0xFF757575)
private val GrisTarjeta     = Color(0xFFFAFAFA)
private val TextoPrincipal  = Color(0xFF1A1A1A)

/**
 * Pantalla de historial de notificaciones del usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    paddingValues: PaddingValues,
    onNotificationClick: (NotificationItem) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount   by viewModel.unreadCount.collectAsState()
    val actionResult  by viewModel.actionResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Muestra Snackbar cuando hay un error de acción
    LaunchedEffect(actionResult) {
        if (actionResult is RequestResult.Failure) {
            snackbarHostState.showSnackbar(
                (actionResult as RequestResult.Failure).errorMessage
            )
            viewModel.resetActionResult()
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = GrisFondo,
        topBar = {
            NotificationTopBar(
                unreadCount   = unreadCount,
                onMarkAllRead = { viewModel.markAllAsRead() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (notifications.isEmpty()) {
                EstadoVacio()
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical   = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = notifications,
                        key   = { it.id }
                    ) { notificacion ->

                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart ||
                                dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.deleteNotification(notificacion.id)
                            }
                        }

                        SwipeToDismissBox(
                            state             = dismissState,
                            backgroundContent = { FondoSwipe(dismissState.dismissDirection) }
                        ) {
                            NotificationCard(
                                notificacion = notificacion,
                                onClick      = {
                                    scope.launch {
                                        // 💡 Se marca como leída secuencialmente ÚNICAMENTE al seleccionarla
                                        viewModel.markAsReadSuspended(notificacion.id)
                                        onNotificationClick(notificacion)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra superior con título, conteo de no leídas y botón "marcar todo".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    unreadCount: Int,
    onMarkAllRead: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text       = stringResource(R.string.notifications_title),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = TextoPrincipal
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(VerdePrimario),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize  = 10.sp,
                            color     = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        actions = {
            if (unreadCount > 0) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        text      = stringResource(R.string.notifications_mark_all_read),
                        color     = VerdePrimario,
                        fontSize  = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

/**
 * Tarjeta de notificación individual con mapeo semántico exhaustivo.
 */
@Composable
private fun NotificationCard(
    notificacion: NotificationItem,
    onClick: () -> Unit
) {
    val (colorFondo, colorIcono) = coloresPorTipo(notificacion.type, notificacion.isRead)
    val iconoTipo = iconoPorTipo(notificacion.type)

    val fondoAnimado by animateColorAsState(
        targetValue   = colorFondo,
        animationSpec = tween(durationMillis = 300),
        label         = "fondo_notificacion"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = fondoAnimado),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notificacion.isRead) 0.dp else 1.5.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = iconoTipo,
                    contentDescription = null,
                    tint               = colorIcono,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = notificacion.title,
                    fontWeight = if (notificacion.isRead) FontWeight.Normal else FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = TextoPrincipal,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = notificacion.body,
                    fontSize = 13.sp,
                    color    = GrisTexto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = formatearTiempo(notificacion.timestamp),
                    fontSize = 11.sp,
                    color    = GrisTexto.copy(alpha = 0.7f)
                )
            }

            if (!notificacion.isRead) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(VerdePrimario)
                )
            }
        }
    }
}

/**
 * Fondo para la acción de Swipe-to-dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FondoSwipe(direction: SwipeToDismissBoxValue) {
    val alineacion = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled    -> Alignment.CenterEnd
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(RojoError),
        contentAlignment = alineacion
    ) {
        Icon(
            imageVector        = Icons.Default.Delete,
            contentDescription = stringResource(R.string.notifications_delete),
            tint               = Color.White,
            modifier           = Modifier
                .padding(horizontal = 20.dp)
                .size(26.dp)
        )
    }
}

/**
 * Estado vacío.
 */
@Composable
private fun EstadoVacio() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Outlined.Notifications,
                contentDescription = null,
                tint               = GrisTexto.copy(alpha = 0.4f),
                modifier           = Modifier.size(80.dp)
            )
            Text(
                text       = stringResource(R.string.notifications_empty_title),
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = GrisTexto
            )
            Text(
                text      = stringResource(R.string.notifications_empty_body),
                fontSize  = 14.sp,
                color     = GrisTexto.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ── Helpers de presentación ──────────────────────────────────────────────────

@Composable
private fun coloresPorTipo(
    tipo: NotificationType,
    isRead: Boolean
): Pair<Color, Color> {
    if (isRead) return GrisTarjeta to GrisTexto
    return when (tipo) {
        NotificationType.EVENT_APPROVED,
        NotificationType.EVENT_CREATED,
        NotificationType.EVENT_FEATURED,
        NotificationType.LEVEL_UP,
        NotificationType.BADGE_UNLOCKED,
        NotificationType.POINTS_EARNED,
        NotificationType.STREAK_REWARD,
        NotificationType.WAITLIST_AVAILABLE  -> VerdeClaro   to VerdePrimario

        NotificationType.EVENT_REJECTED,
        NotificationType.EVENT_CANCELLED,
        NotificationType.REPORT_RECEIVED     -> RojoClaro    to RojoError

        NotificationType.NEW_COMMENT,
        NotificationType.COMMENT_REPLY       -> AzulClaro    to AzulInfo

        NotificationType.NEW_REACTION,
        NotificationType.NEW_PARTICIPANT,
        NotificationType.EVENT_SHARED,
        NotificationType.LOGIN_DETECTED,
        NotificationType.PASSWORD_CHANGED,
        NotificationType.ACCOUNT_UPDATED     -> AzulClaro    to AzulInfo

        NotificationType.EVENT_REMINDER,
        NotificationType.EVENT_STARTING_SOON,
        NotificationType.EVENT_UNDER_REVIEW,
        NotificationType.EVENT_UPDATED       -> NaranjaClaro to NaranjaAlerta

        NotificationType.NEW_INTEREST,
        NotificationType.PARTICIPANT_LEFT    -> VerdeClaro   to VerdePrimario

        NotificationType.GENERAL,
        NotificationType.EVENT_FULL          -> GrisTarjeta  to GrisTexto
    }
}

private fun iconoPorTipo(tipo: NotificationType): ImageVector = when (tipo) {
    NotificationType.EVENT_APPROVED,
    NotificationType.EVENT_FEATURED -> Icons.Default.CheckCircle

    NotificationType.EVENT_REJECTED,
    NotificationType.EVENT_CANCELLED -> Icons.Default.Cancel

    NotificationType.NEW_COMMENT,
    NotificationType.COMMENT_REPLY -> Icons.AutoMirrored.Filled.Comment

    NotificationType.NEW_INTEREST,
    NotificationType.NEW_REACTION -> Icons.Default.Favorite

    NotificationType.EVENT_REMINDER,
    NotificationType.EVENT_STARTING_SOON -> Icons.Default.Alarm

    NotificationType.BADGE_UNLOCKED,
    NotificationType.LEVEL_UP,
    NotificationType.POINTS_EARNED,
    NotificationType.STREAK_REWARD -> Icons.Default.Star

    NotificationType.NEW_PARTICIPANT -> Icons.Default.PersonAdd

    NotificationType.PARTICIPANT_LEFT -> Icons.Default.PersonRemove

    NotificationType.EVENT_SHARED -> Icons.Default.Share

    NotificationType.EVENT_UPDATED,
    NotificationType.ACCOUNT_UPDATED -> Icons.Default.Edit

    NotificationType.LOGIN_DETECTED,
    NotificationType.PASSWORD_CHANGED -> Icons.Default.Lock

    NotificationType.REPORT_RECEIVED -> Icons.Default.Flag

    NotificationType.WAITLIST_AVAILABLE -> Icons.Default.Queue

    NotificationType.EVENT_CREATED,
    NotificationType.EVENT_UNDER_REVIEW,
    NotificationType.EVENT_FULL,
    NotificationType.GENERAL -> Icons.Default.Notifications
}

private fun formatearTiempo(timestamp: Long): String {
    val diff    = System.currentTimeMillis() - timestamp
    val minutos = diff / 60_000
    val horas   = minutos / 60
    val dias    = horas / 24

    return when {
        minutos < 1  -> "Justo ahora"
        minutos < 60 -> "Hace $minutos min"
        horas < 24   -> "Hace $horas h"
        dias == 1L   -> "Ayer"
        dias < 7     -> "Hace $dias días"
        else -> SimpleDateFormat("d MMM", Locale.Builder().setLanguage("es").setRegion("CO").build())
            .format(Date(timestamp))
    }
}