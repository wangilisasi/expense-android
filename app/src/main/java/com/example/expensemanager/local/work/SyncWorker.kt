package com.example.expensemanager.local.work


import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensemanager.api.ExpenseApiService
import com.example.expensemanager.local.daos.ExpenseDao
import com.example.expensemanager.models.ExpenseRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val apiService: ExpenseApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val unsyncedExpenses = expenseDao.getUnsyncedExpenses()

            val creations = unsyncedExpenses.filter { !it.isDeleted }
            val deletions = unsyncedExpenses.filter { it.isDeleted }

            // Sync Creations/Updates
            creations.forEach { expense ->
                val request = ExpenseRequest(expense.description, expense.amount,expense.date, expense.trackerId,)
                val response = apiService.addExpense(request) // Assuming addExpense also handles updates (like a PUT/PATCH)
                if (response.isSuccessful) {
                    expenseDao.upsert(expense.copy(isSynced = true))
                }
            }

            // Sync Deletions
            deletions.forEach { expense ->
                val response = apiService.deleteExpense(expense.id)
                // If successful or if the item is not found (404), it's safe to delete locally
                if (response.isSuccessful || response.code() == 404) {
                    expenseDao.deletePermanently(expense.id)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            // If the network fails, WorkManager will automatically retry this job.
            return Result.retry()
        }
    }
}