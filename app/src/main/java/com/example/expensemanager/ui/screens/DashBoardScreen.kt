package com.example.expensemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

// ✅ Helper function for formatting TZS with space
fun formatTzs(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale("en", "TZ")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "TZS " + numberFormat.format(amount)
}

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
    val statsUiState by expenseViewModel.statsUiState.collectAsState()

    var expenseToDelete by remember { mutableStateOf<ExpenseResponse?>(null) }

    // State for the "Add Expense" dialog
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var newExpenseDescription by remember { mutableStateOf("") }
    var newExpenseAmount by remember { mutableStateOf("") }

    // Hardcoded values to match the UI design
    val budget = statsUiState.trackerStats?.budget ?: 0.0
    val targetSpend = statsUiState.trackerStats?.targetExpenditurePerDay ?: 0.0
    val totalSpent = statsUiState.trackerStats?.totalExpenditure ?: 0.0
    val remaining = budget - totalSpent
    val todaysSpend = statsUiState.trackerStats?.todaysExpenditure ?: 0.0

    // Toggle for Transactions
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "")

    // ========= Add Expense Dialog =========
    if (showAddExpenseDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddExpenseDialog = false
                newExpenseDescription = ""
                newExpenseAmount = ""
            },
            title = { Text("Add New Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newExpenseDescription,
                        onValueChange = { newExpenseDescription = it },
                        label = { Text("Item Bought") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newExpenseAmount,
                        onValueChange = { newExpenseAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amountDouble = newExpenseAmount.toDoubleOrNull()
                        if (newExpenseDescription.isNotBlank() && amountDouble != null) {
                            expenseViewModel.addExpense(
                                newExpenseDescription,
                                amountDouble,
                                LocalDate.now().toString()
                            )
                            showAddExpenseDialog = false
                            newExpenseDescription = ""
                            newExpenseAmount = ""
                        }
                    },
                    enabled = newExpenseDescription.isNotBlank() && newExpenseAmount.toDoubleOrNull() != null
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddExpenseDialog = false
                        newExpenseDescription = ""
                        newExpenseAmount = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ========= Confirm Delete Dialog =========
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete '${expense.description}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseViewModel.deleteExpense(expense.id)
                        expenseToDelete = null
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
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

    // ========= MAIN LAYOUT =========
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                shape = CircleShape,
                containerColor = Color(0xFF616161)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { TopHeader(onLogoutClick = { authViewModel.logout() }) }

                    item {
                        BudgetSummaryCard(
                            remaining = remaining,
                            budget = budget,
                            targetSpend = targetSpend,
                            totalSpent = totalSpent,
                            todaysSpend = todaysSpend
                        )
                    }

                    // Collapsible Recent Transactions Header
                    if (uiState.expenses.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Transactions",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        modifier = Modifier.rotate(rotation)

                                    )
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(visible = expanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    uiState.expenses.forEach { expense ->
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = {
                                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                                    expenseToDelete = expense
                                                    return@rememberSwipeToDismissBoxState false
                                                }
                                                true
                                            },
                                        )

                                        LaunchedEffect(expenseToDelete) {
                                            if (expenseToDelete == null) {
                                                scope.launch { dismissState.reset() }
                                            }
                                        }

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = false,
                                            backgroundContent = {
                                                val color by animateColorAsState(
                                                    targetValue = when (dismissState.targetValue) {
                                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFB71C1C)
                                                        else -> Color.Transparent
                                                    },
                                                    label = "",
                                                )
                                                val scale by animateFloatAsState(
                                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                                                    label = ""
                                                )

                                                Box(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .background(color, shape = RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Icon",
                                                        modifier = Modifier.scale(scale),
                                                        tint = Color.White
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
            }
        }
    }
}

@Composable
fun TopHeader(modifier: Modifier = Modifier, onLogoutClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "September 2025", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun BudgetSummaryCard(
    remaining: Double,
    budget: Double,
    targetSpend: Double,
    totalSpent: Double,
    todaysSpend: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF424242), Color(0xFF616161))
                    )
                )
                .padding(20.dp)
        ) {
            Text("Remaining", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
            Text(
                formatTzs(remaining),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow("Budget", formatTzs(budget))
                SummaryRow("Target Spend", formatTzs(targetSpend))
                SummaryRow("Total Spent", formatTzs(totalSpent))
                SummaryRow("Today's Spend", formatTzs(todaysSpend))
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        Text(amount, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ExpenseListItem(expense: ExpenseResponse) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(expense.description, style = MaterialTheme.typography.bodyLarge)
            Text(formatTzs(expense.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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