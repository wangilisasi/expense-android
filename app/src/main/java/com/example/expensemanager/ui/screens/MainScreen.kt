package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensemanager.navigation.BottomNavItem
import com.example.expensemanager.ui.viewmodels.ExpenseListNavigationEvent
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val mainScreenNavController = rememberNavController()
    val expenseListViewModel: ExpenseListViewModel = hiltViewModel()

    val currentTrackerDetails by expenseListViewModel.statsUiState.collectAsState()
    val uiState by expenseListViewModel.uiState.collectAsState()

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
        bottomBar = { BottomBar(mainScreenNavController) }
    ) { innerPadding ->
        NavHost(
            mainScreenNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                DashBoardScreen(
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
                    initialName = currentTrackerDetails.trackerName,
                    initialAmount = currentTrackerDetails.trackerStats?.budget?.toString() ?: "",
                    initialStartDate = currentTrackerDetails.trackerStats?.startDate ?: "",
                    initialEndDate = currentTrackerDetails.trackerStats?.endDate ?: "",
                    loading = uiState.isLoading,
                    errorMessage = uiState.error ?: currentTrackerDetails.errorMessage
                )
            }
        }
    }
}


@Composable
fun BottomBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.BudgetSetup)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    if (currentDestination?.route != item.route) {
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
