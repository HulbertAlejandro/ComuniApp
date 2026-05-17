package com.miempresa.comuniapp.features.user.myevents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.features.event.components.EventCard

/**
 * Pantalla de gestión de eventos propios del usuario.
 *
 * Organiza los eventos en cuatro pestañas:
 * - Creados:    pendientes de aprobación.
 * - Activos:    aprobados y disponibles.
 * - Finalizados: ya ocurrieron.
 * - Rechazados:  con motivo de rechazo visible.
 *
 * @param paddingValues Padding del Scaffold padre (BottomNav).
 * @param onEventClick  Callback de navegación al detalle del evento.
 * @param onEditEvent   Callback de navegación a la edición del evento.
 * @param viewModel     ViewModel inyectado por Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(
    paddingValues: PaddingValues,
    onEventClick: (String) -> Unit,
    onEditEvent: (String) -> Unit,
    viewModel: MyEventsViewModel = hiltViewModel()
) {
    val createdEvents  by viewModel.createdEvents.collectAsState()
    val activeEvents   by viewModel.activeEvents.collectAsState()
    val finishedEvents by viewModel.finishedEvents.collectAsState()
    val rejectedEvents by viewModel.rejectedEvents.collectAsState()
    val userInterests  by viewModel.userInterests.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.my_events_tab_created),
        stringResource(R.string.my_events_tab_active),
        stringResource(R.string.my_events_tab_finished),
        stringResource(R.string.my_events_tab_rejected)
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.my_events_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color     = Color(0xFF212121),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                )

                // ScrollableTabRow para acomodar las 4 pestañas sin recortes
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier         = Modifier.fillMaxWidth(),
                    containerColor   = Color.White,
                    contentColor     = MaterialTheme.colorScheme.primary,
                    edgePadding      = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick  = { selectedTabIndex = index },
                            text     = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> MyEventsTabContent(
                    events             = createdEvents,
                    onEventClick       = onEventClick,
                    onEditEvent        = onEditEvent,
                    onFinishEvent      = { viewModel.finishEvent(it) },
                    showEditButton     = true,
                    viewModel          = viewModel,
                    showInterestButton = false,
                    userInterests      = userInterests
                )
                1 -> MyEventsTabContent(
                    events             = activeEvents,
                    onEventClick       = onEventClick,
                    onEditEvent        = onEditEvent,
                    onFinishEvent      = { viewModel.finishEvent(it) },
                    showEditButton     = false,
                    viewModel          = viewModel,
                    showFinishButton   = true,
                    userInterests      = userInterests
                )
                2 -> MyEventsTabContent(
                    events             = finishedEvents,
                    onEventClick       = onEventClick,
                    onEditEvent        = onEditEvent,
                    onFinishEvent      = {},
                    showEditButton     = false,
                    viewModel          = viewModel,
                    showInterestButton = false,
                    userInterests      = userInterests
                )
                3 -> MyEventsTabContent(
                    events             = rejectedEvents,
                    onEventClick       = onEventClick,
                    onEditEvent        = onEditEvent,
                    onFinishEvent      = {},
                    showEditButton     = false,
                    viewModel          = viewModel,
                    showInterestButton = false,
                    userInterests      = userInterests,
                    isRejectedTab      = true
                )
            }
        }
    }
}

/**
 * Contenido de una pestaña de "Mis Eventos".
 *
 * Muestra los eventos de la pestaña activa o un mensaje vacío.
 * Si [isRejectedTab] es true, muestra el motivo de rechazo bajo cada tarjeta.
 *
 * @param events             Lista de eventos de esta pestaña.
 * @param onEventClick       Callback de navegación al detalle.
 * @param onEditEvent        Callback de navegación a la edición.
 * @param onFinishEvent      Callback para marcar el evento como finalizado.
 * @param showEditButton     true si se muestra el botón de edición superpuesto.
 * @param viewModel          ViewModel para alternar interés y acceder al mapa de usuarios.
 * @param showInterestButton true si se muestra el botón de "me interesa" en la tarjeta.
 * @param showFinishButton   true si se muestra el botón de "Finalizar" superpuesto.
 * @param userInterests      Conjunto de IDs de eventos marcados por el usuario.
 * @param isRejectedTab      true para mostrar el motivo de rechazo bajo cada tarjeta.
 */
@Composable
fun MyEventsTabContent(
    events: List<Event>,
    onEventClick: (String) -> Unit,
    onEditEvent: (String) -> Unit,
    onFinishEvent: (String) -> Unit,
    showEditButton: Boolean,
    viewModel: MyEventsViewModel,
    showInterestButton: Boolean = true,
    showFinishButton: Boolean = false,
    userInterests: Set<String>,
    isRejectedTab: Boolean = false   // ✅ Una sola definición con valor por defecto
) {
    if (events.isEmpty()) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = stringResource(R.string.my_events_empty_message),
                fontSize  = 15.sp,
                color     = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(events, key = { it.id }) { event ->
                Column {
                    MyEventCardWrapper(
                        event              = event,
                        onEventClick       = { onEventClick(event.id) },
                        onEditClick        = { onEditEvent(event.id) },
                        showEditButton     = showEditButton,
                        viewModel          = viewModel,
                        showInterestButton = showInterestButton,
                        userInterests      = userInterests,
                        onFinishEvent      = onFinishEvent,
                        showFinishButton   = showFinishButton
                    )

                    // Motivo de rechazo visible solo en la pestaña de rechazados
                    if (isRejectedTab && !event.rejectionReason.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier          = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text       = stringResource(R.string.my_events_rejection_reason_label),
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFFC62828),
                                    fontSize   = 12.sp
                                )
                                Text(
                                    text     = event.rejectionReason!!,
                                    color    = Color(0xFFC62828),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wrapper de tarjeta de evento para "Mis Eventos".
 *
 * Superpone botones de acción (Editar / Finalizar) sobre la tarjeta base.
 * Delega la lógica de interés y los datos del organizador al [viewModel].
 */
@Composable
fun MyEventCardWrapper(
    event: Event,
    onEventClick: () -> Unit,
    onEditClick: () -> Unit,
    showEditButton: Boolean,
    viewModel: MyEventsViewModel,
    showInterestButton: Boolean,
    userInterests: Set<String>,
    onFinishEvent: (String) -> Unit,
    showFinishButton: Boolean = false
) {
    val usersMap  by viewModel.usersMap.collectAsState()
    val organizer = usersMap[event.ownerId]

    Box(modifier = Modifier.fillMaxWidth()) {
        EventCard(
            event             = event,
            organizer         = organizer,
            hasVoted          = if (showInterestButton) userInterests.contains(event.id) else false,
            onInterestedClick = if (showInterestButton) {
                { viewModel.toggleInterest(event.id) }
            } else {
                {}
            },
            modifier          = Modifier.clickable { onEventClick() },
            showInterestButton = showInterestButton
        )

        // Botón "Editar" superpuesto en esquina inferior derecha
        if (showEditButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onEditClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text       = stringResource(R.string.my_events_edit_label),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }

        // Botón "Finalizar" superpuesto en esquina inferior derecha
        if (showFinishButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onFinishEvent(event.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text       = stringResource(R.string.my_events_finish_label),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }
    }
}