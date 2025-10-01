package com.example.expensemanager.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensemanager.api.ExpenseApiService
import com.example.expensemanager.local.daos.ExpenseDao
import com.example.expensemanager.local.daos.ExpenseTrackerDao
import com.example.expensemanager.local.entities.ExpenseEntity
import com.example.expensemanager.local.entities.ExpenseTrackerEntity
import com.example.expensemanager.local.work.SyncWorker
import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseTrackerRequest
import com.example.expensemanager.models.ExpenseTrackerResponse
import com.example.expensemanager.models.StatsResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject

// The repository will be injected with the API service
class ExpenseRepository @Inject constructor(private val apiService: ExpenseApiService,
                                            private val expenseDao: ExpenseDao,
                                            private val expenseTrackerDao: ExpenseTrackerDao,
                                            @ApplicationContext private val context: Context
    ) {
    suspend fun getTrackers(): Flow<List<ExpenseTrackerResponse>> {
        return expenseTrackerDao.getAllTrackers().map { entities ->
            entities.map { entity ->
                ExpenseTrackerResponse(entity.id,  entity.name, entity.description, entity.startDate, entity.endDate,entity.budget,
                    emptyList())
            }
        }
    }

    // --- READS: Always from the local database (Single Source of Truth) ---
    fun getExpenses(trackerId: String): Flow<List<ExpenseResponse>> {
        return expenseDao.getExpensesForTracker(trackerId).map { entities ->
            // Map from database entity to your UI model
            entities.map { entity ->
                ExpenseResponse(entity.id, entity.description, entity.amount, entity.date, entity.trackerId, entity.createdAt, entity.updatedAt)
            }
        }
    }

    // --- WRITES: Go to DB first, then schedule a sync ---
    suspend fun addExpense(description: String, amount: Double, date: String, trackerId: String) {
        val newExpense = ExpenseEntity(
            id = UUID.randomUUID().toString(), // Generate the ID on the client!
            description = description,
            amount = amount,
            date = date,
            trackerId = trackerId,
            isSynced = false, // Mark as unsynced
            isDeleted = false,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString(),
        )
        expenseDao.upsert(newExpense)
        scheduleSync()
    }

    // Get tracker stats
    //suspend fun getStats(trackerId: String): StatsResponse {
      //  return apiService.getStatsForTracker(trackerId)
    //}

    //suspend fun createTracker(trackerRequest: ExpenseTrackerRequest): Response<ExpenseTrackerResponse> {
      //  return apiService.createTracker(trackerRequest)
    //}

    //suspend fun addExpense(expenseRequest: ExpenseRequest) {
      //  apiService.addExpense(expenseRequest)
    //}

    //suspend fun deleteExpense(expenseId: String) {
      //  apiService.deleteExpense(expenseId)
    //}

    //suspend fun updateTracker(trackerId: String, expenseTrackerRequest: ExpenseTrackerRequest): Response<ExpenseTrackerResponse> {
      //  return apiService.updateTracker(trackerId, expenseTrackerRequest)
    //}

    //suspend fun getDailyExpenses(trackerId: String): DailyExpensesResponse {
      //  return apiService.getDailyExpensesForTracker(trackerId)
    //}

    // --- SYNCING ---
    suspend fun refreshExpenses(trackerId: String) {
        try {
            val networkExpenses = apiService.getExpensesForTracker(trackerId)
            val entities = networkExpenses.map { response ->
                ExpenseEntity(
                    id = response.id,
                    description = response.description,
                    amount = response.amount,
                    date = response.date,
                    trackerId = response.trackerId,
                    isSynced = true, // Data from the server is always considered synced
                    isDeleted = false,
                    createdAt = response.createdAt,
                    updatedAt = response.updatedAt,
                )
            }
            expenseDao.upsertAll(entities)
        } catch (e: Exception) {
            // Handle network error, maybe log it. The UI will still show cached data.
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "expense_sync_work",
            ExistingWorkPolicy.KEEP, // If a sync is already running, let it finish
            syncRequest
        )
    }


    // ... other functions like deleteExpense, etc.
}