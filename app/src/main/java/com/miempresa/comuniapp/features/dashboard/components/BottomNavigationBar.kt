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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    titleTopBar: (String) -> Unit
){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(currentDestination) {
        val destination = Destination.items.find {
            currentDestination?.hasRoute(it.route::class) == true
        }
        if (destination != null) {
            titleTopBar(destination.label)
        }
    }

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
    ){
        Destination.items.forEach { destination ->
            val isSelected = currentDestination?.hasRoute(destination.route::class) == true

            NavigationBarItem(
                label = { Text(text = destination.label) },
                onClick = {
                    navController.navigate(destination.route){
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
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

sealed class Destination(
    val route: Any,
    val label: String,
    val icon: ImageVector
){
    data object HOME : Destination(DashboardRoutes.EventList, "Eventos", Icons.Default.Home )
    data object SEARCH : Destination(DashboardRoutes.Search, "Buscar", Icons.Default.Search)
    data object PROFILE : Destination(DashboardRoutes.Profile, "Perfil", Icons.Default.AccountCircle)
    
    companion object {
        val items = listOf(HOME, SEARCH, PROFILE)
    }
}
