package com.example.expensemanager.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    //TODO: Later add sync fields to cater for when a user changes their profile while offline
    //val isSynced: Boolean = false,        // Profile data synced with server
    val createdAt: String,
    val updatedAt: String
)