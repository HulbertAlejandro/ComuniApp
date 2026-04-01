package com.miempresa.comuniapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.miempresa.comuniapp.core.navigation.AppNavigation
import com.miempresa.comuniapp.ui.theme.ComuniAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComuniAppTheme {
                AppNavigation()
            }
        }
    }
}