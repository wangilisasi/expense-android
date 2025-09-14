package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class DailyExpense(
    @SerializedName("date")
    val date: String,
    @SerializedName("total_amount")
    val total_amount: Double,
    @SerializedName("transactions")
    val transactions: List<ExpenseTransaction>
)