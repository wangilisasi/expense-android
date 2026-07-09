package com.example.expensemanager.local.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.expensemanager.local.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // The single source of truth for the UI. It's a Flow!
    // We only show items that are NOT marked for deletion.
    @Query("SELECT * FROM expenses WHERE trackerId = :trackerId AND isDeleted = false ORDER BY date DESC, occurredAt DESC")
    fun getExpensesForTracker(trackerId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE trackerId = :trackerId AND isDeleted = false ORDER BY date DESC, occurredAt DESC")
    suspend fun getExpensesForTrackerOnce(trackerId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    // Insert or update one or more expenses.
    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    // --- Methods for the Sync Worker ---

    @Query("SELECT * FROM expenses WHERE isSynced = false")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>

    @Query(
        """
        SELECT expenses.* FROM expenses
        INNER JOIN expense_trackers ON expenses.trackerId = expense_trackers.id
        WHERE expenses.isSynced = false AND expense_trackers.userId = :userId
        """
    )
    suspend fun getUnsyncedExpensesForUser(userId: String): List<ExpenseEntity>

    // --- Methods for Deletion ---

    // 1. User action: Mark an item for deletion (soft delete).
    @Query("UPDATE expenses SET isDeleted = true, isSynced = false WHERE id = :expenseId")
    suspend fun markAsDeleted(expenseId: String)

    // 2. Worker action: After successful API call, delete it permanently.
    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deletePermanently(expenseId: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()

    @Query(
        """
        DELETE FROM expenses
        WHERE trackerId IN (
            SELECT id FROM expense_trackers WHERE userId = :userId
        )
        """
    )
    suspend fun clearForUser(userId: String)
}
