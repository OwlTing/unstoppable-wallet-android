package io.horizontalsystems.bankwallet.modules.send.stellar

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.StellarAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendStellarAddressService(
    private val adapter: StellarAdapter,
    private val token: Token
) {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var isInactiveAddress: Boolean = false

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            addressError = addressError,
            isInactiveAddress = isInactiveAddress,
            canBeSend = false
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun setAddress(address: Address?) {
        this.address = address

        validateAddress()

        emitState()
    }

    private suspend fun validateAddress() {
        addressError = null
        val accountId = this.address?.hex ?: return

        try {
            if (!adapter.isAccountActive(accountId)) {
                isInactiveAddress = true
                addressError = FormsInputStateWarning(Translator.getString(R.string.Tron_AddressNotActive_Warning))
            } else {
                isInactiveAddress = false
            }

            if (token.type == TokenType.Native && adapter.isOwnAccount(accountId)) {
                addressError = Throwable(Translator.getString(R.string.Tron_SelfSendTrxNotAllowed))
            }

        } catch (e: Exception) {
            isInactiveAddress = false
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                addressError = addressError,
                isInactiveAddress = isInactiveAddress,
                canBeSend = (address != null && addressError == null) || addressError is FormsInputStateWarning
            )
        }
    }

    data class State(
        val address: Address?,
        val addressError: Throwable?,
        val isInactiveAddress: Boolean,
        val canBeSend: Boolean
    )
}