package com.example.expensemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BudgetSetupScreen(
    onFormSubmit: (name: String, budget: String, startDate: String, endDate: String) -> Unit
) {
    var budgetName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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

        // Start Date Picker
        OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it }, // Usually not directly changed by typing
            label = { Text("Start Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showStartDatePicker = true },
            readOnly = true, // Make it read-only
            trailingIcon = {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = "Select Start Date",
                    Modifier.clickable { showStartDatePicker = true }
                )
            }
        )

        // End Date Picker
        OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it }, // Usually not directly changed by typing
            label = { Text("End Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEndDatePicker = true },
            readOnly = true, // Make it read-only
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
                if (budgetName.isNotBlank() && amount.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()) {
                    onFormSubmit(budgetName, amount, startDate, endDate)
                    // Optionally clear fields after submit
                    budgetName = ""
                    amount = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    onDateSelected: (dateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() // Default to today
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
            Button(onClick = {
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetInputFormPreview() {
    ExpenseInputForm(onFormSubmit = { itemName, amount ->
        // In a real app, you'd handle the submission here
        println("Item: $itemName, Amount: $amount")

    })
}
