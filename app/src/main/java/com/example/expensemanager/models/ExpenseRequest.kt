package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class ExpenseRequest(
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("uuid_tracker_id")
    val trackerId: String
    // Notice: NO 'id' field here
)