package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class ExpenseTransaction(
    @SerializedName("uuid_id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("category")
    val category: String = DEFAULT_EXPENSE_CATEGORY,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = "",
    val isSynced: Boolean = true
)
