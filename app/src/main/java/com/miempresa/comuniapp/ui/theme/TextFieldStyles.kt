package com.miempresa.comuniapp.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(

    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,

    focusedBorderColor = TextPrimary,
    unfocusedBorderColor = BorderLight,

    focusedLabelColor = TextPrimary,
    unfocusedLabelColor = TextSecondary,

    cursorColor = TextPrimary,

    focusedLeadingIconColor = TextPrimary,
    unfocusedLeadingIconColor = TextSecondary
)