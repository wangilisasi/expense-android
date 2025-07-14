package com.example.expensemanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensemanager.ui.ExpenseInputForm
import com.example.expensemanager.ui.ExpenseListScreen
import com.example.expensemanager.ui.ExpenseListViewModel
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import kotlin.text.isNotBlank
import kotlin.text.reversed
import kotlin.text.takeLast
import kotlin.text.toDoubleOrNull

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseManagerTheme {
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
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ExpenseInputForm(
                                onFormSubmit = { itemName, amount ->
                                    // Handle the submission
                                    // For now, let's just log it.
                                    // In a real app, you'd likely call a ViewModel function here.
                                    Log.d("MainActivity", "Item: $itemName, Amount: $amount")
                                    // You might want to interact with ExpenseListViewModel here
                                    // For example, if your ExpenseListViewModel has an addExpense method:
                                    // val viewModel: ExpenseListViewModel = viewModel()
                                    // viewModel.addExpense(itemName, amount.toDoubleOrNull() ?: 0.0, LocalDate.now())
                                }
                            )

                            ExpenseListScreen(
                                // Apply the innerPadding from the Scaffold
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }


                }
            }
        }
    }
}


