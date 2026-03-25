package com.example.expensemanager.data

import com.example.expensemanager.api.AuthApiService
import com.example.expensemanager.local.TokenManager
import com.example.expensemanager.models.LoginResponse
import com.example.expensemanager.models.RegisterRequest
import com.example.expensemanager.models.RegisterResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApiService.login(username, password)
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                tokenManager.saveToken(loginResponse.accessToken)
                Result.success(loginResponse)
            } else {
                Result.failure(
                    ApiException(
                        code = response.code(),
                        message = response.errorMessage("Login failed")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFacingMessage("Login failed")))
        }
    }

    suspend fun register(registerRequest: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = authApiService.register(registerRequest)
            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                Result.success(registerResponse)
            } else {
                Result.failure(
                    ApiException(
                        code = response.code(),
                        message = response.errorMessage("Registration failed")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFacingMessage("Registration failed")))
        }
    }

    suspend fun logout() {
        tokenManager.clearToken()
    }

    fun getAuthTokenFlow(): Flow<String?> {
        return tokenManager.getToken()
    }
}
