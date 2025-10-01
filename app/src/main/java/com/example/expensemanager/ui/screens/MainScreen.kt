package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.expensemanager.navigation.BottomNavItem
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val mainScreenNavController = rememberNavController()
    val expenseListViewModel: ExpenseListViewModel = hiltViewModel()

    val currentTrackerDetails by expenseListViewModel.statsUiState.collectAsState()
    val uiState by expenseListViewModel.uiState.collectAsState()


    Scaffold(
        bottomBar = { BottomBar(mainScreenNavController) }
    ) { innerPadding ->
        NavHost(
            mainScreenNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                // Pass the ViewModel instance down to ensure the same one is used
                //HomeScreen(
                    //rootNavController = rootNavController,
                //)
                DashBoardScreen(
                    rootNavController = rootNavController,
                )
            }
            //composable(BottomNavItem.Reports.route) {
                //DashBoardScreen(
                    //rootNavController = rootNavController,
                //)
            //}
            composable(BottomNavItem.BudgetSetup.route) {
                val isEditMode = (currentTrackerDetails.trackerStats?.budget?.toString() ?: "").isNotBlank()
                BudgetSetupScreen(
                    onFormSubmit = { name, budget, startDate, endDate ->
                        //expenseListViewModel.updateTracker(name, budget.toDouble(), startDate, endDate)
                        if (isEditMode) {
                                                        //expenseListViewModel.updateTracker(name, budget.toDouble(), startDate, endDate)
                                                   } else {
                                                       // For a new user, call createBudget
                                                       //expenseListViewModel.createBudget(name, budget.toDouble(), startDate, endDate)
                                                    }
                                   },
                    initialAmount = currentTrackerDetails.trackerStats?.budget?.toString() ?: "",
                    initialStartDate = currentTrackerDetails.trackerStats?.startDate ?: "",
                    initialEndDate = currentTrackerDetails.trackerStats?.endDate ?: "",
                    loading = uiState.isLoading


                )
            }
        }
    }
}


@Composable
fun BottomBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Home,BottomNavItem.BudgetSetup)
    // Use NavigationBar instead of BottomNavigation
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            // Use NavigationBarItem instead of BottomNavigationItem
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title
                    )
                }, // Example icon, replace as needed
                label = { Text(item.title) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    if (currentDestination?.route != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}