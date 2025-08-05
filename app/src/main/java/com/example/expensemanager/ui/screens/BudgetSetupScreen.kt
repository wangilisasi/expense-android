package com.example.expensemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onFormSubmit: (name: String, budget: String, startDate: String, endDate: String) -> Unit,
    initialName: String = "Oldenburg Expenses",
    initialAmount: String = "",
    initialStartDate: String = "",
    initialEndDate: String = "",
    loading: Boolean = false
) {
    var budgetName by rememberSaveable { mutableStateOf(initialName) }
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var startDate by rememberSaveable { mutableStateOf(initialStartDate) }
    var endDate by rememberSaveable { mutableStateOf(initialEndDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val isEditMode = initialAmount.isNotBlank()
    val buttonText = if (isEditMode) "Update Budget" else "Create Budget"
    val authViewModel: AuthViewModel = hiltViewModel()



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Budget" else "Budget Setup") },
                actions = {
                    ThreeDotMenu(onLogoutClick = {
                        authViewModel.logout()
                    })
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = budgetName,
                onValueChange = { budgetName = it },
                label = { Text("Title of Budget") },
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
                onValueChange = { /* no-op */ },
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
                onValueChange = { /* no-op */ },
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (
                        budgetName.isNotBlank() &&
                        amount.isNotBlank() &&
                        startDate.isNotBlank() &&
                        endDate.isNotBlank()
                    ) {
                        onFormSubmit(budgetName, amount, startDate, endDate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(buttonText)
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
        onFormSubmit = { name, amount, start, end ->
            // Preview only
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BudgetEditScreenPreview() {
    BudgetSetupScreen(
        onFormSubmit = { name, amount, start, end ->
            // Preview only
        },
        initialName = "Groceries",
        initialAmount = "200",
        initialStartDate = "2024-08-01",
        initialEndDate = "2024-08-31"
    )
}