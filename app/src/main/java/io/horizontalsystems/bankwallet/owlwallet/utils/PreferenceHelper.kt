package io.horizontalsystems.bankwallet.owlwallet.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class PreferenceHelper(context: Context) {

    companion object {

        private const val PREFS_LOGIN_STATE = "prefs_login_state"
        private const val PREFS_ACCESS_TOKEN = "prefs_access_token"
        private const val PREFS_REFRESH_TOKEN = "prefs_refresh_token"
    }

    private val prefs =
        context.getSharedPreferences(context.packageName + "_owlting_preferences", MODE_PRIVATE)

    fun loginStateFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == PREFS_LOGIN_STATE) {
                val loginState = prefs.getBoolean(PREFS_LOGIN_STATE, false)
                Timber.d("on login state changed: $loginState")
                trySend(loginState)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun getLoginState(): Boolean {
        return prefs.getBoolean(PREFS_LOGIN_STATE, false)
    }

    fun getTokenPair(): Pair<String, String> {
        return Pair(
            prefs.getString(PREFS_ACCESS_TOKEN, "") ?: "",
            prefs.getString(PREFS_REFRESH_TOKEN, "") ?: ""
        )
    }

    fun setTokenPair(accessToken: String, refreshToken: String) {
        Timber.d("setTokenPair $accessToken $refreshToken")
        prefs
            .edit()
            .putString(PREFS_ACCESS_TOKEN, accessToken)
            .putString(PREFS_REFRESH_TOKEN, refreshToken)
            .putBoolean(PREFS_LOGIN_STATE, !(accessToken.isBlank() || refreshToken.isBlank()))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}