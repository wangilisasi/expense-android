package com.example.expensemanager.models

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("uuid_id")
    val id: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String
)
