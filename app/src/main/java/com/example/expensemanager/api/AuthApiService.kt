package com.example.expensemanager.api

import com.example.expensemanager.models.LoginResponse
import com.example.expensemanager.models.RegisterRequest
import com.example.expensemanager.models.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST



interface AuthService {
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse> // Use Response for more control over the HTTP response


    @POST("register")
    suspend fun register(
       @Body registerRequest: RegisterRequest
    ): Response<RegisterResponse> // Adjust response type as needed
}