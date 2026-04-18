package com.miempresa.comuniapp.features.admin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AdminDashboardScreen(
    paddingValues : PaddingValues,
    viewModel     : AdminDashboardViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()

    // ✅ Centrado vertical y horizontal — el resumen es el único foco
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text          = "Resumen general",
                fontSize      = 14.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = Color(0xFF757575),
                letterSpacing = 0.5.sp
            )

            // Fila superior
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier       = Modifier.weight(1f),
                    label          = "Pendientes",
                    count          = stats.pendingCount,
                    icon           = Icons.Outlined.HourglassEmpty,
                    iconColor      = Color(0xFFE65100),
                    containerColor = Color(0xFFFFF3E0)
                )
                StatCard(
                    modifier       = Modifier.weight(1f),
                    label          = "Aprobados",
                    count          = stats.approvedCount,
                    icon           = Icons.Outlined.CheckCircle,
                    iconColor      = Color(0xFF2E7D32),
                    containerColor = Color(0xFFE8F5E9)
                )
            }

            // Fila inferior
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier       = Modifier.weight(1f),
                    label          = "Activos",
                    count          = stats.activeCount,
                    icon           = Icons.Outlined.PlayCircle,
                    iconColor      = Color(0xFF1565C0),
                    containerColor = Color(0xFFE3F2FD)
                )
                StatCard(
                    modifier       = Modifier.weight(1f),
                    label          = "Rechazados",
                    count          = stats.rejectedCount,
                    icon           = Icons.Outlined.Cancel,
                    iconColor      = Color(0xFFC62828),
                    containerColor = Color(0xFFFFEBEE)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier       : Modifier,
    label          : String,
    count          : Int,
    icon           : ImageVector,
    iconColor      : Color,
    containerColor : Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(28.dp)
            )
            Text(
                text       = count.toString(),
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF212121)
            )
            Text(
                text     = label,
                fontSize = 13.sp,
                color    = Color(0xFF757575)
            )
        }
    }
}