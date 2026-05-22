package com.miempresa.comuniapp.features.dashboard.user

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.features.dashboard.components.BottomNavigationBar
import com.miempresa.comuniapp.features.dashboard.components.TopAppBar
import com.miempresa.comuniapp.features.dashboard.navigation.DashboardRoutes
import com.miempresa.comuniapp.features.dashboard.navigation.UserNavigation

@Composable
fun UserScreen(
    onLogout: () -> Unit,
    initialEventId: String? = null
) {

    val navController = rememberNavController()

    val defaultTitle = stringResource(R.string.bottom_nav_home)

    var title by remember {
        mutableStateOf(defaultTitle)
    }

    var showTopBar by remember {
        mutableStateOf(false)
    }

    var showBottomBar by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(initialEventId) {

        if (!initialEventId.isNullOrBlank()) {

            navController.navigate(
                DashboardRoutes.EventDetail(initialEventId)
            )
        }
    }

    LaunchedEffect(navController) {

        navController.currentBackStackEntryFlow.collect { backStackEntry ->

            val dest = backStackEntry.destination

            showTopBar =
                !dest.hasRoute<DashboardRoutes.EventList>() &&
                        !dest.hasRoute<DashboardRoutes.Map>() &&
                        !dest.hasRoute<DashboardRoutes.CreateEvent>() &&
                        !dest.hasRoute<DashboardRoutes.EventDetail>() &&
                        !dest.hasRoute<DashboardRoutes.Profile>() &&
                        !dest.hasRoute<DashboardRoutes.UserEdit>() &&
                        !dest.hasRoute<DashboardRoutes.MyEvents>() &&
                        !dest.hasRoute<DashboardRoutes.SavedEvents>() &&
                        !dest.hasRoute<DashboardRoutes.Achievements>() &&
                        !dest.hasRoute<DashboardRoutes.EditEvent>() &&
                        !dest.hasRoute<DashboardRoutes.History>() &&
                        !dest.hasRoute<DashboardRoutes.Notifications>()

            showBottomBar =
                !dest.hasRoute<DashboardRoutes.EventDetail>() &&
                        !dest.hasRoute<DashboardRoutes.CreateEvent>() &&
                        !dest.hasRoute<DashboardRoutes.UserEdit>() &&
                        !dest.hasRoute<DashboardRoutes.MyEvents>() &&
                        !dest.hasRoute<DashboardRoutes.SavedEvents>() &&
                        !dest.hasRoute<DashboardRoutes.Achievements>() &&
                        !dest.hasRoute<DashboardRoutes.EditEvent>() &&
                        !dest.hasRoute<DashboardRoutes.History>()
        }
    }

    Scaffold(

        topBar = {

            if (showTopBar) {

                TopAppBar(
                    title = title,
                    onLogout = onLogout
                )
            }
        },

        bottomBar = {

            if (showBottomBar) {

                BottomNavigationBar(
                    navController = navController,
                    titleTopBar = { title = it }
                )
            }
        }

    ) { padding ->

        UserNavigation(
            navController = navController,
            padding = padding,
            onLogout = onLogout
        )
    }
}