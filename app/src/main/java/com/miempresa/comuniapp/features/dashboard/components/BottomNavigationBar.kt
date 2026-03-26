package com.miempresa.comuniapp.features.dashboard.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    titleTopBar: (String) -> Unit
){
    // Obtener la entrada actual de la pila de navegación
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Actualizar el título de la barra superior según la pantalla actual
    LaunchedEffect(currentDestination) {
        val destination = Destination.items.find {
            currentDestination?.route?.contains(it.route::class.simpleName ?: "") == true
        }
        if (destination != null) {
            titleTopBar(destination.label)
        }
    }

    // Crear la barra de navegación inferior
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ){
        // Iteramos cada item de navegación definido en Destination
        Destination.items.forEachIndexed { index: Int, destination: Destination ->

            // Verificar si el item está seleccionado
            val isSelected = when (destination.route) {
                is DashboardRoutes.HomeUser -> currentDestination?.route?.contains("HomeUser") == true
                is DashboardRoutes.Search -> currentDestination?.route?.contains("Search") == true
                is DashboardRoutes.Profile -> currentDestination?.route?.contains("Profile") == true
                else -> false
            }

            NavigationBarItem(
                label = {
                    // Etiqueta del item de navegación
                    Text(
                        text = destination.label
                    )
                },
                onClick = {
                    // Navegar a la ruta correspondiente al item seleccionado
                    navController.navigate(destination.route){
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    // Icono del item de navegación
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                selected = isSelected
            )
        }
    }
}

// Definición de los items de navegación de la barra inferior
sealed class Destination(
    val route: DashboardRoutes,
    val label: String,
    val icon: ImageVector
){
    data object HOME : Destination(DashboardRoutes.HomeUser, "Home", Icons.Default.Home )
    data object SEARCH : Destination(DashboardRoutes.Search, "Buscar", Icons.Default.Search)
    data object PROFILE : Destination(DashboardRoutes.Profile, "Perfil", Icons.Default.AccountCircle)
    
    companion object {
        val items = listOf(HOME, SEARCH, PROFILE)
    }
}
