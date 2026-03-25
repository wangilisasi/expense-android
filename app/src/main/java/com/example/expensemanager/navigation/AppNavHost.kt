package com.example.expensemanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensemanager.ui.screens.*
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel

@Composable
fun AppNavHost() {
    val rootNavController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(authState, currentRoute) {
        when (authState) {
            AuthState.Unknown -> Unit
            AuthState.Authenticated -> {
                if (currentRoute in setOf(
                        null,
                        Screen.Splash.route,
                        Screen.Login.route,
                        Screen.Register.route,
                        Screen.Onboarding.route
                    )
                ) {
                    rootNavController.navigate(Screen.Main.route) {
                        popUpTo(rootNavController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            AuthState.Unauthenticated -> {
                if (currentRoute !in setOf(Screen.Login.route, Screen.Register.route)) {
                    rootNavController.navigate(Screen.Login.route) {
                        popUpTo(rootNavController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(rootNavController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen()
        }
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
