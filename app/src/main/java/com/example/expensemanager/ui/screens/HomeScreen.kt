package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import com.example.expensemanager.utils.Screen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel= hiltViewModel(),
    expenseViewModel: ExpenseListViewModel= hiltViewModel(),
) //Callback when logout is clickedL
{


    val authstate by authViewModel.authState.collectAsState()
    val uiState by expenseViewModel.uiState.collectAsState()

    LaunchedEffect(authstate){
        if (authstate is AuthState.Unauthenticated){
            navController.navigate(Screen.Login.route)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    ThreeDotMenu(onLogoutClick = { authViewModel.logout() })
                }
            )
        }
    ){innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BudgetStatus(
                    modifier = modifier,
                    viewModel = expenseViewModel
                )
            }

            item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else if (uiState.error != null) {
                        Text(text = uiState.error!!)
                    } else {
                        ExpenseInputForm(
                            onFormSubmit = { itemName, amount ->
                                expenseViewModel.addExpense(
                                    itemName,
                                    amount.toDoubleOrNull() ?: 0.0,
                                    LocalDate.now().toString()
                                )
                            }
                        )
                    }
                }
            }

        }
    }

}