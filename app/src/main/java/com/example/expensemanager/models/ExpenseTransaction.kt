package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class ExpenseTransaction(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("amount")
    val amount: Double
)