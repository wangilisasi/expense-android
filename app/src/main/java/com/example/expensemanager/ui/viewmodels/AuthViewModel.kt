package com.example.expensemanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.AuthRepository
import com.example.expensemanager.local.TokenManager
import com.example.expensemanager.models.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Unknown : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager // Inject TokenManager to check initial auth state
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginInProgress = MutableStateFlow(false)
    val loginInProgress: StateFlow<Boolean> = _loginInProgress.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _registrationInProgress = MutableStateFlow(false)
    val registrationInProgress: StateFlow<Boolean> = _registrationInProgress.asStateFlow()

    private val _errorEvents = MutableStateFlow<String?>(null)
    val errorEvents: StateFlow<String?> = _errorEvents.asStateFlow()

    private val _registrationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val registrationEvents = _registrationEvents.asSharedFlow()


    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            tokenManager.getToken().collectLatest { token ->
                if (token != null && !tokenManager.isTokenExpired(token)) {
                    _authState.value = AuthState.Authenticated
                    _username.value = tokenManager.getUsernameFromToken(token)
                } else if (token != null && tokenManager.isTokenExpired(token)) {
                    tokenManager.clearToken()
                    _authState.value = AuthState.Unauthenticated
                    _username.value = null
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _username.value = null
                }
            }
        }
    }


    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginInProgress.value = true
            _errorEvents.value = null
            val result = authRepository.login(username, password)
            result.onSuccess { loginResponse ->

                _authState.value = AuthState.Authenticated
                _username.value = tokenManager.getUsernameFromToken(loginResponse.accessToken) ?: username

            }.onFailure { exception ->
                _errorEvents.value = exception.message ?: "Login failed"
                _authState.value =
                    AuthState.Unauthenticated // Ensure state is unauthenticated on error
                _username.value = null
            }
            _loginInProgress.value = false
        }
    }

    fun register(registerRequest: RegisterRequest) {
        viewModelScope.launch {
            _registrationInProgress.value = true
            _errorEvents.value = null
            val result = authRepository.register(registerRequest)
            result.onSuccess { registerResponse ->
                _authState.value = AuthState.Unauthenticated
                _registrationEvents.tryEmit(Unit)
                _errorEvents.value = "Registration successful for ${registerResponse.username}. Please log in."
            }.onFailure { exception ->
                _errorEvents.value = exception.message ?: "Registration failed"
            }
            _registrationInProgress.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
            _username.value = null
        }
    }

    // Deal with error events
    fun clearError() {
        _errorEvents.value = null
    }

}
