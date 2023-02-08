package io.horizontalsystems.bankwallet.owlwallet.register

import android.text.TextUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.getLangParam
import io.horizontalsystems.bankwallet.owlwallet.utils.passwordRegex
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

enum class GenderOption {
    MALE, FEMALE, RATHER_NOT_SAY
}

data class RegisterUiState(
    val displayNameState: DataState<String>?,
    val emailState: DataState<String>?,
    val passwordState: DataState<String>?,
    val password2State: DataState<String>?,
    val birthdayState: DataState<String>?,
    val showDatePicker: Boolean,
    val genderOption: GenderOption?,
)

sealed class ActionState {
    object Loading : ActionState()
    class RegisterSuccess(val isBindingSent: Boolean) : ActionState()
    object Failed : ActionState()
}

class RegisterViewModel(
    private val languageManager: LanguageManager,
    private val repo: OTRepository,
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var _displayNameState: DataState<String>? = null
    private var _emailState: DataState<String>? = null
    private var _password: String? = null
    private var _password2: String? = null
    private var _passwordState: DataState<String>? = null
    private var _password2State: DataState<String>? = null
    private var _birthdayState: DataState<String>? = null

    var genderOption: GenderOption? = null
        private set

    var showDatePicker: Boolean = false
        private set

    private val privacyPolicyState = mutableStateOf(false)
    val isFormCompleted = mutableStateOf(false)

    var uiState by mutableStateOf(
        RegisterUiState(
            displayNameState = _displayNameState,
            emailState = _emailState,
            passwordState = _passwordState,
            password2State = _password2State,
            birthdayState = _birthdayState,
            showDatePicker = showDatePicker,
            genderOption = null,
        )
    )
        private set

    var actionState by mutableStateOf<ActionState?>(null)
        private set

    fun onDisplayNameChanged(displayName: String) {
        _displayNameState = if (TextUtils.isEmpty(displayName)) {
            DataState.Error(Throwable())
        } else {
            DataState.Success(displayName)
        }

        emitState()
    }

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
        _password = password
        verifyPassword()
        verifyPassword2()
    }

    private fun verifyPassword() {
        _passwordState = if (TextUtils.isEmpty(_password)) {
            DataState.Error(Throwable())
        } else if (_password!!.matches(passwordRegex)) {
            DataState.Success(_password!!)
        } else {
            DataState.Error(Throwable())
        }
        emitState()
    }

    fun onPassword2Changed(password: String) {
        _password2 = password
        verifyPassword()
        verifyPassword2()
    }

    private fun verifyPassword2() {
        _password2State = if (TextUtils.isEmpty(_password2)) {
            DataState.Error(Throwable())
        } else if (_password2!!.matches(passwordRegex) && _password2 == _password) {
            DataState.Success(_password2!!)
        } else {
            DataState.Error(Throwable())
        }
        emitState()
    }

    fun onDateSelected(selection: Long) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = selection

        Timber.d("date: ${dateFormatter.format(calendar.time)}")
        _birthdayState = DataState.Success(dateFormatter.format(calendar.time))
        showDatePicker = false
        emitState()
    }

    fun clearDate() {
        _birthdayState = DataState.Error(Throwable())
        emitState()
    }

    fun onToggleDatePicker(enable: Boolean) {
        showDatePicker = enable
        emitState()
    }

    fun onGenderOptionChanged(option: GenderOption) {
        genderOption = option
        emitState()
    }

    fun getPrivacyPolicyState(): Boolean {
        return privacyPolicyState.value
    }

    fun onTogglePrivacyPolicy(enable: Boolean) {
        privacyPolicyState.value = enable
        verify()
    }

    fun doRegister() {
        if (_emailState is DataState.Success
            && _passwordState is DataState.Success
            && _displayNameState is DataState.Success
            && genderOption != null
            && _birthdayState is DataState.Success
        ) {
            viewModelScope.launch {
                val email = (_emailState as DataState.Success).data
                val password = (_passwordState as DataState.Success).data
                val name = (_displayNameState as DataState.Success).data
                val gender = when (genderOption) {
                    GenderOption.MALE -> "male"
                    GenderOption.FEMALE -> "female"
                    else -> "unknown"
                }
                val birthday = (_birthdayState as DataState.Success).data
                actionState = ActionState.Loading

                val result = repo.register(email, password, name, gender, birthday)
                if (result.succeeded) {
                    val metaResult = repo.getUserMeta(getLangParam(languageManager.currentLanguage))
                    if (metaResult.succeeded) {
                        val meta = metaResult as OTResult.Success
                        Timber.d("meta result: ${metaResult.data}")
                        if (meta.data.status) {
                            actionState = ActionState.RegisterSuccess(true)
                            return@launch
                        } else if (meta.data.code == 10032) {
                            actionState = ActionState.RegisterSuccess(false)
                            return@launch
                        }
                    }
                }
                actionState = ActionState.Failed
            }
        }
    }

    private fun emitState() {
        uiState = RegisterUiState(
            displayNameState = _displayNameState,
            emailState = _emailState,
            passwordState = _passwordState,
            password2State = _password2State,
            birthdayState = _birthdayState,
            showDatePicker = showDatePicker,
            genderOption = genderOption,
        )

        verify()
    }

    private fun verify() {
        isFormCompleted.value =
            _displayNameState is DataState.Success
                    && _emailState is DataState.Success
                    && _passwordState is DataState.Success
                    && _password2State is DataState.Success
                    && _birthdayState is DataState.Success
                    && genderOption != null && privacyPolicyState.value
    }

    fun resetActionState() {
        actionState = null
    }
}