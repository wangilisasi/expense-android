package com.example.expensemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ExpenseInputForm(
    onFormSubmit: (itemName: String, amount: String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Bought") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (itemName.isNotBlank() && amount.isNotBlank()) {
                    onFormSubmit(itemName, amount)
                    // Optionally clear fields after submit
                    itemName = ""
                    amount = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseInputFormPreview() {
    ExpenseInputForm(onFormSubmit = { itemName, amount ->
        // In a real app, you'd handle the submission here
        println("Item: $itemName, Amount: $amount")
    })
}
