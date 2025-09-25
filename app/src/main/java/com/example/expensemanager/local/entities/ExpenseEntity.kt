package com.example.expensemanager.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String, // The UUID, same as server
    val description: String,
    val amount: Double,
    val date: String,
    val trackerId: String,
    val createdAt: String,
    val updatedAt: String,

    // --- Offline-First Flags ---
    // Tracks if this entity has been successfully sent to the server.
    val isSynced: Boolean = false,
    // For soft deletes. We mark it for deletion and sync the delete action.
    val isDeleted: Boolean = false
)