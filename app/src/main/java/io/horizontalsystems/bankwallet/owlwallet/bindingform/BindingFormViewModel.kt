package io.horizontalsystems.bankwallet.owlwallet.bindingform

import android.text.TextUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.RefreshTokenExpiredException
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.*
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

data class KycState(
    val nameState: DataState<String>?,
    val nationalityState: DataState<Country>?,
    val birthdayState: DataState<String>?,
    val showDatePicker: Boolean,
    val kycDataReady: Boolean,
)

sealed class ActionState {
    object Loading : ActionState()
    object SendSuccess : ActionState()
    object Failed : ActionState()
    object Expired : ActionState()
}

class BindingFormViewModel(
    private val accountManager: IAccountManager,
    walletManager: IWalletManager,
    private val prefs: PreferenceHelper,
    private val repo: OTRepository
) : ViewModel() {

    var actionState by mutableStateOf<ActionState?>(null)
        private set

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var _nameState: DataState<String>? = null
    private var _nationalityState: DataState<Country>? = null
    private var _birthdayState: DataState<String>? = null
    private var _showDatePicker: Boolean = false
    private var _kycDataReady: Boolean = false

    var kycState by mutableStateOf(
        KycState(
            nameState = _nameState,
            nationalityState = _nationalityState,
            birthdayState = _birthdayState,
            showDatePicker = _showDatePicker,
            kycDataReady = _kycDataReady,
        )
    )
        private set

    val walletOptions = mutableStateListOf<Wallet>()
    val walletSelections = mutableStateListOf<Boolean>()

    val isKycFinished = mutableStateOf(false)

    private var privacyPolicyState = mutableStateOf(false)

    private var isFormCompleted = mutableStateOf(false)

    init {
        val account = accountManager.activeAccount
        if (account != null) {
            val wallets = walletManager.getWallets(account)
            wallets.forEach {
                walletOptions.add(it)
                walletSelections.add(false)
            }
        }

        prefs.getUserMeta()?.let {
            Timber.d("userMeta: $it")
            if (it.integratedStatus == VerifyState.VERIFIED.stateName || it.integratedStatus == VerifyState.UNFINISHED.stateName) {
                _nameState = DataState.Success(it.ssoUserMeta.name)
                _nationalityState = DataState.Success(it.ssoUserMeta.country)
                _birthdayState = DataState.Success(it.ssoUserMeta.birthday)
                emitKycState()
                isKycFinished.value = true
            }
        }
    }

    fun getVerifyState(): VerifyState {
        return prefs.getVerifyState()
    }

    fun getUser(): User {
        return prefs.getUser() ?: User()
    }

    fun onNameChanged(name: String) {
        _nameState = if (TextUtils.isEmpty(name)) {
            DataState.Error(Throwable())
        } else {
            DataState.Success(name)
        }

        emitKycState()
    }

    fun onUpdateCountry() {
        val country = prefs.getAmlRegisterCountry()

        if (country != null) {
            _nationalityState = DataState.Success(country)
            emitKycState()
        }
    }

    fun clearCountry() {
        _nationalityState = DataState.Error(Throwable())
        prefs.setAmlRegisterCountry(null)
        emitKycState()
    }

    fun onDateSelected(selection: Long) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = selection
        _birthdayState = DataState.Success(dateFormatter.format(calendar.time))
        _showDatePicker = false
        emitKycState()
    }

    fun onToggleDatePicker(enable: Boolean) {
        _showDatePicker = enable
        emitKycState()
    }

    fun clearDate() {
        _birthdayState = DataState.Error(Throwable())
        emitKycState()
    }

    fun onToggleWalletOption(index: Int, enable: Boolean) {
        if (index < walletOptions.size) {
            walletSelections[index] = enable
            verify()
        }
    }

    fun getPrivacyPolicyState(): Boolean {
        return privacyPolicyState.value
    }

    fun onTogglePrivacyPolicy(enable: Boolean) {
        privacyPolicyState.value = enable
        verify()
    }

    private fun emitKycState() {
        _kycDataReady =
            _nameState is DataState.Success && _nationalityState is DataState.Success && _birthdayState is DataState.Success
        kycState = KycState(
            nameState = _nameState,
            nationalityState = _nationalityState,
            birthdayState = _birthdayState,
            showDatePicker = _showDatePicker,
            kycDataReady = _kycDataReady,
        )
        verify()
    }

    fun isFormCompleted(): Boolean {
        return isFormCompleted.value
    }

    private fun verify() {
        isFormCompleted.value =
            _kycDataReady && walletSelections.contains(true) && privacyPolicyState.value
    }

    fun send() {
        when (getVerifyState()) {
            VerifyState.VERIFIED, VerifyState.UNFINISHED -> {
                sendAmlChainRegister()
            }
            else -> sendAmlMetaRegister()
        }
    }

    private fun sendAmlMetaRegister() {
        if (isFormCompleted.value) {
            viewModelScope.launch {
                actionState = ActionState.Loading

                val country = _nationalityState?.dataOrNull?.isoCode ?: ""
                val birthday = _birthdayState?.dataOrNull ?: ""
                val name = _nameState?.dataOrNull ?: ""
                val chains = mutableListOf<AmlMetaRegisterChain>()

                walletOptions.forEachIndexed { index, wallet ->
                    if (walletSelections[index]) {
                        chains.add(
                            AmlMetaRegisterChain(
                                address = App.getReceiveAddress(wallet),
                                network = wallet.token.blockchain.name,
                                asset = wallet.coin.code.uppercase(),
                            )
                        )
                    }
                }

                val request = AmlMetaRegisterRequest(
                    country = country,
                    birthday = birthday,
                    name = name,
                    chains = chains,
                )

                Timber.d("request: $request")
                val result = repo.amlMetaRegister(request)
                if (result.succeeded) {
                    actionState = ActionState.SendSuccess
                    return@launch
                } else {
                    val exception = (result as OTResult.Error).exception
                    actionState = if (exception is RefreshTokenExpiredException) {
                        ActionState.Expired
                    } else {
                        ActionState.Failed
                    }
                }
            }
        }
    }

    private fun sendAmlChainRegister() {
        Timber.d("sendAmlChainRegister")
        if (isFormCompleted.value) {
            viewModelScope.launch {
                actionState = ActionState.Loading
                val userMetaResult = repo.getUserMeta()
                if (!userMetaResult.succeeded) {
                    if ((userMetaResult as OTResult.Error).exception is RefreshTokenExpiredException) {
                        actionState = ActionState.Expired
                        return@launch
                    }
                }

                val chains = mutableListOf<AmlChainRegisterChain>()

                walletOptions.forEachIndexed { index, wallet ->
                    val address = App.getReceiveAddress(wallet)
                    if (walletSelections[index]) {
                        chains.add(
                            AmlChainRegisterChain(
                                address,
                                wallet.token.blockchain.name,
                                wallet.coin.code,
                                1
                            )
                        )
                    } else {
                        val userChains = prefs.getUserMeta()?.ssoUserChains ?: listOf()
                        val userChain = userChains.firstOrNull {
                            it.chainAddress == address && it.chainNetwork == wallet.token.blockchain.name && it.chainAsset == wallet.coin.code
                        }

                        if (userChain != null) {
                            chains.add(
                                AmlChainRegisterChain(
                                    address,
                                    wallet.token.blockchain.name,
                                    wallet.coin.code,
                                    0
                                )
                            )
                        }
                    }
                }

                val result = repo.amlChainRegister(AmlChainRegisterRequest(chains))
                if (result.succeeded) {
                    actionState = ActionState.SendSuccess
                    return@launch
                } else {
                    val exception = (result as OTResult.Error).exception
                    actionState = if (exception is RefreshTokenExpiredException) {
                        ActionState.Expired
                    } else {
                        ActionState.Failed
                    }
                }
            }
        }
    }
}