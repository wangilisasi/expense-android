package com.example.expensemanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.expensemanager.models.CategoryAnalyticsItem
import com.example.expensemanager.navigation.Screen
import com.example.expensemanager.ui.theme.Amber500
import com.example.expensemanager.ui.theme.Green600
import com.example.expensemanager.ui.theme.Lavender300
import com.example.expensemanager.ui.theme.Lavender500
import com.example.expensemanager.ui.theme.Red600
import com.example.expensemanager.ui.theme.Slate600
import com.example.expensemanager.ui.theme.Slate700
import com.example.expensemanager.ui.theme.Teal300
import com.example.expensemanager.ui.viewmodels.AuthState
import com.example.expensemanager.ui.viewmodels.AuthViewModel
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import java.util.Locale

private val ChartColors = listOf(
    Teal300,
    Slate600,
    Amber500,
    Lavender300,
    Green600,
    Slate700,
    Red600,
    Lavender500
)

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseListViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    rootNavController: NavHostController,
) {
    val authState by authViewModel.authState.collectAsState()
    val username by authViewModel.username.collectAsState()
    val statsUiState by expenseViewModel.statsUiState.collectAsState()
    val uiState by expenseViewModel.uiState.collectAsState()
    val categoryItems = statsUiState.categoryAnalytics?.categories.orEmpty()
    val hasActiveBudget = uiState.activeTracker != null

    LaunchedEffect(authState) {
        if (authState == AuthState.Unauthenticated) {
            rootNavController.navigate(Screen.Login.route) {
                popUpTo(Screen.Main.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        when {
            statsUiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            statsUiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statsUiState.errorMessage ?: "Failed to load analytics",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            !hasActiveBudget -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create an active budget to view analytics.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TopHeader(
                            username = username,
                            onLogoutClick = { authViewModel.logout() }
                        )
                    }

                    item {
                        AnalyticsChartCard(categoryItems = categoryItems)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsChartCard(categoryItems: List<CategoryAnalyticsItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Category Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (categoryItems.isEmpty()) {
                Text(
                    text = "No category spend yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val total = categoryItems.sumOf { it.totalAmount }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    var startAngle = -90f
                    categoryItems.forEachIndexed { index, item ->
                        val sweep = if (total > 0) ((item.totalAmount / total) * 360f).toFloat() else 0f
                        drawArc(
                            color = ChartColors[index % ChartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 42f)
                        )
                        startAngle += sweep
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTzs(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = ChartColors[index % ChartColors.size],
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${formatTzs(item.totalAmount)} (${String.format(Locale.US, "%.1f", item.percentage)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
