package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class ExpenseTransaction(
    @SerializedName("uuid_id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("amount")
    val amount: Double
)