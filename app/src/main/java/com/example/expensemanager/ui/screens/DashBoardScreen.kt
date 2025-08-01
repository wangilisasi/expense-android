package com.example.expensemanager.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoardScreen(
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseListViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    rootNavController: NavHostController
) {
    val uiState by expenseViewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Observe auth state changes and navigate when user becomes unauthenticated
    LaunchedEffect(authState) {
        if (authState == AuthState.Unauthenticated) {
            rootNavController.navigate(Screen.Login.route) {
                popUpTo(Screen.Main.route) {
                    inclusive = true // remove Main from the back stack
                }
                launchSingleTop = true
            }
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Expense List") },
                actions = {
                    ThreeDotMenu(onLogoutClick = {
                        authViewModel.logout()
                    })
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.error!!)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = true
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { desiredValue ->
                                if (desiredValue == SwipeToDismissBoxValue.EndToStart) { // Swiped left
                                    expenseViewModel.deleteExpense(expense.id)
                                    true // Confirm the dismiss
                                } else {
                                    false // Don't confirm other swipes (e.g., if you only want EndToStart)
                                }
                            }
                        )

                        // Optional: Reset swipe state if the item is re-added or if you want to programmatically reset
                        // LaunchedEffect(expense.id) { // Or some other key if needed
                        //    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        //        dismissState.reset()
                        //    }
                        // }


                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false, // Disable swipe right
                            enableDismissFromEndToStart = true,   // Enable swipe left
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                        else -> Color.Transparent // Or your default background
                                    },
                                    label = "background color animation"
                                )
                                val scale by animateFloatAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                                    label = "icon scale animation"
                                )

                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd // Align icon to the right for swipe left
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Icon",
                                        modifier = Modifier.scale(scale)
                                    )
                                }
                            }
                        ) {
                            // This is the content that will be swiped.
                            ExpenseListItem(expense = expense)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseListItem(expense: ExpenseResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // No vertical padding here, SwipeToDismissBox handles its own size
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = expense.description)
            Text(text = "€${expense.amount}")
        }
    }
}
