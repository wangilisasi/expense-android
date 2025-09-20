package com.example.expensemanager.models


import com.google.gson.annotations.SerializedName


data class ExpenseTrackerResponse(
    @SerializedName("uuid_id")
    val id: String,
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
    @SerializedName("expenses")
    val expenses: List<ExpenseResponse> // List of the nested expense objects
)

// Represents the 'ExpenseTracker' table
//data class ExpenseTracker(
//    val id: Int,
//    val startDate: LocalDate,
//    val endDate: LocalDate,
//    val budget: Double,
//    val name: String,
//    val description: String?
//)