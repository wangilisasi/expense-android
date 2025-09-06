package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName



data class StatsResponse(
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    @SerializedName("budget")
    val budget: Double,
    @SerializedName("remaining_days")
    val remainingDays: Int,
    @SerializedName("target_expenditure_per_day")
    val targetExpenditurePerDay: Double,
    @SerializedName("total_expenditure")
    val totalExpenditure: Double,
    @SerializedName("todays_expenditure")
    val todaysExpenditure: Double
)
