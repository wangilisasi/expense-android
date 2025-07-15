package com.example.expensemanager.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card // Import Material 3 Card
import androidx.compose.material3.CardDefaults // Import CardDefaults for elevation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.ui.ExpenseListViewModel

const val NUM_ROWS_IN_CARD = 8 // Renamed for clarity within this context
const val NUM_COLS_IN_CARD = 2

@Composable
fun BudgetStatus(
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val textStates = remember {
        List(NUM_ROWS_IN_CARD * NUM_COLS_IN_CARD) { mutableStateOf("") }
    }

    Card(
        modifier = modifier
            .fillMaxWidth() // Card can take full width or be sized by its parent
            .padding(8.dp), // Padding around the card itself
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            // Padding inside the card, between card border and grid content
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Increased spacing for visual appeal in card
        ) {
            repeat(NUM_ROWS_IN_CARD) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Increased spacing
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(NUM_COLS_IN_CARD) { colIndex ->
                        val stateIndex = rowIndex * NUM_COLS_IN_CARD + colIndex
                        var currentText by textStates[stateIndex]

                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { newText ->
                                textStates[stateIndex].value = newText
                            },
                            label = { Text("R${rowIndex + 1}C${colIndex + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TextFieldGridInCardPreview() {
    // Provide some padding around the preview to see the card's elevation and shape
    Column(modifier = Modifier.padding(16.dp)) {
        BudgetStatus()
    }
}
