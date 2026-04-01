package com.miempresa.comuniapp.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

@Composable
fun HomeScreen(
    onLoginClick: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_comunidad),
                contentDescription = "Logo de la aplicación",
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ComuniApp",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Plataforma para descubrir, crear y participar en eventos comunitarios.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors()
            ) {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
