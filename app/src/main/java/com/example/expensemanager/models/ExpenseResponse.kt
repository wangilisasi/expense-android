package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

// Represents a single expense entry.

data class ExpenseResponse(
    @SerializedName("uuid_id")
    val id: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("uuid_tracker_id")
    val trackerId: String, // Foreign key to link to an ExpenseTracker
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val isSynced: Boolean = false
)
