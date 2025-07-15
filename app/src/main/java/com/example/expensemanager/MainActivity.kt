package com.example.expensemanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.ui.composables.ExpenseInputForm
import com.example.expensemanager.ui.composables.ExpenseListScreen
import com.example.expensemanager.ui.ExpenseListViewModel
import com.example.expensemanager.ui.composables.BudgetStatus
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import kotlin.text.toDoubleOrNull

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseManagerTheme {
                val viewModel: ExpenseListViewModel = hiltViewModel()

                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {

                })
                { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BudgetStatus(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel
                            )
                            ExpenseInputForm(
                                onFormSubmit = { itemName, amount ->
                                    // Handle the submission
                                    // For now, let's just log it.
                                    // In a real app, you'd likely call a ViewModel function here.
                                    Log.d("MainActivity", "Item: $itemName, Amount: $amount")
                                    // You might want to interact with ExpenseListViewModel here
                                    // For example, if your ExpenseListViewModel has an addExpense method:
                                    viewModel.addExpense(itemName, amount.toDoubleOrNull() ?: 0.0, LocalDate.now().toString())
                                    // viewModel.addExpense(itemName, amount.toDoubleOrNull() ?: 0.0, LocalDate.now())
                                }
                            )

                            ExpenseListScreen(
                                // Apply the innerPadding from the Scaffold
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel
                            )
                        }
                    }


                }
            }
        }
    }
}


