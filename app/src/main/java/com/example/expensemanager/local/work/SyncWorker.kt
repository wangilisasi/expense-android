package com.example.expensemanager.local.work


import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensemanager.api.ExpenseApiService
import com.example.expensemanager.local.daos.ExpenseDao
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.Response

private const val TAG = "SyncWorker"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val apiService: ExpenseApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val unsyncedExpenses = expenseDao.getUnsyncedExpenses()
        val creations = unsyncedExpenses.filter { !it.isDeleted }
        val deletions = unsyncedExpenses.filter { it.isDeleted }
        var shouldRetry = false

        creations.forEach { expense ->
            try {
                val request = ExpenseRequest(
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
                        expenseDao.upsert(
                            expense.copy(
                                id = serverExpense.id,
                                isSynced = true,
                                createdAt = serverExpense.createdAt,
                                updatedAt = serverExpense.updatedAt
                            )
                        )
                    } else {
                        expenseDao.upsert(expense.copy(isSynced = true))
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
}
