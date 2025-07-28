package com.example.expensemanager.models


import com.google.gson.annotations.SerializedName

data class ExpenseTrackerRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("startDate")
    val startDate: String, // Consider parsing to Date/LocalDate
    @SerializedName("endDate")
    val endDate: String,   // Consider parsing to Date/LocalDate
    @SerializedName("budget")
    val budget: Double,
)
