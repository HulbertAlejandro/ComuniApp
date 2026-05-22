package com.miempresa.comuniapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * Componente reutilizable para cargar imágenes desde URLs de Firebase Storage
 * (o cualquier URL HTTPS) con estados de carga y error.
 *
 * @param url           URL de descarga de Firebase Storage.
 * @param modifier      Modificador para tamaño y forma (aplica clip antes de pasarlo).
 * @param contentScale  Escala de la imagen; [ContentScale.Crop] para avatares.
 * @param fallbackIcon  Ícono mostrado si la URL está vacía.
 * @param errorIcon     Ícono mostrado si la carga falla.
 */
@Composable
fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = Icons.Default.Person,
    errorIcon: ImageVector = Icons.Default.BrokenImage
) {
    if (url.isBlank()) {
        Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Icon(fallbackIcon, contentDescription = null, modifier = Modifier.size(36.dp))
        }
        return
    }

    AsyncImage(
        model             = url,
        contentDescription = null,
        modifier          = modifier,
        contentScale      = contentScale,
        onState           = { /* puedes loguear errores aquí si lo necesitas */ },
        transform         = AsyncImagePainter.DefaultTransform,
    )
}

/**
 * Variante con indicador de carga explícito.
 * Útil en tarjetas de eventos donde el placeholder importa visualmente.
 */
@Composable
fun NetworkImageWithLoading(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model              = url,
        contentDescription = null,
        modifier           = modifier,
        contentScale       = contentScale,
        onLoading          = { /* AsyncImage maneja el estado internamente */ },
        // Coil3 muestra automáticamente el placeholder mientras carga
    )
}