package com.example.fix_finder.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.fix_finder.data.model.User
import com.example.fix_finder.data.model.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    @Volatile
    private var cachedUser: User? = null

    @Volatile
    private var cachedSettings: UserSettings? = null

    companion object {
        private const val PREF_NAME = "fix_finder_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_USER_SETTINGS = "user_settings"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    open fun saveAuthSession(user: User, token: String) {
        cachedUser = user
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.putString(KEY_USER_DATA, gson.toJson(user))
        editor.apply()
    }

    open fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    open fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    open fun getUser(): User? {
        if (cachedUser != null) return cachedUser
        val userJson = prefs.getString(KEY_USER_DATA, null) ?: return null
        return try {
            val user = gson.fromJson(userJson, User::class.java)
            cachedUser = user
            user
        } catch (_: Throwable) {
            null
        }
    }

    open fun updateUser(user: User) {
        cachedUser = user
        prefs.edit().putString(KEY_USER_DATA, gson.toJson(user)).apply()
    }

    open fun getSettings(): UserSettings {
        if (cachedSettings != null) return cachedSettings!!
        val json = prefs.getString(KEY_USER_SETTINGS, null)
        val currentUser = getUser()
        val defaultUserId = currentUser?.id ?: "guest"
        if (json != null) {
            val settings = try {
                gson.fromJson(json, UserSettings::class.java)
            } catch (_: Throwable) {
                UserSettings(userId = defaultUserId)
            }
            cachedSettings = settings
            return settings
        }
        val settings = UserSettings(userId = defaultUserId)
        cachedSettings = settings
        return settings
    }

    open fun saveSettings(settings: UserSettings) {
        cachedSettings = settings
        prefs.edit().putString(KEY_USER_SETTINGS, gson.toJson(settings)).apply()
    }

    open fun logout(context: Context? = null) {
        cachedUser = null
        cachedSettings = null
        val editor = prefs.edit()
        editor.remove(KEY_IS_LOGGED_IN)
        editor.remove(KEY_AUTH_TOKEN)
        editor.remove(KEY_USER_DATA)
        editor.apply()

        // Firebase Auth Sign Out
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Throwable) {
            // Safe fallback if Firebase is not initialized
        }

        // Credential Manager Clear Credential State
        context?.let { safeContext ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val credentialManager = CredentialManager.create(safeContext)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Throwable) {
                    // Safe fallback
                }
            }
        }
    }
}
