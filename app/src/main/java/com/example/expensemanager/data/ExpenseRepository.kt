package com.example.expensemanager.data

import android.content.Context
import android.util.Log
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
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseTrackerRequest
import com.example.expensemanager.models.ExpenseTrackerResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ExpenseRepository"
private const val EXPENSE_SYNC_WORK = "expense_sync_work"
private const val UNKNOWN_USER_ID = ""

class ExpenseRepository @Inject constructor(
    private val apiService: ExpenseApiService,
    private val expenseDao: ExpenseDao,
    private val expenseTrackerDao: ExpenseTrackerDao,
    @ApplicationContext private val context: Context,
) {
    fun getTrackers(): Flow<List<ExpenseTrackerResponse>> {
        return expenseTrackerDao.getAllTrackers().map { entities ->
            entities.map { entity -> entity.toResponse() }
        }
    }

    suspend fun getLatestTracker(): ExpenseTrackerResponse? {
        return expenseTrackerDao.getLatestTracker()?.toResponse()
    }

    suspend fun refreshTrackers() {
        val trackers = apiService.getTrackers()
        expenseTrackerDao.replaceAll(trackers.map { it.toEntity(isSynced = true) })
    }

    suspend fun syncAllFromBackend() {
        val trackers = apiService.getTrackers()
        expenseTrackerDao.replaceAll(trackers.map { it.toEntity(isSynced = true) })

        trackers.forEach { tracker ->
            try {
                val networkExpenses = apiService.getExpensesForTracker(tracker.id)
                mergeServerExpenses(tracker.id, networkExpenses)
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync expenses for tracker ${tracker.id}", e)
            }
        }
    }

    suspend fun createTracker(
        name: String,
        budget: Double,
        startDate: String,
        endDate: String
    ): ExpenseTrackerResponse {
        val response = apiService.createTracker(
            ExpenseTrackerRequest(
                name = name,
                description = name,
                startDate = startDate,
                endDate = endDate,
                budget = budget
            )
        )

        if (!response.isSuccessful) {
            throw IllegalStateException("Failed to create tracker: ${response.code()}")
        }
        val createdTracker = response.body()
            ?: throw IllegalStateException("Failed to create tracker: ${response.code()}")

        expenseTrackerDao.upsert(createdTracker.toEntity(isSynced = response.isSuccessful))
        return createdTracker
    }

    suspend fun updateTracker(
        trackerId: String,
        name: String,
        budget: Double,
        startDate: String,
        endDate: String
    ): ExpenseTrackerResponse {
        val request = ExpenseTrackerRequest(
            name = name,
            description = name,
            startDate = startDate,
            endDate = endDate,
            budget = budget
        )
        val response = apiService.updateTracker(
            trackerId = trackerId,
            expenseTrackerRequest = request
        ).let { patchResponse ->
            when (patchResponse.code()) {
                404, 405 -> {
                    apiService.updateTrackerPut(
                        trackerId = trackerId,
                        expenseTrackerRequest = request
                    )
                }
                400, 422 -> {
                    updateTrackerSnakeCase(trackerId, name, budget, startDate, endDate)
                }
                else -> patchResponse
            }
        }

        if (!response.isSuccessful) {
            val detail = response.errorBody()?.string().orEmpty()
            val message = if (detail.isBlank()) {
                "Failed to update tracker: ${response.code()}"
            } else {
                "Failed to update tracker: ${response.code()} - $detail"
            }
            throw IllegalStateException(message)
        }
        val updatedTracker = response.body()
            ?: runCatching { apiService.getTrackerDetails(trackerId) }
                .getOrElse {
                    ExpenseTrackerResponse(
                        id = trackerId,
                        name = name,
                        description = name,
                        startDate = startDate,
                        endDate = endDate,
                        budget = budget,
                        expenses = emptyList()
                    )
                }

        expenseTrackerDao.upsert(updatedTracker.toEntity(isSynced = response.isSuccessful))
        return updatedTracker
    }

    private suspend fun updateTrackerSnakeCase(
        trackerId: String,
        name: String,
        budget: Double,
        startDate: String,
        endDate: String
    ): Response<ExpenseTrackerResponse> {
        val snakePayload: Map<String, Any> = mapOf(
            "name" to name,
            "description" to name,
            "start_date" to startDate,
            "end_date" to endDate,
            "budget" to budget
        )

        val patchResponse = apiService.updateTrackerMap(
            trackerId = trackerId,
            expenseTrackerRequest = snakePayload
        )
        return when (patchResponse.code()) {
            404, 405 -> {
                apiService.updateTrackerPutMap(
                    trackerId = trackerId,
                    expenseTrackerRequest = snakePayload
                )
            }
            else -> patchResponse
        }
    }

    fun getExpenses(trackerId: String): Flow<List<ExpenseResponse>> {
        return expenseDao.getExpensesForTracker(trackerId).map { entities ->
            entities.map { entity ->
                ExpenseResponse(
                    id = entity.id,
                    description = entity.description,
                    amount = entity.amount,
                    date = entity.date,
                    trackerId = entity.trackerId,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    isSynced = entity.isSynced
                )
            }
        }
    }

    suspend fun addExpense(description: String, amount: Double, date: String, trackerId: String) {
        val newExpense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            description = description,
            amount = amount,
            date = date,
            trackerId = trackerId,
            isSynced = false,
            isDeleted = false,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString(),
        )
        expenseDao.upsert(newExpense)
        val syncedNow = trySyncExpense(newExpense)
        if (!syncedNow) {
            scheduleSync()
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        expenseDao.markAsDeleted(expenseId)
        scheduleSync()
    }

    fun triggerExpenseSync() {
        scheduleSync()
    }

    suspend fun refreshExpenses(trackerId: String) {
        try {
            val networkExpenses = apiService.getExpensesForTracker(trackerId)
            mergeServerExpenses(trackerId, networkExpenses)
        } catch (e: Exception) {
            Log.w(TAG, "Could not refresh expenses for tracker $trackerId", e)
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
            EXPENSE_SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    private suspend fun trySyncExpense(expense: ExpenseEntity): Boolean {
        return try {
            val request = ExpenseRequest(
                id = expense.id,
                description = expense.description,
                amount = expense.amount,
                date = expense.date,
                trackerId = expense.trackerId
            )
            val response = createExpenseOnServer(request, expense.trackerId)
            if (response.isSuccessful) {
                val serverExpense = response.body()
                if (serverExpense != null && serverExpense.id != expense.id) {
                    expenseDao.deletePermanently(expense.id)
                    expenseDao.upsert(serverExpense.toEntity())
                } else {
                    expenseDao.upsert(expense.copy(isSynced = true))
                }
                true
            } else {
                Log.w(TAG, "Immediate sync failed for ${expense.id}. code=${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync exception for ${expense.id}", e)
            false
        }
    }

    private suspend fun createExpenseOnServer(
        request: ExpenseRequest,
        trackerId: String
    ): Response<ExpenseResponse> {
        val response = apiService.addExpense(request)
        return if (response.code() == 404 || response.code() == 405) {
            apiService.addExpenseForTracker(trackerId = trackerId, expenseRequest = request)
        } else {
            response
        }
    }

    private suspend fun mergeServerExpenses(
        trackerId: String,
        networkExpenses: List<ExpenseResponse>
    ) {
        val serverEntities = networkExpenses.map { it.toEntity() }
        val serverIds = serverEntities.map { it.id }.toSet()
        val serverSignatures = serverEntities.map { it.signature() }.toSet()

        val localExpenses = expenseDao.getExpensesForTrackerOnce(trackerId)
        localExpenses
            .filter { local ->
                local.id !in serverIds && local.signature() in serverSignatures
            }
            .forEach { duplicate ->
                expenseDao.deletePermanently(duplicate.id)
            }

        expenseDao.upsertAll(serverEntities)
    }
}

private fun ExpenseTrackerResponse.toEntity(isSynced: Boolean): ExpenseTrackerEntity {
    return ExpenseTrackerEntity(
        id = id,
        userId = UNKNOWN_USER_ID,
        name = name,
        budget = budget,
        description = description,
        startDate = startDate,
        endDate = endDate,
        isSynced = isSynced,
        isDeleted = false
    )
}

private fun ExpenseTrackerEntity.toResponse(): ExpenseTrackerResponse {
    return ExpenseTrackerResponse(
        id = id,
        name = name,
        description = description,
        startDate = startDate,
        endDate = endDate,
        budget = budget,
        expenses = emptyList()
    )
}

private fun ExpenseResponse.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        description = description,
        amount = amount,
        date = date,
        trackerId = trackerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = true,
        isDeleted = false
    )
}

private fun ExpenseEntity.signature(): String {
    return "$trackerId|$date|$amount|$description"
}
