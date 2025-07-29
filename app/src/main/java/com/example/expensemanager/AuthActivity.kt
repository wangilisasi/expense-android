// AuthActivity.kt
package com.example.expensemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.example.expensemanager.ui.screens.LoginScreen
import com.example.expensemanager.ui.screens.RegisterScreen
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.utils.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseManagerTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()

                // When user becomes Authenticated, start MainActivity and finish this one
                val ctx = LocalContext.current
                LaunchedEffect(authState) {
                    if (authState == AuthState.Authenticated) {
                        ctx.startActivity(Intent(ctx, MainActivity::class.java))
                        finish()
                    }
                }

                // NavHost local to AuthActivity
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            viewModel = authViewModel,
                            onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                        )
                    }
                    composable(Screen.Register.route) {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}