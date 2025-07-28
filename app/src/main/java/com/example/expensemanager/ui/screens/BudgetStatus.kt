package com.example.expensemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.ui.uistates.TrackerStatsUiState
import com.example.expensemanager.ui.viewmodels.ExpenseListViewModel
import java.text.NumberFormat
import java.util.Locale

data class BudgetDetailItem(
    val label: String,
    val value: String
)

@Composable
fun BudgetStatus(
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val statsUiState by viewModel.statsUiState.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                statsUiState.isLoading -> {
                    LoadingContent()
                }
                statsUiState.errorMessage != null -> {
                    ErrorContent(errorMessage = statsUiState.errorMessage)
                }
                else -> {
                    BudgetDetailsContent(statsUiState = statsUiState)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ErrorContent(errorMessage: String?) {
    Text(
        text = errorMessage ?: "An unknown error occurred",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BudgetDetailsContent(statsUiState: TrackerStatsUiState) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault())
    }

    val budgetDetails = remember(statsUiState.trackerStats) {
        createBudgetDetailsList(statsUiState, currencyFormatter)
    }

    BudgetDetailsGrid(budgetDetails = budgetDetails)
}

@Composable
private fun BudgetDetailsGrid(budgetDetails: List<BudgetDetailItem>) {
    val chunkedDetails = remember(budgetDetails) {
        budgetDetails.chunked(2)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        chunkedDetails.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    BudgetDetailCell(
                        label = item.label,
                        value = item.value,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Add spacer for odd number of items in the last row
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BudgetDetailCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun createBudgetDetailsList(
    statsUiState: TrackerStatsUiState,
    currencyFormatter: NumberFormat
): List<BudgetDetailItem> {
    val trackerStats = statsUiState.trackerStats
    return listOf(
        BudgetDetailItem(
            label = "Start Date",
            value = trackerStats?.startDate ?: "N/A"
        ),
        BudgetDetailItem(
            label = "End Date",
            value = trackerStats?.endDate ?: "N/A"
        ),
        BudgetDetailItem(
            label = "Total Budget",
            value = trackerStats?.budget?.let {
                currencyFormatter.format(it) } ?: "N/A"
        ),
        BudgetDetailItem(
            label = "Total Expenses",
            value = trackerStats?.totalExpenditure?.let { currencyFormatter.format(it) } ?: "N/A"
        ),
        BudgetDetailItem(
            label = "Days Remaining",
            value = trackerStats?.remainingDays?.toString() ?: "N/A"
        ),
        BudgetDetailItem(
            label = "Target Expenditure",
            value = trackerStats?.targetExpenditurePerDay?.let { currencyFormatter.format(it) } ?: "N/A"
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun BudgetStatusPreview() {
    MaterialTheme {
        // Note: This preview won't work without proper ViewModel setup
        // Consider creating a preview version with mock data
        Column {
            Text("Preview requires mock data setup")
        }
    }
}