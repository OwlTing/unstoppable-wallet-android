package io.horizontalsystems.bankwallet.owlwallet.login

import android.text.TextUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.settings.main.SnackBarState
import io.horizontalsystems.bankwallet.owlwallet.data.AccountDeletedException
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.getLangParam
import kotlinx.coroutines.launch
import timber.log.Timber

data class QRCodeData(
    val token: String,
    @SerializedName("owlting_uuid")
    val uuid: String,
)

data class LoginUiState(
    val emailState: DataState<String>?,
    val passwordState: DataState<String>?,
    val canLogin: Boolean,
)

sealed class ActionState {
    object Loading : ActionState()
    class LoginSuccess(val isBindingSent: Boolean) : ActionState()
    object Failed : ActionState()
    object AccountDeleted : ActionState()
}

class LoginViewModel(
    private val repo: OTRepository,
    private val languageManager: LanguageManager,
) : ViewModel() {

    private var _emailState: DataState<String>? = null
    private var _passwordState: DataState<String>? = null
    private var _canLogin: Boolean = false

    var uiState by mutableStateOf(
        LoginUiState(
            emailState = _emailState,
            passwordState = _passwordState,
            canLogin = _canLogin,
        )
    )
        private set

    var actionState by mutableStateOf<ActionState?>(null)
        private set

    fun onEmailChanged(email: String) {
        _emailState = if (TextUtils.isEmpty(email)) {
            DataState.Error(Throwable())
        } else {
            val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            if (isValid) DataState.Success(email)
            else DataState.Error(Throwable())
        }

        emitState()
    }

    fun onPasswordChanged(password: String) {
        _passwordState = if (TextUtils.isEmpty(password)) {
            DataState.Error(Throwable())
        } else {
            DataState.Success(password)
        }

        emitState()
    }

    fun doLogin() {
        if (_emailState is DataState.Success && _passwordState is DataState.Success) {
            viewModelScope.launch {
                val email = (_emailState as DataState.Success).data
                val password = (_passwordState as DataState.Success).data
                actionState = ActionState.Loading

                val result = repo.login(email, password)

                if (result.succeeded) {
                    val metaResult = repo.getUserMeta(getLangParam(languageManager.currentLanguage))
                    Timber.d("meta result: $metaResult")
                    if (metaResult.succeeded) {
                        val meta = metaResult as OTResult.Success
                        Timber.d("meta result: ${metaResult.data}")
                        if (meta.data.status) {
                            actionState = ActionState.LoginSuccess(true)
                            return@launch
                        } else if (meta.data.code == 10032) {
                            actionState = ActionState.LoginSuccess(false)
                            return@launch
                        }
                    }
                } else {
                    if ((result as OTResult.Error).exception is AccountDeletedException) {
                        actionState = ActionState.AccountDeleted
                        return@launch
                    }
                }
                actionState = ActionState.Failed
            }
        }
    }

    fun doLoginByToken(scannedText: String) {
        Timber.d("scannedText: $scannedText")
        viewModelScope.launch {
            actionState = ActionState.Loading
            try {
                val qrCodeData = Gson().fromJson(scannedText, QRCodeData::class.java)
                val result = repo.loginByToken(qrCodeData.uuid, qrCodeData.token)
                if (result.succeeded) {
                    val metaResult = repo.getUserMeta(getLangParam(languageManager.currentLanguage))
                    Timber.d("meta result: $metaResult")
                    if (metaResult.succeeded) {
                        val meta = metaResult as OTResult.Success
                        Timber.d("meta result: ${metaResult.data}")
                        if (meta.data.status) {
                            actionState = ActionState.LoginSuccess(true)
                            return@launch
                        } else if (meta.data.code == 10032) {
                            actionState = ActionState.LoginSuccess(false)
                            return@launch
                        }
                    }
                } else {
                    if ((result as OTResult.Error).exception is AccountDeletedException) {
                        actionState = ActionState.AccountDeleted
                        return@launch
                    }
                }
                actionState = ActionState.Failed
            } catch (e: Exception) {
                actionState = ActionState.Failed
            }
        }
    }

    private fun emitState() {
        _canLogin = _emailState is DataState.Success && _passwordState is DataState.Success
        uiState = LoginUiState(
            emailState = _emailState,
            passwordState = _passwordState,
            canLogin = _canLogin,
        )
    }

    fun resetActionState() {
        actionState = null
    }
}