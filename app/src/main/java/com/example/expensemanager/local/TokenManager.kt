package com.example.expensemanager.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auth0.android.jwt.JWT
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
        }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    fun getUsernameFromToken(token: String?): String? {
        if (token == null) return null
        return try {
            val jwt = JWT(token)
            // 'getSubject' is the helper method for the 'sub' claim.
            jwt.subject
        } catch (e: Exception) {
            // Token is malformed or invalid
            null
        }
    }

    fun isTokenExpired(token: String?, bufferInSeconds: Long = 60): Boolean {
        if (token == null) {
            return true
        }
        try {
            val jwt = JWT(token)
            // Check if the 'exp' claim is present
            val expiresAt = jwt.expiresAt
            if (expiresAt == null) {
                // If there's no expiration, treat it as invalid/expired
                // for security reasons.
                return true
            }
            // Check if the token is expired, applying a safety buffer
            // to account for clock skew and network latency.
            val bufferDate = Date(System.currentTimeMillis() - bufferInSeconds * 1000)
            return expiresAt.before(bufferDate)
        } catch (e: Exception) {
            // Any exception during decoding means the token is invalid.
            // Log the exception for debugging.
            // Log.e("AuthManager", "Failed to decode JWT", e)
            return true
        }
    }
}


