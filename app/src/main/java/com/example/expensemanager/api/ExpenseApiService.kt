package com.example.expensemanager.api

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

    @GET("trackers/{id}")
    suspend fun getTrackerDetails(@Path("id") trackerId: Int): ExpenseTrackerResponse

    // --- Expense Endpoints ---

    @GET("trackers/{trackerId}/expenses")
    suspend fun getExpensesForTracker(
        @Path("trackerId") trackerId: Int
    ): List<ExpenseResponse>

    // Stats Endpoint
    @GET("trackers/{trackerId}/stats")
    suspend fun getStatsForTracker(
        @Path("trackerId") trackerId: Int
    ): StatsResponse


    @POST("trackers")
    suspend fun createTracker(@Body trackerRequest: ExpenseTrackerRequest): Response<ExpenseTrackerResponse>

    @POST("expenses")
    suspend fun addExpense(@Body expenseRequest: ExpenseRequest): Response<ExpenseResponse> // Or some other success response

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") expenseId: Int): Response<Unit>

    @PATCH("trackers/{id}")
    suspend fun updateTracker(
        @Path("id") trackerId: Int,
        @Body expenseTrackerRequest: ExpenseTrackerRequest
    ): Response<ExpenseTrackerResponse>


}