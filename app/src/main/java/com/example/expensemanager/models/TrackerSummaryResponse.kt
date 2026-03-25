package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class TrackerSummaryResponse(
    @SerializedName("uuid_id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String,
    @SerializedName("budget")
    val budget: Double
)
