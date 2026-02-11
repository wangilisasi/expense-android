package com.example.expensemanager.api

import com.example.expensemanager.models.DailyExpensesResponse
import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseTrackerRequest
import com.example.expensemanager.models.ExpenseTrackerResponse

import retrofit2.Response
import retrofit2.http.*
import com.example.expensemanager.models.StatsResponse

interface ExpenseApiService {
    // --- ExpenseTracker Endpoints ---

    @GET("trackers")
    suspend fun getTrackers(): List<ExpenseTrackerResponse>

    @GET("trackers/{tracker_uuid_id}")
    suspend fun getTrackerDetails(@Path("tracker_uuid_id") trackerId: String): ExpenseTrackerResponse

    // --- Expense Endpoints ---

    @GET("trackers/{tracker_uuid_id}/expenses")
    suspend fun getExpensesForTracker(
        @Path("tracker_uuid_id") trackerId: String
    ): List<ExpenseResponse>

    // Stats Endpoint
    @GET("trackers/{tracker_uuid_id}/stats")
    suspend fun getStatsForTracker(
        @Path("tracker_uuid_id") trackerId: String
    ): StatsResponse


    @POST("trackers")
    suspend fun createTracker(@Body trackerRequest: ExpenseTrackerRequest): Response<ExpenseTrackerResponse>

    @POST("expenses")
    suspend fun addExpense(@Body expenseRequest: ExpenseRequest): Response<ExpenseResponse> // Or some other success response

    @POST("trackers/{tracker_uuid_id}/expenses")
    suspend fun addExpenseForTracker(
        @Path("tracker_uuid_id") trackerId: String,
        @Body expenseRequest: ExpenseRequest
    ): Response<ExpenseResponse>

    @DELETE("expenses/{uuid_id}")
    suspend fun deleteExpense(@Path("uuid_id") expenseId: String): Response<Unit>

    @PATCH("trackers/{uuid_id}")
    suspend fun updateTracker(
        @Path("uuid_id") trackerId: String,
        @Body expenseTrackerRequest: ExpenseTrackerRequest
    ): Response<ExpenseTrackerResponse>

    @PUT("trackers/{uuid_id}")
    suspend fun updateTrackerPut(
        @Path("uuid_id") trackerId: String,
        @Body expenseTrackerRequest: ExpenseTrackerRequest
    ): Response<ExpenseTrackerResponse>

    @PATCH("trackers/{uuid_id}")
    suspend fun updateTrackerMap(
        @Path("uuid_id") trackerId: String,
        @Body expenseTrackerRequest: Map<String, @JvmSuppressWildcards Any>
    ): Response<ExpenseTrackerResponse>

    @PUT("trackers/{uuid_id}")
    suspend fun updateTrackerPutMap(
        @Path("uuid_id") trackerId: String,
        @Body expenseTrackerRequest: Map<String, @JvmSuppressWildcards Any>
    ): Response<ExpenseTrackerResponse>

    @GET("trackers/{tracker_uuid_id}/daily-expenses")
    suspend fun getDailyExpensesForTracker(
        @Path("tracker_uuid_id") trackerId: String
    ): DailyExpensesResponse


}
