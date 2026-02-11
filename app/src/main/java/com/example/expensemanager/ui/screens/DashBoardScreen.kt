package com.example.expensemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.expensemanager.models.DailyExpense
import com.example.expensemanager.models.ExpenseTransaction
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.theme.Green600
import com.example.expensemanager.ui.theme.Red600
import com.example.expensemanager.ui.theme.Slate700
import com.example.expensemanager.ui.theme.Slate900
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
    val statsUiState by expenseViewModel.statsUiState.collectAsState()

    // State for the "Add Expense" dialog
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var newExpenseDescription by remember { mutableStateOf("") }
    var newExpenseAmount by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<ExpenseTransaction?>(null) }

    // Budget values
    val budget = statsUiState.trackerStats?.budget ?: 0.0
    val targetSpend = statsUiState.trackerStats?.targetExpenditurePerDay ?: 0.0
    val totalSpent = statsUiState.trackerStats?.totalExpenditure ?: 0.0
    val remaining = budget - totalSpent
    val todaysSpend = statsUiState.trackerStats?.todaysExpenditure ?: 0.0

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

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete expense?") },
            text = { Text("Delete \"${expense.name}\" (${formatTzs(expense.amount)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseViewModel.deleteExpense(expense.id)
                        expenseToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
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

    // ========= MAIN LAYOUT =========
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    )
                )
        ) {
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { TopHeader() }

                        item {
                            BudgetSummaryCard(
                                remaining = remaining,
                                budget = budget,
                                targetSpend = targetSpend,
                                totalSpent = totalSpent,
                                todaysSpend = todaysSpend,
                                averageExpenditure = statsUiState.trackerStats?.averageExpenditure ?: 0.0
                            )
                        }
                        item {
                            DailyExpensesSection(
                                dailyExpenses = uiState.dailyExpenses.daily_expenses,
                                onDeleteClick = { expenseToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(modifier: Modifier = Modifier) {
    val monthLabel = remember {
        YearMonth.now().format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
    todaysSpend: Double,
    averageExpenditure: Double = 0.0
) {
    val remainingColor = if (remaining >= 0) Color.White else Color(0xFFFFCDD2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Slate900, Slate700)
                    )
                )
                .padding(22.dp)
        ) {
            Text(
                text = "Remaining",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                formatTzs(remaining),
                color = remainingColor,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 52.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
            )
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.22f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow("Budget", formatTzs(budget))
                SummaryRow("Total Spent", formatTzs(totalSpent))
                SummaryRow("Today's Spend", formatTzs(todaysSpend))
                SummaryRow("Target Spend", formatTzs(targetSpend))
                SummaryRow("Average Spend", formatTzs(averageExpenditure))
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
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.84f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = amount,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
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

@Composable
fun DailyExpensesSection(
    dailyExpenses: List<DailyExpense>,
    onDeleteClick: (ExpenseTransaction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Daily Totals",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${dailyExpenses.size} day(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (dailyExpenses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "No daily totals yet. Add an expense to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
            return
        }

        dailyExpenses.forEachIndexed { index, day ->
            var expanded by remember { mutableStateOf(index == 0) } // first one (today) expanded by default
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "")
            val isToday = day.date == LocalDate.now().toString()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                        CircleShape
                                    )
                            )

                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatDayLabel(day.date),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatTzs(day.total_amount),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                IconButton(
                                    modifier = Modifier.size(30.dp),
                                    onClick = { expanded = !expanded }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(rotation),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(animationSpec = tween(durationMillis = 160)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 120)) + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            day.transactions.forEach { trx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trx.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTzs(trx.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        SyncBadge(isSynced = trx.isSynced)
                                        IconButton(
                                            modifier = Modifier.size(22.dp),
                                            onClick = { onDeleteClick(trx) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete expense",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncBadge(isSynced: Boolean) {
    val badgeBg = if (isSynced) Green600.copy(alpha = 0.16f) else Red600.copy(alpha = 0.16f)
    val badgeText = if (isSynced) Green600 else Red600

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = badgeBg
    ) {
        Text(
            text = if (isSynced) "S" else "U",
            color = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

private fun formatDayLabel(rawDate: String): String {
    val parsed = runCatching { LocalDate.parse(rawDate.take(10)) }.getOrNull() ?: return rawDate
    val today = LocalDate.now()
    return when (parsed) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> parsed.format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.getDefault()))
    }
}
