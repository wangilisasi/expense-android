package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class DailyExpensesResponse(
    @SerializedName("daily_expenses")
    val daily_expenses: List<DailyExpense> = emptyList()
)
