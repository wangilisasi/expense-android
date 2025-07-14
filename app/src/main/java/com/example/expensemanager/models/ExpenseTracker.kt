package com.example.expensemanager.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.util.UUID

data class ExpenseTracker(
    @SerializedName("id")
    val id: Int,
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
    val expenses: List<Expense> // List of the nested expense objects
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