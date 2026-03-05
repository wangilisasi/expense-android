package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class CategoryAnalyticsResponse(
    @SerializedName("tracker_uuid_id")
    val trackerId: String,
    @SerializedName("total_expenditure")
    val totalExpenditure: Double,
    @SerializedName("categories")
    val categories: List<CategoryAnalyticsItem> = emptyList()
)

data class CategoryAnalyticsItem(
    @SerializedName("category")
    val category: String,
    @SerializedName("total_amount")
    val totalAmount: Double,
    @SerializedName("percentage")
    val percentage: Double,
    @SerializedName("expense_count")
    val expenseCount: Int
)
