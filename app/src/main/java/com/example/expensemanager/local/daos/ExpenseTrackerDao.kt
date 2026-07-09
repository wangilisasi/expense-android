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
    @Query("SELECT * FROM expense_trackers WHERE userId = :userId ORDER BY startDate DESC")
    fun getAllTrackers(userId: String): Flow<List<ExpenseTrackerEntity>>

    @Query("SELECT * FROM expense_trackers WHERE userId = :userId ORDER BY startDate DESC")
    suspend fun getAllTrackersOnce(userId: String): List<ExpenseTrackerEntity>

    @Query("SELECT * FROM expense_trackers WHERE id = :trackerId AND userId = :userId LIMIT 1")
    suspend fun getTrackerById(trackerId: String, userId: String): ExpenseTrackerEntity?

    @Query("SELECT * FROM expense_trackers WHERE serverId = :serverId AND userId = :userId LIMIT 1")
    suspend fun getTrackerByServerId(serverId: String, userId: String): ExpenseTrackerEntity?

    @Query("SELECT * FROM expense_trackers WHERE userId = :userId ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestTracker(userId: String): ExpenseTrackerEntity?

    @Query("SELECT * FROM expense_trackers WHERE userId = :userId AND isSynced = 0 ORDER BY startDate DESC")
    suspend fun getUnsyncedTrackers(userId: String): List<ExpenseTrackerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tracker: ExpenseTrackerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(trackers: List<ExpenseTrackerEntity>)

    @Query("DELETE FROM expense_trackers")
    suspend fun clearAll()

    @Query("DELETE FROM expense_trackers WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("UPDATE expense_trackers SET userId = :userId WHERE userId = ''")
    suspend fun assignLegacyTrackersToUser(userId: String)

    @Transaction
    suspend fun replaceAllForUser(userId: String, trackers: List<ExpenseTrackerEntity>) {
        clearForUser(userId)
        upsertAll(trackers)
    }
}
