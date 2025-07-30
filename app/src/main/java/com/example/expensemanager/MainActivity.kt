// MainActivity.kt
package com.example.expensemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.expensemanager.ui.screens.HomeScreen
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.utils.Screen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.ui.screens.BudgetSetupScreen
import com.example.expensemanager.ui.screens.ExpenseListScreen
import com.example.expensemanager.ui.viewmodels.ExpenseListNavigationEvent
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseManagerTheme {
                val navController = rememberNavController()

                // Observe AuthState so we can force‐logout back to AuthActivity
                val authViewModel: AuthViewModel = hiltViewModel()
                val expenseListViewModel: ExpenseListViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()
                val navigationEvents by expenseListViewModel.navigationEvents.collectAsState(null)
                val ctx = LocalContext.current
                LaunchedEffect(authState) {
                    if (authState == AuthState.Unauthenticated) {
                        ctx.startActivity(Intent(ctx, AuthActivity::class.java))
                        finish()
                    }
                }
                LaunchedEffect(navigationEvents) {
                    when (navigationEvents) {
                        is ExpenseListNavigationEvent.NavigateToBudgetSetup -> {
                            navController.navigate(Screen.BudgetSetup.route)
                        }
                        is ExpenseListNavigationEvent.NavigateToHome -> {
                            navController.navigate(Screen.Home.route)
                        }
                        else->{
                            // Handle other navigation events if needed
                        }
                    }
                }

                // Bottom nav only on Home & Dashboard
                val bottomItems = listOf(Screen.Home, Screen.Reports)
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route
                val showBottomBar = currentRoute in bottomItems.map { it.route }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, null) },
                                        label = { Text(screen.title) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onLogout = { authViewModel.logout() },
                            )
                        }
                        composable(Screen.Reports.route) {
                            ExpenseListScreen()
                        }
                        composable(Screen.BudgetSetup.route) {
                            BudgetSetupScreen(
                                onFormSubmit = {budgetName, amount, startDate, endDate ->
                                    expenseListViewModel.createBudget(budgetName,amount.toDoubleOrNull()?:0.0,startDate,endDate)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}