package com.example.expensemanager.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.ui.ExpenseListViewModel

// --- Grid Dimensions ---
private const val NUM_ROWS = 6
private const val NUM_COLS = 2

// --- Manually Initialized Data (Label to Value) ---
private val budgetDetails = listOf(
    "Start Date" to "3-7-25",
    "End Date" to "31-8-25",
    "Total Budget" to "€500.00",
    "Total Expenses" to "€1,149.50",
    "Days Remaining" to "10",
    "Target Expenditure" to "€10.00",
//    "Groceries" to "€450.50",
//    "Transport" to "€80.00",
//    "Entertainment" to "€150.00",
//    "Subscriptions" to "€35.00",
//    "Miscellaneous" to "€65.00",
//    "Remaining Budget" to "€649.50"
)

@Composable
fun BudgetStatus(
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel= hiltViewModel()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            // Padding inside the card
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Vertical spacing between rows
        ) {
            // Create 6 rows
            repeat(NUM_ROWS) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Horizontal spacing between columns
                ) {
                    // Create 2 columns
                    repeat(NUM_COLS) { colIndex ->
                        val dataIndex = rowIndex * NUM_COLS + colIndex
                        if (dataIndex < budgetDetails.size) {
                            val (label, value) = budgetDetails[dataIndex]

                            // Each cell is a Column containing a label and a value
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Label Text
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Value Text
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetStatusPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        BudgetStatus()
    }
}