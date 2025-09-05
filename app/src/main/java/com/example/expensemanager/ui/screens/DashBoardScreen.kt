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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoardScreen(
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseListViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    rootNavController: NavHostController,
) {
    val uiState by expenseViewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()

    // NEW: State to hold the expense to be deleted, which triggers the dialog
    var expenseToDelete by remember { mutableStateOf<ExpenseResponse?>(null) }

    // Show confirmation dialog if an expense is marked for deletion
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = {
                // Hide the dialog if the user clicks outside or presses back
                expenseToDelete = null
            },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete '${expense.description}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // User confirmed, call the original delete function
                        expenseViewModel.deleteExpense(expense.id)
                        expenseToDelete = null // Hide dialog
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        expenseToDelete = null // Hide dialog
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(authState) {
        if (authState == AuthState.Unauthenticated) {
            rootNavController.navigate(Screen.Login.route) {
                popUpTo(Screen.Main.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Expense List") },
                actions = { ThreeDotMenu(onLogoutClick = { authViewModel.logout() }) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorContent(uiState.error)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.expenses.asReversed(), key = { it.id }) { expense ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    // CHANGED: Instead of deleting, show the dialog
                                    expenseToDelete = expense
                                    // Important: Return false to prevent the SwipeToDismissBox
                                    // from automatically dismissing the item.
                                    return@rememberSwipeToDismissBoxState false
                                }
                                true
                            },
                        )

                        // Reset the swipe state after the dialog is shown or dismissed
                        // This makes the item snap back into place.
                        LaunchedEffect(expenseToDelete) {
                            if (expenseToDelete == null) {
                                scope.launch {
                                    dismissState.reset()
                                }
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                        else -> Color.Transparent
                                    },
                                    label = "background color animation",
                                )
                                val scale by animateFloatAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                                    label = "icon scale animation",
                                )

                                Box(
                                    Modifier.fillMaxSize().background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Icon",
                                        modifier = Modifier.scale(scale),
                                    )
                                }
                            },
                        ) {
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


@Composable
private fun ErrorContent(errorMessage: String?) {
    Text(
        text = errorMessage ?: "An unknown error occurred",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}