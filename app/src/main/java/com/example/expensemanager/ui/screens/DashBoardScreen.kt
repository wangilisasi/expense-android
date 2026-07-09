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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.expensemanager.models.DEFAULT_EXPENSE_CATEGORY
import com.example.expensemanager.models.DEFAULT_EXPENSE_DESCRIPTION
import com.example.expensemanager.models.DEFAULT_EXPENSE_SELECTION_CATEGORY
import com.example.expensemanager.models.DailyExpense
import com.example.expensemanager.models.ExpenseTransaction
import com.example.expensemanager.models.FALLBACK_EXPENSE_CATEGORIES
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.theme.Amber500
import com.example.expensemanager.ui.theme.Cloud50
import com.example.expensemanager.ui.theme.Green600
import com.example.expensemanager.ui.theme.Red600
import com.example.expensemanager.ui.theme.Slate700
import com.example.expensemanager.ui.theme.Slate900
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ✅ Helper function for formatting TZS with space
fun formatTzs(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale("en", "TZ")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "TZS " + numberFormat.format(amount)
}

fun formatTzsAmountOnly(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale("en", "TZ")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return numberFormat.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoardScreen(
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseListViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    rootNavController: NavHostController,
) {
    val uiState by expenseViewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val username by authViewModel.username.collectAsState()
    val statsUiState by expenseViewModel.statsUiState.collectAsState()
    val hasActiveBudget = uiState.activeTracker != null

    // State for the "Add Expense" dialog
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var newExpenseDescription by remember { mutableStateOf("") }
    var newExpenseAmount by remember { mutableStateOf("") }
    var pendingDeleteExpense by remember { mutableStateOf<ExpenseTransaction?>(null) }
    var pendingEditExpense by remember { mutableStateOf<Pair<ExpenseTransaction, String>?>(null) }
    val availableCategories = uiState.availableCategories.ifEmpty { FALLBACK_EXPENSE_CATEGORIES }
    var newExpenseCategory by remember { mutableStateOf(DEFAULT_EXPENSE_SELECTION_CATEGORY) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var editExpenseDescription by remember { mutableStateOf("") }
    var editExpenseAmount by remember { mutableStateOf("") }
    var editExpenseCategory by remember { mutableStateOf(DEFAULT_EXPENSE_SELECTION_CATEGORY) }
    var editCategoryMenuExpanded by remember { mutableStateOf(false) }
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(availableCategories) {
        if (newExpenseCategory !in availableCategories) {
            newExpenseCategory = availableCategories.firstOrNull { it == DEFAULT_EXPENSE_SELECTION_CATEGORY }
                ?: availableCategories.firstOrNull { it == DEFAULT_EXPENSE_CATEGORY }
                ?: availableCategories.first()
        }
    }
    LaunchedEffect(showAddExpenseDialog) {
        if (showAddExpenseDialog) {
            amountFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(pendingEditExpense?.first?.id) {
        pendingEditExpense?.first?.let { expense ->
            editExpenseDescription = expense.name.takeIf { it != DEFAULT_EXPENSE_DESCRIPTION }.orEmpty()
            editExpenseAmount = expense.amount.toString()
            editExpenseCategory = expense.category
            editCategoryMenuExpanded = false
        }
    }

    // Budget values
    val budget = statsUiState.trackerStats?.budget ?: 0.0
    val targetSpend = statsUiState.trackerStats?.targetExpenditurePerDay ?: 0.0
    val totalSpent = statsUiState.trackerStats?.totalExpenditure ?: 0.0
    val remaining = budget - totalSpent
    val todaysSpend = statsUiState.trackerStats?.todaysExpenditure ?: 0.0

    // ========= Add Expense Dialog =========
    if (showAddExpenseDialog) {
        AlertDialog(
            modifier = Modifier
                .shadow(14.dp, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp),
            onDismissRequest = {
                showAddExpenseDialog = false
                newExpenseDescription = ""
                newExpenseAmount = ""
                newExpenseCategory = availableCategories.firstOrNull { it == DEFAULT_EXPENSE_SELECTION_CATEGORY }
                    ?: availableCategories.firstOrNull { it == DEFAULT_EXPENSE_CATEGORY }
                    ?: availableCategories.first()
                categoryMenuExpanded = false
            },
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 8.dp,
            title = { Text("Add New Expense") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1) Amount
                    OutlinedTextField(
                        value = newExpenseAmount,
                        onValueChange = { newExpenseAmount = it },
                        label = { Text("Amount") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(amountFocusRequester)
                    )

                    // 2) Category
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newExpenseCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { categoryMenuExpanded = !categoryMenuExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select category"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        newExpenseCategory = category
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3) Description (optional)
                    OutlinedTextField(
                        value = newExpenseDescription,
                        onValueChange = { newExpenseDescription = it },
                        label = { Text("Description (optional)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amountDouble = newExpenseAmount.toDoubleOrNull()
                        if (amountDouble != null) {
                            expenseViewModel.addExpense(
                                newExpenseDescription.trim(),
                                amountDouble,
                                LocalDate.now().toString(),
                                newExpenseCategory
                            )
                            showAddExpenseDialog = false
                            newExpenseDescription = ""
                            newExpenseAmount = ""
                            newExpenseCategory = availableCategories.firstOrNull { it == DEFAULT_EXPENSE_SELECTION_CATEGORY }
                                ?: availableCategories.firstOrNull { it == DEFAULT_EXPENSE_CATEGORY }
                                ?: availableCategories.first()
                            categoryMenuExpanded = false
                        }
                    },
                    enabled = newExpenseAmount.toDoubleOrNull() != null
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
                        newExpenseCategory = availableCategories.firstOrNull { it == DEFAULT_EXPENSE_SELECTION_CATEGORY }
                            ?: availableCategories.firstOrNull { it == DEFAULT_EXPENSE_CATEGORY }
                            ?: availableCategories.first()
                        categoryMenuExpanded = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingDeleteExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDeleteExpense = null },
            title = { Text("Delete Expense?") },
            text = {
                Text(
                    text = "This will permanently delete ${formatTzs(expense.amount)} from ${expense.category}.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseViewModel.deleteExpense(expense.id)
                        pendingDeleteExpense = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingEditExpense?.let { editTarget ->
        val expense = editTarget.first
        val expenseDate = editTarget.second
        AlertDialog(
            modifier = Modifier
                .shadow(14.dp, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp),
            onDismissRequest = {
                pendingEditExpense = null
                editCategoryMenuExpanded = false
            },
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 8.dp,
            title = { Text("Edit Expense") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editExpenseAmount,
                        onValueChange = { editExpenseAmount = it },
                        label = { Text("Amount") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editExpenseCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { editCategoryMenuExpanded = !editCategoryMenuExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select category"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = editCategoryMenuExpanded,
                            onDismissRequest = { editCategoryMenuExpanded = false }
                        ) {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        editExpenseCategory = category
                                        editCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editExpenseDescription,
                        onValueChange = { editExpenseDescription = it },
                        label = { Text("Description (optional)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amountDouble = editExpenseAmount.toDoubleOrNull()
                        if (amountDouble != null) {
                            expenseViewModel.updateExpense(
                                expense.id,
                                editExpenseDescription.trim(),
                                amountDouble,
                                expenseDate,
                                editExpenseCategory
                            )
                            pendingEditExpense = null
                            editCategoryMenuExpanded = false
                        }
                    },
                    enabled = editExpenseAmount.toDoubleOrNull() != null
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingEditExpense = null
                        editCategoryMenuExpanded = false
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

    // ========= MAIN LAYOUT =========
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (hasActiveBudget) {
                Surface(
                    onClick = { showAddExpenseDialog = true },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Expense",
                            tint = Cloud50,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Add Expense",
                            color = Cloud50,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
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
                !hasActiveBudget -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No active budget for today.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Open the Budget tab to create a new budget and keep using this account.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
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
                        item {
                            TopHeader(
                                username = username,
                                onLogoutClick = { authViewModel.logout() }
                            )
                        }

                        uiState.infoMessage?.takeIf { it.isNotBlank() }?.let { message ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

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
                                onEditClick = { expense, date ->
                                    pendingEditExpense = expense to date
                                },
                                onDeleteClick = { expense ->
                                    pendingDeleteExpense = expense
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(
    modifier: Modifier = Modifier,
    username: String? = null,
    onLogoutClick: () -> Unit
) {
    val monthLabel = remember {
        YearMonth.now().format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            username
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    Text(
                        text = "Hi, $it",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                ?: Spacer(modifier = Modifier.width(1.dp))

            ThreeDotMenu(onLogoutClick = onLogoutClick)
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelMedium,
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
    val remainingColor = if (remaining >= 0) Cloud50 else Red600.copy(alpha = 0.95f)
    val usageFraction = if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val remainingFraction = if (budget > 0) (remaining / budget).toFloat().coerceIn(0f, 1f) else 0f
    val remainingPercentage = (remainingFraction * 100).roundToInt()
    val usageColor = when {
        budget <= 0 -> Cloud50.copy(alpha = 0.7f)
        remaining <= 0 -> Red600
        remainingFraction > 0.5f -> Green600
        remainingFraction > 0.2f -> Amber500
        else -> Red600
    }
    val budgetContext = if (budget > 0) {
        "$remainingPercentage% left of ${formatTzs(budget)} budget"
    } else {
        "No monthly budget set"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Slate900, Slate700)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Remaining",
                color = Cloud50.copy(alpha = 0.82f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                )
            )
            Text(
                formatTzs(remaining),
                color = remainingColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            Text(
                text = budgetContext,
                color = Cloud50.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Cloud50.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(usageFraction)
                        .clip(RoundedCornerShape(8.dp))
                        .background(usageColor)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Cloud50.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Cloud50.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "All amounts in TZS",
                        color = Cloud50.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BudgetMetricCell(
                            modifier = Modifier.weight(1f),
                            label = "Spent",
                            amount = formatTzsAmountOnly(totalSpent)
                        )
                        BudgetMetricCell(
                            modifier = Modifier.weight(1f),
                            label = "Today",
                            amount = formatTzsAmountOnly(todaysSpend)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        thickness = 1.dp,
                        color = Cloud50.copy(alpha = 0.1f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BudgetMetricCell(
                            modifier = Modifier.weight(1f),
                            label = "Daily target",
                            amount = formatTzsAmountOnly(targetSpend),
                            compact = true
                        )
                        BudgetMetricCell(
                            modifier = Modifier.weight(1f),
                            label = "Daily average",
                            amount = formatTzsAmountOnly(averageExpenditure),
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    compact: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = Cloud50.copy(alpha = 0.72f),
            style = if (compact) {
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        )
        Text(
            text = amount,
            color = Cloud50,
            style = if (compact) {
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            } else {
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                )
            },
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
    onEditClick: (ExpenseTransaction, String) -> Unit,
    onDeleteClick: (ExpenseTransaction) -> Unit
) {
    var showAllDays by remember(dailyExpenses.size) { mutableStateOf(false) }
    val visibleDailyExpenses = if (showAllDays) dailyExpenses else dailyExpenses.take(5)

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
                text = "${visibleDailyExpenses.size} of ${dailyExpenses.size} day(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (dailyExpenses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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

        visibleDailyExpenses.forEachIndexed { index, day ->
            var expanded by remember { mutableStateOf(index == 0) } // first one (today) expanded by default
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "")
            val isToday = day.date == LocalDate.now().toString()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(14.dp),
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
                                val showName = trx.name.isNotBlank() && trx.name != DEFAULT_EXPENSE_DESCRIPTION
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (showName) {
                                            Text(
                                                text = trx.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = trx.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = trx.category,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = formatExpenseTimestamp(
                                                trx.occurredAt.ifBlank { trx.createdAt },
                                                day.date
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                                        )
                                    }
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
                                        ExpenseRowMenu(
                                            onEditClick = { onEditClick(trx, day.date) },
                                            onDeleteClick = { onDeleteClick(trx) }
                                        )
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

        if (dailyExpenses.size > 5) {
            TextButton(
                onClick = { showAllDays = !showAllDays },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(if (showAllDays) "Show Less" else "Show More")
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

@Composable
private fun ExpenseRowMenu(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Expense actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onEditClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onDeleteClick()
                }
            )
        }
    }
}

private fun formatExpenseTimestamp(rawTimestamp: String, fallbackDate: String): String {
    val zoneId = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    val localDateTime = rawTimestamp
        .takeIf { it.isNotBlank() }
        ?.let { raw ->
            runCatching {
                Instant.ofEpochMilli(raw.toLong()).atZone(zoneId).toLocalDateTime()
            }.getOrNull()
                ?: runCatching {
                    OffsetDateTime.parse(raw).atZoneSameInstant(zoneId).toLocalDateTime()
                }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(raw.take(19))
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(zoneId)
                        .toLocalDateTime()
                }.getOrNull()
        }

    return localDateTime?.format(formatter)
        ?: runCatching { LocalDate.parse(fallbackDate.take(10)).format(DateTimeFormatter.ofPattern("dd MMM")) }
            .getOrDefault(fallbackDate)
}
