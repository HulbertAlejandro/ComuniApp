package com.miempresa.comuniapp.features.event.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.miempresa.comuniapp.domain.model.EventStatus

@Composable
fun EventStatusChip(status: EventStatus) {

    val (color, text) = when (status) {
        EventStatus.ACTIVE -> Color(0xFF4CAF50) to "Activo"
        EventStatus.FULL -> Color.Red to "Lleno"
        EventStatus.FINISHED -> Color.Gray to "Finalizado"
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color
        )
    }
}