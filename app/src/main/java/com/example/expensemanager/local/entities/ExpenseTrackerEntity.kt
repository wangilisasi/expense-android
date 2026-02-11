package com.example.expensemanager.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_trackers")
data class ExpenseTrackerEntity(
    @PrimaryKey
    val id: String, // UUID, also used as server ID
    val userId: String, // Foreign key to the user
    val name: String,
    val budget: Double,
    val description: String,
    val startDate: String,
    val endDate: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)