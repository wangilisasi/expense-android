package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

// Represents a single expense entry.

data class ExpenseResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("trackerId")
    val trackerId: Int // Foreign key to link to an ExpenseTracker
)
