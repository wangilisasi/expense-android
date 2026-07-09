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
import com.example.expensemanager.local.TokenManager
import com.example.expensemanager.local.work.SyncWorker
import com.example.expensemanager.models.CategoryAnalyticsItem
import com.example.expensemanager.models.CategoryAnalyticsResponse
import com.example.expensemanager.models.DEFAULT_EXPENSE_CATEGORY
import com.example.expensemanager.models.DEFAULT_EXPENSE_DESCRIPTION
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseTrackerRequest
import com.example.expensemanager.models.ExpenseTrackerResponse
import com.example.expensemanager.models.ExpenseUpdateRequest
import com.example.expensemanager.models.FALLBACK_EXPENSE_CATEGORIES
import com.example.expensemanager.models.TrackerSummaryResponse
import com.example.expensemanager.models.normalizeExpenseCategories
import com.example.expensemanager.models.normalizeExpenseCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ExpenseRepository"
private const val EXPENSE_SYNC_WORK = "expense_sync_work"
private const val OVERLAP_BUDGET_MESSAGE =
    "These budget dates overlap an existing budget. Choose a different date range."

sealed interface ActiveTrackerResult {
    data class Found(val tracker: TrackerSummaryResponse) : ActiveTrackerResult
    data object None : ActiveTrackerResult
}

