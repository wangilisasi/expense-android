package com.example.expensemanager.local.work


import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensemanager.api.ExpenseApiService
import com.example.expensemanager.local.TokenManager
import com.example.expensemanager.local.daos.ExpenseDao
import com.example.expensemanager.local.daos.ExpenseTrackerDao
import com.example.expensemanager.models.DEFAULT_EXPENSE_DESCRIPTION
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseTrackerRequest
import com.example.expensemanager.models.ExpenseUpdateRequest
import com.example.expensemanager.models.normalizeExpenseCategory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response

private const val TAG = "SyncWorker"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val trackerDao: ExpenseTrackerDao,
    private val tokenManager: TokenManager,
    private val apiService: ExpenseApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = tokenManager.getCurrentUserId().firstOrNull() ?: return Result.success()
        var shouldRetry = syncTrackers(userId)
        val unsyncedExpenses = expenseDao.getUnsyncedExpensesForUser(userId)
        val creations = unsyncedExpenses.filter { !it.isDeleted }
        val deletions = unsyncedExpenses.filter { it.isDeleted }

        creations.forEach { expense ->
            try {
                val serverTrackerId = resolveServerTrackerId(expense.trackerId, userId)
                if (serverTrackerId == null) {
                    shouldRetry = true
                    return@forEach
                }

                val updateResponse = updateExpenseOnServer(expense)
                val response = if (updateResponse.code() == 404 || updateResponse.code() == 405) {
                    val request = ExpenseRequest(
                        id = expense.id,
                        description = expense.description.ifBlank { DEFAULT_EXPENSE_DESCRIPTION },
                        amount = expense.amount,
                        date = expense.date,
                        trackerId = serverTrackerId,
                        category = normalizeExpenseCategory(expense.category)
                    )
                    createExpenseOnServer(request, serverTrackerId)
                } else {
                    updateResponse
                }

                if (response.isSuccessful) {
                    val serverExpense = response.body()
                    if (serverExpense != null && serverExpense.id != expense.id) {
                        expenseDao.deletePermanently(expense.id)
                        expenseDao.upsert(
                            expense.copy(
                                id = serverExpense.id,
                                trackerId = expense.trackerId,
                                category = normalizeExpenseCategory(serverExpense.category),
                                isSynced = true,
                                createdAt = serverExpense.createdAt,
                                updatedAt = serverExpense.updatedAt
                            )
                        )
                    } else if (serverExpense != null) {
                        expenseDao.upsert(
                            expense.copy(
                                description = serverExpense.description,
                                amount = serverExpense.amount,
                                date = serverExpense.date,
                                category = normalizeExpenseCategory(serverExpense.category),
                                createdAt = serverExpense.createdAt,
                                updatedAt = serverExpense.updatedAt,
                                isSynced = true,
                                isDeleted = false
                            )
                        )
                    } else {
                        expenseDao.upsert(
                            expense.copy(
                                category = normalizeExpenseCategory(expense.category),
                                isSynced = true
                            )
                        )
                    }
                } else {
                    Log.w(TAG, "Create sync failed for ${expense.id}. code=${response.code()}")
                    if (response.code() >= 500 || response.code() == 429 || response.code() == 401) {
                        shouldRetry = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Create sync exception for ${expense.id}", e)
                shouldRetry = true
            }
        }

        deletions.forEach { expense ->
            try {
                val response = apiService.deleteExpense(expense.id)
                if (response.isSuccessful || response.code() == 404) {
                    expenseDao.deletePermanently(expense.id)
                } else {
                    Log.w(TAG, "Delete sync failed for ${expense.id}. code=${response.code()}")
                    if (response.code() >= 500 || response.code() == 429 || response.code() == 401) {
                        shouldRetry = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Delete sync exception for ${expense.id}", e)
                shouldRetry = true
            }
        }

        return if (shouldRetry) Result.retry() else Result.success()
    }

    private suspend fun syncTrackers(userId: String): Boolean {
        val unsyncedTrackers = trackerDao.getUnsyncedTrackers(userId)
        var shouldRetry = false

        unsyncedTrackers.forEach { tracker ->
            try {
                val request = ExpenseTrackerRequest(
                    name = tracker.name,
                    description = tracker.description,
                    startDate = tracker.startDate,
                    endDate = tracker.endDate,
                    budget = tracker.budget
                )
                val response = if (tracker.serverId == null) {
                    apiService.createTracker(request)
                } else {
                    updateTrackerOnServer(tracker.serverId, request)
                }

                if (response.isSuccessful) {
                    val serverTracker = response.body()
                    val serverId = serverTracker?.id ?: tracker.serverId
                    if (serverId == null) {
                        shouldRetry = true
                        return@forEach
                    }

                    trackerDao.upsert(
                        tracker.copy(
                            serverId = serverId,
                            name = serverTracker?.name ?: tracker.name,
                            description = serverTracker?.description ?: tracker.description,
                            startDate = serverTracker?.startDate ?: tracker.startDate,
                            endDate = serverTracker?.endDate ?: tracker.endDate,
                            budget = serverTracker?.budget ?: tracker.budget,
                            isSynced = true,
                            isDeleted = false
                        )
                    )
                } else {
                    Log.w(
                        TAG,
                        "Tracker sync failed for ${tracker.id}. code=${response.code()}"
                    )
                    if (response.code() >= 500 || response.code() == 429 || response.code() == 401) {
                        shouldRetry = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Tracker sync exception for ${tracker.id}", e)
                shouldRetry = true
            }
        }

        return shouldRetry
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

    private suspend fun updateExpenseOnServer(expense: com.example.expensemanager.local.entities.ExpenseEntity): Response<ExpenseResponse> {
        val request = ExpenseUpdateRequest(
            description = expense.description.ifBlank { DEFAULT_EXPENSE_DESCRIPTION },
            amount = expense.amount,
            date = expense.date,
            category = normalizeExpenseCategory(expense.category)
        )
        return apiService.updateExpense(expense.id, request)
    }

    private suspend fun updateTrackerOnServer(
        serverTrackerId: String,
        request: ExpenseTrackerRequest
    ): Response<com.example.expensemanager.models.ExpenseTrackerResponse> {
        val response = apiService.updateTracker(serverTrackerId, request)
        return when (response.code()) {
            404, 405 -> apiService.updateTrackerPut(serverTrackerId, request)
            400, 422 -> {
                val snakePayload: Map<String, Any> = mapOf(
                    "name" to request.name,
                    "description" to request.description,
                    "start_date" to request.startDate,
                    "end_date" to request.endDate,
                    "budget" to request.budget
                )
                apiService.updateTrackerMap(serverTrackerId, snakePayload).let { patchResponse ->
                    when (patchResponse.code()) {
                        404, 405 -> apiService.updateTrackerPutMap(serverTrackerId, snakePayload)
                        else -> patchResponse
                    }
                }
            }

            else -> response
        }
    }

    private suspend fun resolveServerTrackerId(localTrackerId: String, userId: String): String? {
        val tracker = trackerDao.getTrackerById(localTrackerId, userId) ?: return null
        return tracker.serverId ?: tracker.id.takeIf { tracker.isSynced }
    }
}
