package com.example.expensemanager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Splash Screen
    data object Splash : Screen("splash_screen", "Splash", Icons.Default.LocationOn)
    data object Login : Screen("login_screen", "Login", Icons.Default.Lock)
    data object Register : Screen("register_screen", "Register", Icons.Default.LocationOn)
    data object Onboarding : Screen("onboarding_screen", "Onboarding", Icons.Default.LocationOn)
    data object Main : Screen("main_screen", "Main", Icons.Default.LocationOn)
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home_screen", "Home", Icons.Default.Home)
    data object Reports : BottomNavItem("reports_screen", "Reports", Icons.Default.Info)
    data object BudgetSetup : BottomNavItem("budget_setup_screen", "Budget Setup", Icons.Default.Info)
}
