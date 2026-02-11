package com.example.expensemanager.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.expensemanager.local.entities.ExpenseTrackerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTrackerDao {
    @Query("SELECT * FROM expense_trackers ORDER BY startDate DESC")
    fun getAllTrackers(): Flow<List<ExpenseTrackerEntity>>

    @Query("SELECT * FROM expense_trackers ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestTracker(): ExpenseTrackerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tracker: ExpenseTrackerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(trackers: List<ExpenseTrackerEntity>)

    @Query("DELETE FROM expense_trackers")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(trackers: List<ExpenseTrackerEntity>) {
        clearAll()
        upsertAll(trackers)
    }
}
