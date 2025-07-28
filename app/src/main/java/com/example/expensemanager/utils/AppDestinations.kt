package com.example.expensemanager.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Screens for AuthActivity
    data object Login : Screen("login_screen", "Login", Icons.Default.Lock)
    data object Register : Screen("register_screen", "Register", Icons.Default.LocationOn)

    // Screens for MainActivity (Bottom Nav)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Reports : Screen("expenses", "Expenses", Icons.Default.Info)
}

object NavRoutes {
    const val AUTH_GRAPH_ROUTE = "auth_graph"
}