class ExpenseRepository @Inject constructor(
    private val apiService: ExpenseApiService,
    private val expenseDao: ExpenseDao,
    private val expenseTrackerDao: ExpenseTrackerDao,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTrackers(): Flow<List<TrackerSummaryResponse>> {
        return tokenManager.getCurrentUserId().flatMapLatest { userId ->
            if (userId == null) {
                emptyFlow()
            } else {
                expenseTrackerDao.getAllTrackers(userId).map { entities ->
                    entities.map { entity -> entity.toSummary() }
                }
            }
        }
    }

    suspend fun getCachedTrackers(): List<TrackerSummaryResponse> {
        val userId = requireCurrentUserId()
        expenseTrackerDao.assignLegacyTrackersToUser(userId)
        return expenseTrackerDao.getAllTrackersOnce(userId).map { entity -> entity.toSummary() }
    }

    suspend fun refreshTrackers() {
        val userId = requireCurrentUserId()
        expenseTrackerDao.assignLegacyTrackersToUser(userId)
        val trackers = apiService.getTrackers()
        mergeNetworkTrackers(trackers, userId)
    }

    suspend fun clearLocalData() {
        val userId = currentUserIdOrNull() ?: return
        expenseDao.clearForUser(userId)
        expenseTrackerDao.clearForUser(userId)
    }

    suspend fun getActiveTracker(): ActiveTrackerResult {
        val response = apiService.getActiveTracker()
        if (response.isSuccessful) {
            val tracker = response.body()
                ?: throw ApiException(500, "Active budget response was empty")
            val localTracker = mergeNetworkTracker(tracker, userId = requireCurrentUserId())
            return ActiveTrackerResult.Found(localTracker.toSummary())
        }

        if (response.code() == 404) {
            return ActiveTrackerResult.None
        }

        throw ApiException(
            code = response.code(),
            message = response.errorMessage("Failed to load active budget")
        )
    }

    suspend fun syncAllFromBackend() {
        val userId = requireCurrentUserId()
        val trackers = apiService.getTrackers()
        val mergedTrackers = mergeNetworkTrackers(trackers, userId)

        mergedTrackers.forEach { tracker ->
            try {
                val serverTrackerId = tracker.serverId ?: tracker.id.takeIf { tracker.isSynced } ?: return@forEach
                val networkExpenses = apiService.getExpensesForTracker(serverTrackerId)
                mergeServerExpenses(tracker.id, networkExpenses)
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync expenses for tracker ${tracker.id}", e)
            }
        }
    }

    suspend fun getCategories(): List<String> {
        return runCatching { apiService.getCategories() }
            .getOrElse { FALLBACK_EXPENSE_CATEGORIES }
            .let(::normalizeExpenseCategories)
            .filter { it.isNotBlank() }
            .ifEmpty { FALLBACK_EXPENSE_CATEGORIES }
    }

    suspend fun getCategoryAnalyticsForTracker(trackerId: String): CategoryAnalyticsResponse? {
        val serverTrackerId = resolveServerTrackerId(trackerId) ?: return null
        return runCatching { apiService.getCategoryAnalyticsForTracker(serverTrackerId) }
            .onFailure { Log.w(TAG, "Could not load category analytics for tracker $trackerId", it) }
            .getOrNull()
            ?.let { analytics ->
                val totalExpenditure = analytics.totalExpenditure
                val mergedCategories = analytics.categories
                    .groupBy { item -> normalizeExpenseCategory(item.category) }
                    .map { (category, items) ->
                        val totalAmount = items.sumOf { it.totalAmount }
                        CategoryAnalyticsItem(
                            category = category,
                            totalAmount = totalAmount,
                            percentage = if (totalExpenditure > 0.0) {
                                (totalAmount / totalExpenditure) * 100.0
                            } else {
                                0.0
                            },
                            expenseCount = items.sumOf { it.expenseCount }
                        )
                    }
                    .sortedByDescending { it.totalAmount }

                analytics.copy(categories = mergedCategories)
            }
    }

    suspend fun createTracker(
        name: String,
        budget: Double,
        startDate: String,
        endDate: String
    ): TrackerSummaryResponse {
        val userId = requireCurrentUserId()
        val request = ExpenseTrackerRequest(
            name = name,
            description = name,
            startDate = startDate,
            endDate = endDate,
            budget = budget
        )

        return try {
            val response = apiService.createTracker(request)
            if (!response.isSuccessful) {
                throw ApiException(
                    code = response.code(),
                    message = trackerSaveErrorMessage(response, "Failed to create budget")
                )
            }

            val createdTracker = response.body()
                ?: throw ApiException(500, "Budget was created but no tracker was returned")

            val localTracker = mergeNetworkTracker(
                networkTracker = createdTracker.toSummary(),
                userId = userId
            )
            localTracker.toSummary()
        } catch (exception: Exception) {
            if (exception is ApiException || exception !is IOException) {
                throw exception
            }

            val localTracker = ExpenseTrackerEntity(
                id = UUID.randomUUID().toString(),
                serverId = null,
                userId = userId,
                name = name,
                budget = budget,
                description = name,
                startDate = startDate,
                endDate = endDate,
                isSynced = false,
                isDeleted = false
            )
            expenseTrackerDao.upsert(localTracker)
            scheduleSync()
            localTracker.toSummary()
        }
    }

    suspend fun updateTracker(
        trackerId: String,
        name: String,
        budget: Double,
        startDate: String,
        endDate: String
    ): TrackerSummaryResponse {
        val request = ExpenseTrackerRequest(
            name = name,
            description = name,
            startDate = startDate,
            endDate = endDate,
            budget = budget
        )
        val userId = requireCurrentUserId()
        val existing = expenseTrackerDao.getTrackerById(trackerId, userId)
            ?: throw ApiException(404, "Tracker not found")

        if (existing.serverId == null) {
            val localTracker = existing.copy(
                name = name,
                budget = budget,
                description = name,
                startDate = startDate,
                endDate = endDate,
                isSynced = false
            )
            expenseTrackerDao.upsert(localTracker)

            return try {
                val response = apiService.createTracker(request)
                if (!response.isSuccessful) {
                    throw ApiException(
                        code = response.code(),
                        message = trackerSaveErrorMessage(response, "Failed to update budget")
                    )
                }

                val createdTracker = response.body()
                    ?: throw ApiException(500, "Budget was created but no tracker was returned")
                val mergedTracker = mergeNetworkTracker(
                    networkTracker = createdTracker.toSummary(),
                    preferredLocalId = trackerId,
                    userId = userId
                )
                mergedTracker.toSummary()
            } catch (exception: Exception) {
                if (exception is ApiException || exception !is IOException) {
                    throw exception
                }
                scheduleSync()
                localTracker.toSummary()
            }
        }

        return try {
            val serverTrackerId = existing.serverId
            val response = apiService.updateTracker(
                trackerId = serverTrackerId,
                expenseTrackerRequest = request
            ).let { patchResponse ->
                when (patchResponse.code()) {
                    404, 405 -> {
                        apiService.updateTrackerPut(
                            trackerId = serverTrackerId,
                            expenseTrackerRequest = request
                        )
                    }

                    400, 422 -> {
                        updateTrackerSnakeCase(serverTrackerId, name, budget, startDate, endDate)
                    }

                    else -> patchResponse
                }
            }

            if (!response.isSuccessful) {
                throw ApiException(
                    code = response.code(),
                    message = trackerSaveErrorMessage(response, "Failed to update budget")
                )
            }

            val updatedTracker = response.body()
                ?: runCatching { apiService.getTrackerDetails(serverTrackerId) }
                    .getOrElse {
                        ExpenseTrackerResponse(
                            id = serverTrackerId,
                            name = name,
                            description = name,
                            startDate = startDate,
                            endDate = endDate,
                            budget = budget,
                            expenses = emptyList()
                        )
                    }

            val mergedTracker = mergeNetworkTracker(
                networkTracker = updatedTracker.toSummary(),
                preferredLocalId = trackerId,
                userId = userId
            )
            mergedTracker.toSummary()
        } catch (exception: Exception) {
            if (exception is ApiException || exception !is IOException) {
                throw exception
            }

            val localTracker = existing.copy(
                name = name,
                budget = budget,
                description = name,
                startDate = startDate,
                endDate = endDate,
                isSynced = false
            )
            expenseTrackerDao.upsert(localTracker)
            scheduleSync()
            localTracker.toSummary()
        }
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
                    category = normalizeExpenseCategory(entity.category),
                    trackerId = entity.trackerId,
                    occurredAt = entity.occurredAt,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    isSynced = entity.isSynced
                )
            }
        }
    }

    suspend fun addExpense(
        description: String,
        amount: Double,
        date: String,
        trackerId: String,
        category: String = DEFAULT_EXPENSE_CATEGORY
    ) {
        val normalizedCategory = normalizeExpenseCategory(category)
        val now = Instant.now().toString()
        val newExpense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            description = description,
            amount = amount,
            date = date,
            category = normalizedCategory,
            trackerId = trackerId,
            isSynced = false,
            isDeleted = false,
            occurredAt = now,
            createdAt = now,
            updatedAt = now,
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

    suspend fun updateExpense(
        expenseId: String,
        description: String,
        amount: Double,
        date: String,
        category: String = DEFAULT_EXPENSE_CATEGORY
    ) {
        val existing = expenseDao.getExpenseById(expenseId)
            ?: throw ApiException(404, "Expense not found")
        val now = Instant.now().toString()
        val normalizedCategory = normalizeExpenseCategory(category)
        val updatedExpense = existing.copy(
            description = description,
            amount = amount,
            date = date,
            category = normalizedCategory,
            updatedAt = now,
            isSynced = false
        )
        expenseDao.upsert(updatedExpense)

        val syncedNow = trySyncExpenseUpdate(updatedExpense)
        if (!syncedNow) {
            scheduleSync()
        }
    }

    fun triggerExpenseSync() {
        scheduleSync()
    }

    suspend fun refreshExpenses(trackerId: String) {
        val serverTrackerId = resolveServerTrackerId(trackerId) ?: return
        try {
            val networkExpenses = apiService.getExpensesForTracker(serverTrackerId)
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
            val serverTrackerId = resolveServerTrackerId(expense.trackerId) ?: return false
            val request = ExpenseRequest(
                id = expense.id,
                description = expense.description.ifBlank { DEFAULT_EXPENSE_DESCRIPTION },
                amount = expense.amount,
                date = expense.date,
                occurredAt = expense.occurredAt,
                trackerId = serverTrackerId,
                category = normalizeExpenseCategory(expense.category)
            )
            val response = createExpenseOnServer(request, serverTrackerId)
            if (response.isSuccessful) {
                val serverExpense = response.body()
                if (serverExpense != null && serverExpense.id != expense.id) {
                    expenseDao.deletePermanently(expense.id)
                    expenseDao.upsert(serverExpense.toEntity(localTrackerId = expense.trackerId))
                } else {
                    expenseDao.upsert(
                        expense.copy(
                            category = normalizeExpenseCategory(expense.category),
                            isSynced = true
                        )
                    )
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

    private suspend fun trySyncExpenseUpdate(expense: ExpenseEntity): Boolean {
        return try {
            val response = updateExpenseOnServer(expense)
            if (response.isSuccessful) {
                response.body()?.let { serverExpense ->
                    expenseDao.upsert(serverExpense.toEntity(localTrackerId = expense.trackerId))
                } ?: expenseDao.upsert(
                    expense.copy(
                        category = normalizeExpenseCategory(expense.category),
                        isSynced = true
                    )
                )
                true
            } else if (response.code() == 404) {
                trySyncExpense(expense)
            } else {
                Log.w(TAG, "Immediate update sync failed for ${expense.id}. code=${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate update sync exception for ${expense.id}", e)
            false
        }
    }

    private suspend fun updateExpenseOnServer(expense: ExpenseEntity): Response<ExpenseResponse> {
        val request = ExpenseUpdateRequest(
            description = expense.description.ifBlank { DEFAULT_EXPENSE_DESCRIPTION },
            amount = expense.amount,
            date = expense.date,
            occurredAt = expense.occurredAt,
            category = normalizeExpenseCategory(expense.category)
        )
        return apiService.updateExpense(expense.id, request)
    }

    private suspend fun mergeServerExpenses(
        trackerId: String,
        networkExpenses: List<ExpenseResponse>
    ) {
        val serverEntities = networkExpenses.map { it.toEntity(localTrackerId = trackerId) }
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

    private fun trackerSaveErrorMessage(
        response: Response<*>,
        fallback: String
    ): String {
        return if (response.code() == 409) {
            OVERLAP_BUDGET_MESSAGE
        } else {
            response.errorMessage(fallback)
        }
    }

    private suspend fun resolveServerTrackerId(localTrackerId: String): String? {
        val userId = requireCurrentUserId()
        val tracker = expenseTrackerDao.getTrackerById(localTrackerId, userId) ?: return null
        return tracker.serverId ?: tracker.id.takeIf { tracker.isSynced }
    }

    private suspend fun mergeNetworkTrackers(
        networkTrackers: List<TrackerSummaryResponse>,
        userId: String
    ): List<ExpenseTrackerEntity> {
        val localTrackers = expenseTrackerDao.getAllTrackersOnce(userId)
        val localByServerId = localTrackers.mapNotNull { tracker ->
            tracker.serverId?.let { serverId -> serverId to tracker }
        }.toMap()
        val localById = localTrackers.associateBy { it.id }
        val merged = mutableListOf<ExpenseTrackerEntity>()
        val mergedIds = mutableSetOf<String>()

        networkTrackers.forEach { tracker ->
            val existing = localByServerId[tracker.id] ?: localById[tracker.id]
            val mergedTracker = mergeWithExistingTracker(tracker, existing, userId)
            merged += mergedTracker
            mergedIds += mergedTracker.id
        }

        localTrackers
            .filter { tracker -> tracker.id !in mergedIds && !tracker.isSynced }
            .forEach { tracker ->
                merged += tracker
            }

        expenseTrackerDao.replaceAllForUser(userId, merged)
        return merged
    }

    private suspend fun mergeNetworkTracker(
        networkTracker: TrackerSummaryResponse,
        preferredLocalId: String? = null,
        userId: String
    ): ExpenseTrackerEntity {
        val existing = preferredLocalId?.let { expenseTrackerDao.getTrackerById(it, userId) }
            ?: expenseTrackerDao.getTrackerByServerId(networkTracker.id, userId)
            ?: expenseTrackerDao.getTrackerById(networkTracker.id, userId)
        val mergedTracker = mergeWithExistingTracker(networkTracker, existing, userId)
        expenseTrackerDao.upsert(mergedTracker)
        return mergedTracker
    }

    private fun mergeWithExistingTracker(
        networkTracker: TrackerSummaryResponse,
        existing: ExpenseTrackerEntity?,
        userId: String
    ): ExpenseTrackerEntity {
        return when {
            existing == null -> networkTracker.toEntity(userId = userId, isSynced = true)
            !existing.isSynced -> existing.copy(serverId = networkTracker.id, userId = userId)
            else -> existing.copy(
                serverId = networkTracker.id,
                userId = userId,
                name = networkTracker.name,
                budget = networkTracker.budget,
                description = networkTracker.description.orEmpty(),
                startDate = networkTracker.startDate,
                endDate = networkTracker.endDate,
                isSynced = true,
                isDeleted = false
            )
        }
    }

    private suspend fun requireCurrentUserId(): String {
        return currentUserIdOrNull()
            ?: throw ApiException(401, "No active local user session")
    }

    private suspend fun currentUserIdOrNull(): String? {
        return tokenManager.getCurrentUserId().firstOrNull()
    }
}

private fun TrackerSummaryResponse.toEntity(userId: String, isSynced: Boolean): ExpenseTrackerEntity {
    return ExpenseTrackerEntity(
        id = id,
        serverId = id,
        userId = userId,
        name = name,
        budget = budget,
        description = description.orEmpty(),
        startDate = startDate,
        endDate = endDate,
        isSynced = isSynced,
        isDeleted = false
    )
}

private fun ExpenseTrackerEntity.toSummary(): TrackerSummaryResponse {
    return TrackerSummaryResponse(
        id = id,
        name = name,
        description = description.ifBlank { null },
        startDate = startDate,
        endDate = endDate,
        budget = budget
    )
}

private fun ExpenseTrackerResponse.toSummary(): TrackerSummaryResponse {
    return TrackerSummaryResponse(
        id = id,
        name = name,
        description = description,
        startDate = startDate,
        endDate = endDate,
        budget = budget
    )
}

private fun ExpenseResponse.toEntity(localTrackerId: String): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        description = description,
        amount = amount,
        date = date,
        category = normalizeExpenseCategory(category),
        trackerId = localTrackerId,
        occurredAt = occurredAt?.takeIf { it.isNotBlank() } ?: createdAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = true,
        isDeleted = false
    )
}

private fun ExpenseEntity.signature(): String {
    return "$trackerId|$date|$amount|$description|${normalizeExpenseCategory(category)}"
}
