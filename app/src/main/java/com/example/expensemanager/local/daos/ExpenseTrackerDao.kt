package com.example.expensemanager.local.daos

import androidx.room.Dao
import androidx.room.Query
import com.example.expensemanager.local.entities.ExpenseTrackerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTrackerDao {
    @Query("SELECT * FROM expense_trackers")
    fun getAllTrackers(): Flow<List<ExpenseTrackerEntity>>
}