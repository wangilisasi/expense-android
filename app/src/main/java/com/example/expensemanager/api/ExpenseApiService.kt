package com.example.expensemanager.api

import com.example.expensemanager.models.ExpenseResponse
import com.example.expensemanager.models.ExpenseRequest
import com.example.expensemanager.models.ExpenseTrackerResponse

import retrofit2.Response
import retrofit2.http.*

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

    @POST("expenses")
    suspend fun addExpense(@Body expenseRequest: ExpenseRequest): Response<ExpenseResponse> // Or some other success response

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") expenseId: String): Response<Unit>
}