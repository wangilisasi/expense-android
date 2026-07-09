package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class ExpenseUpdateRequest(
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("category")
    val category: String = DEFAULT_EXPENSE_CATEGORY,
    @SerializedName("name")
    val name: String = description
)
