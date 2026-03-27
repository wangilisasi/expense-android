package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expensemanager.models.DEFAULT_EXPENSE_CATEGORY
import com.example.expensemanager.models.DEFAULT_EXPENSE_SELECTION_CATEGORY
import com.example.expensemanager.models.FALLBACK_EXPENSE_CATEGORIES

@Composable
fun ExpenseInputForm(
    categories: List<String> = FALLBACK_EXPENSE_CATEGORIES,
    onFormSubmit: (itemName: String, amount: String, category: String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(DEFAULT_EXPENSE_SELECTION_CATEGORY) }
    val validCategories = categories.ifEmpty { FALLBACK_EXPENSE_CATEGORIES }

    LaunchedEffect(validCategories) {
        if (selectedCategory !in validCategories) {
            selectedCategory = validCategories.firstOrNull { it == DEFAULT_EXPENSE_SELECTION_CATEGORY }
                ?: validCategories.firstOrNull { it == DEFAULT_EXPENSE_CATEGORY }
                ?: validCategories.first()
        }
    }

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
            label = { Text("Item Bought (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
                if (it.isNotBlank() && it.toDoubleOrNull() == null) {
                    amountError = "Please enter a valid number"
                } else {
                    amountError = null
                }
            },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = amountError != null,
            supportingText = {
                if (amountError != null) {
                    Text(amountError!!)
                }
            }
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
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
                validCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                if (amount.toDoubleOrNull() != null) {
                    onFormSubmit(itemName.trim(), amount, selectedCategory)
                    // Optionally clear fields after submit
                    itemName = ""
                    amount = ""
                }
            },
            enabled = amount.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseInputFormPreview() {
    ExpenseInputForm(onFormSubmit = { itemName, amount, category ->
        // In a real app, you'd handle the submission here
        println("Item: $itemName, Amount: $amount, Category: $category")

    })
}
