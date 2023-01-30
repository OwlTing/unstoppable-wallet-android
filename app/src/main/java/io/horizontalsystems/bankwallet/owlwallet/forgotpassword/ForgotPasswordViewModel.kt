package io.horizontalsystems.bankwallet.owlwallet.forgotpassword

import android.text.TextUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val emailState: DataState<String>?,
    val canResetPassword: Boolean,
)

sealed class ActionState {
    object Loading : ActionState()
    object ResetPasswordSuccess : ActionState()
    object Failed : ActionState()
}

class ForgotPasswordViewModel(
    private val repo: OTRepository
) : ViewModel() {

    private var _emailState: DataState<String>? = null
    private var _canResetPassword: Boolean = false

    var uiState by mutableStateOf(
        ForgotPasswordUiState(
            emailState = _emailState,
            canResetPassword = _canResetPassword,
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

    fun resetPassword() {
        if (_emailState is DataState.Success) {
            viewModelScope.launch {
                val email = (_emailState as DataState.Success).data
                actionState = ActionState.Loading

                val result = repo.resetPassword(email)
                actionState = if (result.succeeded) {
                    ActionState.ResetPasswordSuccess
                } else {
                    ActionState.Failed
                }
            }
        }
    }

    private fun emitState() {
        _canResetPassword = _emailState is DataState.Success
        uiState = ForgotPasswordUiState(
            emailState = _emailState,
            canResetPassword = _canResetPassword,
        )
    }

    fun resetActionState() {
        actionState = null
    }
}