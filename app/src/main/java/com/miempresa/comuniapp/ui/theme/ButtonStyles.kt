package com.miempresa.comuniapp.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable

@Composable
fun appPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = ButtonBackground,
    contentColor = ButtonTextDark,
    disabledContainerColor = ButtonBackground.copy(alpha = 0.5f),
    disabledContentColor = ButtonTextDark.copy(alpha = 0.5f)
)