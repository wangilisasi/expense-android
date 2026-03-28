package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensemanager.navigation.BottomNavItem
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListNavigationEvent
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import com.example.expensemanager.ui.viewmodels.TrackerSessionState

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val expenseListViewModel: ExpenseListViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val authState by authViewModel.authState.collectAsState()
    val currentTrackerDetails by expenseListViewModel.statsUiState.collectAsState()
    val uiState by expenseListViewModel.uiState.collectAsState()
    val sessionState by expenseListViewModel.sessionState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated -> expenseListViewModel.bootstrapSession()
            AuthState.Unauthenticated -> expenseListViewModel.clearSession()
            AuthState.Unknown -> Unit
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState == TrackerSessionState.RequiresLogin) {
            authViewModel.logout()
        }
    }

    when (val state = sessionState) {
        TrackerSessionState.Idle,
        TrackerSessionState.Loading,
        TrackerSessionState.RequiresLogin -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is TrackerSessionState.Error -> {
            SessionErrorScreen(
                message = state.message,
                onRetry = {
                    expenseListViewModel.bootstrapSession(force = true)
                }
            )
        }

        TrackerSessionState.ActiveBudget,
        TrackerSessionState.NoActiveBudget -> {
            val startDestination = if (state == TrackerSessionState.ActiveBudget) {
                BottomNavItem.Home.route
            } else {
                BottomNavItem.BudgetSetup.route
            }

            key(startDestination) {
                val mainScreenNavController = rememberNavController()

                LaunchedEffect(expenseListViewModel, mainScreenNavController) {
                    expenseListViewModel.navigationEvents.collect { event ->
                        when (event) {
                            ExpenseListNavigationEvent.NavigateToHome -> {
                                mainScreenNavController.navigateToBottomTab(BottomNavItem.Home.route)
                            }
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        BottomBar(
                            navController = mainScreenNavController,
                            hasActiveBudget = state == TrackerSessionState.ActiveBudget
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        mainScreenNavController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(BottomNavItem.Home.route) {
                            DashBoardScreen(
                                expenseViewModel = expenseListViewModel,
                                rootNavController = rootNavController,
                            )
                        }
                        composable(BottomNavItem.Analytics.route) {
                            AnalyticsScreen(
                                expenseViewModel = expenseListViewModel,
                                rootNavController = rootNavController,
                            )
                        }
                        composable(BottomNavItem.BudgetSetup.route) {
                            BudgetSetupScreen(
                                onFormSubmit = { name, budget, startDate, endDate ->
                                    val parsedBudget = budget.toDoubleOrNull()
                                    if (parsedBudget != null) {
                                        expenseListViewModel.submitBudget(
                                            name = name,
                                            budget = parsedBudget,
                                            startDate = startDate,
                                            endDate = endDate
                                        )
                                    }
                                },
                                initialName = uiState.activeTracker?.name.orEmpty(),
                                initialAmount = uiState.activeTracker?.budget?.toString().orEmpty(),
                                initialStartDate = uiState.activeTracker?.startDate.orEmpty(),
                                initialEndDate = uiState.activeTracker?.endDate.orEmpty(),
                                loading = uiState.isLoading,
                                errorMessage = uiState.error ?: currentTrackerDetails.errorMessage,
                                infoMessage = uiState.infoMessage,
                                hasActiveTracker = state == TrackerSessionState.ActiveBudget,
                                activeTrackerId = uiState.activeTracker?.id,
                                trackers = uiState.trackers
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(message)
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun BottomBar(
    navController: NavHostController,
    hasActiveBudget: Boolean
) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.Analytics, BottomNavItem.BudgetSetup)
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.scrim,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val enabled = hasActiveBudget || item.route == BottomNavItem.BudgetSetup.route
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentDestination?.route == item.route,
                enabled = enabled,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                ),
                onClick = {
                    if (enabled && currentDestination?.route != item.route) {
                        navController.navigateToBottomTab(item.route)
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateToBottomTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
