package com.miempresa.comuniapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    
    secondary = SecondaryBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color.Black,
    
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    
    background = BackgroundLight,
    onBackground = Color.Black,
    
    surface = Color.White,
    onSurface = Color.Black,
    
    // Forzamos surfaceVariant y surfaceTint para eliminar CUALQUIER rastro de morado
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.DarkGray,
    surfaceTint = Color.Transparent, 
    
    outline = BorderColor,
    outlineVariant = Color(0xFFF0F0F0),
    
    error = ErrorRed,
    onError = Color.White,

    // M3 Surface Container tokens - Forzamos blanco puro para diálogos y menús
    surfaceBright = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,
)

@Composable
fun ComuniAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
