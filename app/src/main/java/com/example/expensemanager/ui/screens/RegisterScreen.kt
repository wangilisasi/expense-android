package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.models.RegisterRequest
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import kotlin.text.contains

@Composable
fun RegisterScreen(
    onRegistrationSuccessNavigation: () -> Unit, // e.g., navigate to login
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val registrationInProgress by viewModel.registrationInProgress.collectAsState()
    val errorEvent by viewModel.errorEvents.collectAsState()

    // You might want to observe a specific state for registration success
    // For now, we'll rely on the errorEvent or a navigation callback.

    LaunchedEffect(errorEvent) {
        errorEvent?.let {
            if (it.contains("Registration successful", ignoreCase = true)) {
                // You could navigate directly or show a success message before navigating
                onRegistrationSuccessNavigation()
            }
            // Show a Snackbar or Toast for the error/success
            println("Registration Event: $it")
            viewModel.consumeErrorEvent() // Reset the event
        }
    }


    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Register", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val registerRequest = RegisterRequest(username.trim(), email.trim(), password)
                    viewModel.register(registerRequest)
                },
                enabled = !registrationInProgress && username.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (registrationInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Register")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Login")
            }

            errorEvent?.let {
                // Display error message only if it's not the success message
                if (!it.contains("Registration successful", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
