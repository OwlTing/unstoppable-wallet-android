package io.horizontalsystems.bankwallet.owlwallet.login

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class LoginUiState(
    val emailState: DataState<String>? = null,
    val passwordState: DataState<String>? = null,
    val canLogin: Boolean = false,
    val currentState: SnackBarState? = null,
)

sealed class SnackBarState {
    object Loading : SnackBarState()
    class LoginSuccess(val msg: String) : SnackBarState()
    class ResetPasswordSuccess(val msg: String) : SnackBarState()
    class Failed(val msg: String) : SnackBarState()
}

class LoginViewModel(
    private val repo: OTRepository
) : ViewModel() {

    private val _emailState: MutableStateFlow<DataState<String>?> =
        MutableStateFlow(null)
    private val _passwordState: MutableStateFlow<DataState<String>?> =
        MutableStateFlow(null)
    private val _currentState: MutableStateFlow<SnackBarState?> = MutableStateFlow(null)

    val uiState: StateFlow<LoginUiState> = combine(
        _emailState, _passwordState, _currentState,
    ) { emailState, passwordState, currentState ->
        LoginUiState(
            emailState = emailState,
            passwordState = passwordState,
            canLogin = emailState is DataState.Success && passwordState is DataState.Success,
            currentState = currentState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoginUiState()
    )

    fun onEmailChanged(email: String) {
        _currentState.value = null
        if (TextUtils.isEmpty(email)) {
            _emailState.value = null
        } else {
            val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            _emailState.value =
                if (isValid) DataState.Success(email)
                else DataState.Error(Throwable())
        }
    }

    fun onPasswordChanged(password: String) {
        _currentState.value = null
        if (TextUtils.isEmpty(password)) {
            _passwordState.value = null
        } else {
            _passwordState.value = DataState.Success(password)
//            _passwordState.value =
//                if (password.length >= 6) DataState.Success(password)
//                else DataState.Error(Throwable("Invalid password format"))
        }
    }

    fun doLogin(email: String, password: String) {
        Timber.d("doLogin")
        viewModelScope.launch {
            _currentState.value = SnackBarState.Loading
            val result = repo.doLogin(email, password)
            if (result.succeeded) {
                _currentState.value = SnackBarState.LoginSuccess("Logged In")
            } else {
                _currentState.value =
                    SnackBarState.Failed((result as OTResult.Error).exception.message!!)
            }
        }
    }

    fun resetPassword(email: String) {
        Timber.d("resetPassword")
        viewModelScope.launch {
            _currentState.value = SnackBarState.Loading
            val result = repo.resetPassword(email)

            if (result.succeeded) {
                _currentState.value = SnackBarState.ResetPasswordSuccess((result as OTResult.Success).data.msg)
            } else {
                _currentState.value =
                    SnackBarState.Failed((result as OTResult.Error).exception.message!!)
            }
        }
    }
}