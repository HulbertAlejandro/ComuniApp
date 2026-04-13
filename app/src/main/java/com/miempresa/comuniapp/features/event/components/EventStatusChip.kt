package com.miempresa.comuniapp.features.event.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.comuniapp.domain.model.EventStatus

@Composable
fun EventStatusChip(
    status: EventStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        EventStatus.ACTIVE -> Color(0xFF2E7D32) to "ACTIVO"
        EventStatus.FULL -> Color(0xFFC62828) to "LLENO"
        EventStatus.CREATED -> Color(0xFFE65100) to "PENDIENTE"
        EventStatus.FINISHED -> Color(0xFF424242) to "FINALIZADO"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
