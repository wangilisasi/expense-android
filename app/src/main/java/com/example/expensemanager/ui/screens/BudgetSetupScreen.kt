package com.example.expensemanager.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.models.TrackerSummaryResponse
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onFormSubmit: (name: String, budget: String, startDate: String, endDate: String) -> Unit,
    initialName: String = "",
    initialAmount: String = "",
    initialStartDate: String = "",
    initialEndDate: String = "",
    loading: Boolean = false,
    errorMessage: String? = null,
    infoMessage: String? = null,
    hasActiveTracker: Boolean = false,
    activeTrackerId: String? = null,
    trackers: List<TrackerSummaryResponse> = emptyList()
) {
    var budgetName by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var amount by rememberSaveable(initialAmount) { mutableStateOf(initialAmount) }
    var startDate by rememberSaveable(initialStartDate) { mutableStateOf(initialStartDate) }
    var endDate by rememberSaveable(initialEndDate) { mutableStateOf(initialEndDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val isEditMode = hasActiveTracker && initialAmount.isNotBlank()
    val buttonText = if (isEditMode) "Update Budget" else "Create Budget"
    val authViewModel: AuthViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Budget" else "Create Budget") },
                actions = {
                    ThreeDotMenu(onLogoutClick = {
                        authViewModel.logout()
                    })
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                if (hasActiveTracker) {
                    StatusBanner(
                        text = "Active budget found. You can edit it here or review previous budgets below.",
                        isError = false
                    )
                } else {
                    StatusBanner(
                        text = "No active budget was found for today. Create a new budget for this account.",
                        isError = false
                    )
                }
            }

            infoMessage?.takeIf { it.isNotBlank() }?.let { message ->
                item {
                    StatusBanner(text = message, isError = false)
                }
            }

            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                item {
                    StatusBanner(text = message, isError = true)
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = budgetName,
                        onValueChange = { budgetName = it },
                        label = { Text("Budget Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Budget Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { },
                        label = { Text("Start Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Select Start Date",
                                Modifier.clickable { showStartDatePicker = true }
                            )
                        }
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { },
                        label = { Text("End Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Select End Date",
                                Modifier.clickable { showEndDatePicker = true }
                            )
                        }
                    )

                    Button(
                        onClick = {
                            if (
                                budgetName.isNotBlank() &&
                                amount.isNotBlank() &&
                                startDate.isNotBlank() &&
                                endDate.isNotBlank()
                            ) {
                                onFormSubmit(budgetName, amount, startDate, endDate)
                                Log.d(
                                    "BudgetSetupScreen",
                                    "onFormSubmit called with values: $budgetName, $amount, $startDate, $endDate"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        enabled = !loading
                    ) {
                        if (loading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(buttonText)
                        }
                    }
                }
            }

            if (trackers.isNotEmpty()) {
                item {
                    Text(
                        text = "Budget History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(trackers, key = { it.id }) { tracker ->
                    TrackerHistoryCard(
                        tracker = tracker,
                        isActiveTracker = tracker.id == activeTrackerId
                    )
                }
            }
        }

        if (showStartDatePicker) {
            MyDatePickerDialog(
                onDateSelected = { selectedDateMillis ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                    }
                    startDate = dateFormatter.format(calendar.time)
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }

        if (showEndDatePicker) {
            MyDatePickerDialog(
                onDateSelected = { selectedDateMillis ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                    }
                    endDate = dateFormatter.format(calendar.time)
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }
    }
}

@Composable
private fun StatusBanner(
    text: String,
    isError: Boolean
) {
    val background = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = background
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun TrackerHistoryCard(
    tracker: TrackerSummaryResponse,
    isActiveTracker: Boolean
) {
    val status = tracker.statusLabel()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = tracker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "TZS ${formatTzsAmountOnly(tracker.budget)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isActiveTracker || status == "Active") {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = if (isActiveTracker) "Current" else status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Text(
                text = "${tracker.startDate} to ${tracker.endDate}",
                style = MaterialTheme.typography.bodyMedium
            )

            tracker.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun TrackerSummaryResponse.statusLabel(today: LocalDate = LocalDate.now()): String {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val start = runCatching { LocalDate.parse(startDate.take(10), formatter) }.getOrNull()
    val end = runCatching { LocalDate.parse(endDate.take(10), formatter) }.getOrNull()

    return when {
        start == null || end == null -> "Saved"
        today.isBefore(start) -> "Upcoming"
        today.isAfter(end) -> "Expired"
        else -> "Active"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    onDateSelected: (dateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(it)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetSetupScreenPreview() {
    BudgetSetupScreen(
        onFormSubmit = { _, _, _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun BudgetEditScreenPreview() {
    BudgetSetupScreen(
        onFormSubmit = { _, _, _, _ -> },
        initialName = "Groceries",
        initialAmount = "200",
        initialStartDate = "2024-08-01",
        initialEndDate = "2024-08-31",
        hasActiveTracker = true,
        activeTrackerId = "1",
        trackers = listOf(
            TrackerSummaryResponse(
                id = "1",
                name = "Groceries",
                description = "Monthly groceries",
                startDate = "2024-08-01",
                endDate = "2024-08-31",
                budget = 200.0
            )
        )
    )
}
