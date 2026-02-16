package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class ExpenseRequest(
    @SerializedName("uuid_id")
    val id: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("uuid_tracker_id")
    val trackerId: String,
    // Some backend variants use "name" instead of "description".
    @SerializedName("name")
    val name: String = description,
    // Some backend variants use "tracker_uuid_id" in request payloads.
    @SerializedName("tracker_uuid_id")
    val trackerUuidId: String = trackerId,
    // Compatibility alias for backends that use "client_uuid_id" naming.
    @SerializedName("client_uuid_id")
    val clientUuidId: String = id
)
