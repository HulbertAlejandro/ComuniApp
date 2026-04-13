package com.miempresa.comuniapp.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable

@Composable
fun appPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PrimaryBlue,
    contentColor = SurfaceWhite,
    disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f),
    disabledContentColor = SurfaceWhite.copy(alpha = 0.5f)
)