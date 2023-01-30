package io.horizontalsystems.bankwallet.owlwallet.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class PreferenceHelper(context: Context) {

    companion object {

        private const val PREFS_LOGIN_STATE = "prefs_login_state"
        private const val PREFS_VERIFY_STATE = "prefs_verify_state"
        private const val PREFS_ACCESS_TOKEN = "prefs_access_token"
        private const val PREFS_REFRESH_TOKEN = "prefs_refresh_token"
        private const val PREFS_USER = "prefs_user"
        private const val PREFS_USER_META = "prefs_user_meta"
        private const val PREFS_AML_REGISTER_COUNTRY = "prefs_AML_REGISTER_COUNTRY"
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
        send(prefs.getBoolean(PREFS_LOGIN_STATE, false))
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.buffer(Channel.UNLIMITED)

    fun verifyStateFlow(): Flow<VerifyState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == PREFS_VERIFY_STATE) {
                val verifyState = VerifyState.values()[prefs.getInt(PREFS_VERIFY_STATE, 0)]
                Timber.d("on verify state changed: $verifyState")
                trySend(verifyState)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send(VerifyState.values()[prefs.getInt(PREFS_VERIFY_STATE, 0)])
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.buffer(Channel.UNLIMITED)

    fun getLoginState(): Boolean {
        return prefs.getBoolean(PREFS_LOGIN_STATE, false)
    }

    fun getVerifyState(): VerifyState {
        return VerifyState.values()[prefs.getInt(PREFS_VERIFY_STATE, 0)]
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

    fun getUser(): User? {
        return try {
            Gson().fromJson(prefs.getString(PREFS_USER, "") ?: "", User::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun setUser(user: User?) {
        prefs
            .edit()
            .putString(PREFS_USER, if (user != null) Gson().toJson(user) else "")
            .apply()
    }

    fun getUserMeta(): UserMeta? {
        return try {
            Gson().fromJson(prefs.getString(PREFS_USER_META, "") ?: "", UserMeta::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun setUserMeta(userMeta: UserMeta?) {
        val verifyState = when (userMeta?.integratedStatus) {
            VerifyState.UNVERIFIED.stateName -> VerifyState.UNVERIFIED
            VerifyState.VERIFIED.stateName -> VerifyState.VERIFIED
            VerifyState.REJECTED.stateName -> VerifyState.REJECTED
            VerifyState.UNFINISHED.stateName -> VerifyState.UNFINISHED
            else -> VerifyState.NOT_FOUND
        }

        prefs
            .edit()
            .putString(PREFS_USER_META, if (userMeta != null) Gson().toJson(userMeta) else "")
            .putInt(PREFS_VERIFY_STATE, verifyState.ordinal)
            .apply()
    }

    fun login(accessToken: String, refreshToken: String, user: User) {
        prefs
            .edit()
            .putString(PREFS_ACCESS_TOKEN, accessToken)
            .putString(PREFS_REFRESH_TOKEN, refreshToken)
            .putString(PREFS_USER, Gson().toJson(user))
            .putBoolean(PREFS_LOGIN_STATE, !(accessToken.isBlank() || refreshToken.isBlank()))
            .apply()
    }

    fun logout() {
        prefs
            .edit()
            .putString(PREFS_ACCESS_TOKEN, "")
            .putString(PREFS_REFRESH_TOKEN, "")
            .putString(PREFS_USER, "")
            .putString(PREFS_USER_META, "")
            .putBoolean(PREFS_LOGIN_STATE, false)
            .putInt(PREFS_VERIFY_STATE, 0)
            .apply()
    }

    fun setAmlRegisterCountry(country: Country?) {
        prefs
            .edit()
            .putString(PREFS_AML_REGISTER_COUNTRY, if (country != null) Gson().toJson(country) else "")
            .apply()
    }

    fun getAmlRegisterCountry(): Country? {
        return try {
            Gson().fromJson(
                prefs.getString(PREFS_AML_REGISTER_COUNTRY, "") ?: "",
                Country::class.java
            )
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}