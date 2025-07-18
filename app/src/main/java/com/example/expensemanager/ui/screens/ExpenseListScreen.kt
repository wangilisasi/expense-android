package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel

@Composable
fun ExpenseListScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel: ExpenseListViewModel = hiltViewModel()
    // Collect the single state object
    val uiState by viewModel.uiState.collectAsState()
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Now access properties from the state object
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.error != null) {
            Text(text = uiState.error!!)
        } else {
            LazyColumn {
                items(uiState.expenses) { expense ->
                    ExpenseListItem(expense = expense)
                }
            }
        }
    }

}


@Composable
fun ExpenseListItem(expense: ExpenseResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = expense.description)
            Text(text = "€${expense.amount}") // Format your currency
        }
    }
}