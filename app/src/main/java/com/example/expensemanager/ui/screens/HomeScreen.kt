package com.example.expensemanager.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import java.time.LocalDate

@Composable
fun HomeScreen(modifier: Modifier=Modifier, viewModel: ExpenseListViewModel){
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
       item{
           BudgetStatus(
               modifier = modifier,
               viewModel = viewModel
           )
       }

        item{
            ExpenseInputForm(
                onFormSubmit = { itemName, amount ->
                    // Handle the submission
                    // For now, let's just log it.
                    // In a real app, you'd likely call a ViewModel function here.
                    Log.d("MainActivity", "Item: $itemName, Amount: $amount")
                    // You might want to interact with ExpenseListViewModel here
                    // For example, if your ExpenseListViewModel has an addExpense method:
                    //viewModel.addExpense(itemName, amount.toDoubleOrNull() ?: 0.0, LocalDate.now().toString())
                    viewModel.addExpense(itemName, amount.toDoubleOrNull() ?: 0.0, LocalDate.now().toString())
                }
            )
        }

    }
}