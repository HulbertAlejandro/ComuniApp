package com.miempresa.comuniapp.features.dashboard.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    titleTopBar: (String) -> Unit,
    items: List<Destination> = Destination.userItems
) {
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(currentDestination) {
        items.find { currentDestination?.hasRoute(it.route::class) == true }
            ?.let { titleTopBar(it.label) }
    }

    NavigationBar(
        modifier       = Modifier.fillMaxWidth(),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { destination ->
            val isSelected = currentDestination?.hasRoute(destination.route::class) == true

            NavigationBarItem(
                label = {
                    Text(
                        text  = destination.label,
                        fontSize = 10.sp,
                        color = if (isSelected) Color(0xFF1565C0) else Color(0xFF9E9E9E)
                    )
                },
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon = {
                    Icon(
                        imageVector        = if (isSelected) destination.selectedIcon
                        else destination.unselectedIcon,
                        contentDescription = destination.label
                    )
                },
                selected = isSelected,
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Color(0xFF1565C0),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    selectedTextColor   = Color(0xFF1565C0),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor      = Color.Transparent
                )
            )
        }
    }
}

sealed class Destination(
    val route         : Any,
    val label         : String,
    val selectedIcon  : ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Destination(
        DashboardRoutes.EventList, "Inicio",
        Icons.Default.Home, Icons.Outlined.Home
    )
    data object Map : Destination(
        DashboardRoutes.Map, "Mapa",
        Icons.Outlined.Map, Icons.Outlined.Map
    )
    data object Create : Destination(
        DashboardRoutes.CreateEvent, "Crear",
        Icons.Default.AddCircle, Icons.Outlined.AddCircleOutline
    )
    data object Notifications : Destination(
        DashboardRoutes.Notifications, "Alertas",
        Icons.Outlined.Notifications, Icons.Outlined.Notifications
    )
    data object Profile : Destination(
        DashboardRoutes.Profile, "Perfil",
        Icons.Outlined.AccountCircle, Icons.Outlined.AccountCircle
    )
    data object AdminHome : Destination(
        DashboardRoutes.AdminDashboard, "Panel",
        Icons.Outlined.Dashboard, Icons.Outlined.Dashboard
    )
    data object AdminPublications : Destination(
        DashboardRoutes.ManagePublications, "Publicaciones",
        Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Outlined.LibraryBooks
    )
    data object AdminHistory : Destination(
        DashboardRoutes.ModerationHistory, "Historial",
        Icons.Outlined.History, Icons.Outlined.History
    )
    data object AdminProfile : Destination(
        DashboardRoutes.Profile, "Perfil",
        Icons.Outlined.AccountCircle, Icons.Outlined.AccountCircle
    )

    companion object {
        val userItems  = listOf(Home, Map, Create, Notifications, Profile)
        val adminItems = listOf(AdminHome, AdminPublications, AdminHistory, AdminProfile)
    }
}