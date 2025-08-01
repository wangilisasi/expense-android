package com.example.expensemanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensemanager.ui.screens.*
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel

@Composable
fun AppNavHost() {
    val rootNavController = rememberNavController()
    NavHost(rootNavController, startDestination = Screen.Onboarding.route) {
        composable(Screen.Onboarding.route) {
            OnBoardingScreen(onContinue = { rootNavController.navigate(Screen.Login.route) })
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { rootNavController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { rootNavController.navigate(Screen.Login.route) },
                onRegistered = {
                    rootNavController.popBackStack()
                })
        }
        composable(Screen.Main.route) {
            MainScreen(rootNavController)
        }
    }
}