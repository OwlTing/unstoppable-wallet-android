package io.horizontalsystems.bankwallet.modules.send.stellar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.adapters.StellarAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendAmountAdvancedService
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronConfirmationData
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.transaction.Fee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.stellar.sdk.Transaction
import timber.log.Timber
import java.math.BigDecimal
import java.net.UnknownHostException

class SendStellarViewModel(
    val wallet: Wallet,
    private val sendToken: Token,
    private val feeToken: Token,
    private val adapter: StellarAdapter,
    private val xRateService: XRateService,
    private val amountService: SendAmountAdvancedService,
    private val addressService: SendStellarAddressService,
    val coinMaxAllowedDecimals: Int,
) : ViewModel() {

    val memoMaxLength = 120

    val blockchainType = wallet.token.blockchainType
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState: FeeState = FeeState.Loading
    private var cautions: List<HSCaution> = listOf()

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            proceedEnabled = amountState.canBeSend && addressState.canBeSend,
            sendEnabled = feeState is FeeState.Success,
            feeViewState = feeState.viewState,
            cautions = listOf()
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var confirmationData by mutableStateOf<SendTronConfirmationData?>(null)
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    private val decimalAmount: BigDecimal
        get() = amountState.evmAmount!!.toBigDecimal().movePointLeft(sendToken.decimals)

    var memo = ""
        private set

    init {
        viewModelScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }
        viewModelScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(sendToken.coin.uid).collect {
                coinRate = it
            }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(feeToken.coin.uid).collect {
                feeCoinRate = it
            }
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        viewModelScope.launch {
            addressService.setAddress(address)
        }
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo
    }

    fun onNavigateToConfirmation() {
        val address = addressState.address!!
//        val contact = contactsRepo.getContactsFiltered(
//            blockchainType = blockchainType,
//            addressQuery = address.hex
//        ).firstOrNull()

        confirmationData = SendTronConfirmationData(
            amount = decimalAmount,
            fee = null,
            activationFee = null,
            resourcesConsumed = null,
            address = address,
//            contact = contact,
            contact = null,
            coin = wallet.coin,
            feeCoin = feeToken.coin,
            isInactiveAddress = addressState.isInactiveAddress,
            memo = memo,
        )

        viewModelScope.launch {
            estimateFee()
            validateBalance()
        }
    }

    private fun validateBalance() {
//        val trxAmount = if (sendToken == feeToken) decimalAmount else BigDecimal.ZERO
        val feeState = feeState as? FeeState.Success ?: return
//        val totalFee = feeState.fees.sumOf { it.feeInSuns }.toBigDecimal().movePointLeft(feeToken.decimals)
        val totalFee = 1_000_000.toBigDecimal().movePointLeft(feeToken.decimals)
        val availableBalance = adapter.balanceData.available
        val availableBaseTokenBalance = adapter.baseTokenBalanceData.available

        if (sendToken.type == TokenType.Native) {
            Timber.d("decimalAmount: $decimalAmount, totalFee: $totalFee, availableBalance: $availableBalance, availableBase: $availableBaseTokenBalance")
            cautions = if (decimalAmount + totalFee > availableBaseTokenBalance) {
                listOf(
                    HSCaution(
                        TranslatableString.PlainString(
                            Translator.getString(
                                R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                                feeToken.coin.code
                            )
                        )
                    )
                )
            } else {
                listOf()
            }
        } else {
            Timber.d("decimalAmount: $decimalAmount, totalFee: $totalFee, availableBalance: $availableBalance, availableBase: $availableBaseTokenBalance")
            cautions = if (totalFee > availableBaseTokenBalance) {
                listOf(
                    HSCaution(
                        TranslatableString.PlainString(
                            Translator.getString(
                                R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                                feeToken.coin.code
                            )
                        )
                    )
                )
            } else {
                listOf()
            }
        }

        cautions = if (decimalAmount + totalFee > availableBaseTokenBalance) {
            listOf(
                HSCaution(
                    TranslatableString.PlainString(
                        Translator.getString(
                            R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                            feeToken.coin.code
                        )
                    )
                )
            )
        } else {
            listOf()
        }
        emitState()
    }

    private fun estimateFee() {
        try {
            feeState = FeeState.Loading
            emitState()

//            val amount = amountState.evmAmount!!
//            val tronAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(addressState.address!!.hex)
//            val fees = adapter.estimateFee(amount, tronAddress)
//
//            var activationFee: BigDecimal? = null
//            var bandwidth: String? = null
//            var energy: String? = null
//
//            fees.forEach { fee ->
//                when (fee) {
//                    is Fee.AccountActivation -> {
//                        activationFee = fee.feeInSuns.toBigDecimal().movePointLeft(feeToken.decimals)
//                    }
//
//                    is Fee.Bandwidth -> {
//                        bandwidth = "${fee.points} Bandwidth"
//                    }
//
//                    is Fee.Energy -> {
//                        val formattedEnergy = App.numberFormatter.formatNumberShort(fee.required.toBigDecimal(), 0)
//                        energy = "$formattedEnergy Energy"
//                    }
//                }
//            }
//
//            val resourcesConsumed = if (bandwidth != null) {
//                bandwidth + (energy?.let { " \n + $it" } ?: "")
//            } else {
//                energy
//            }

            feeState = FeeState.Success(listOf())
            emitState()

//            val totalFee = fees.sumOf { it.feeInSuns }.toBigDecimal().movePointLeft(feeToken.decimals)
            confirmationData = confirmationData?.copy(
                fee = 1_000_000.toBigDecimal().movePointLeft(feeToken.decimals),
//                activationFee = activationFee,
//                resourcesConsumed = resourcesConsumed
            )
        } catch (error: Throwable) {

            cautions = listOf(createCaution(error))
            feeState = FeeState.Error(error)
            emitState()
            confirmationData =
                confirmationData?.copy(fee = null, activationFee = null, resourcesConsumed = null)
        }
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        try {
            sendResult = SendResult.Sending
//            logger.info("sending tx")

            Timber.d("evmAmount: ${amountState.evmAmount!!}")
            adapter.send(sendToken, amountState.evmAmount!!, addressState.address!!.hex, memo)

            sendResult = SendResult.Sent
//            logger.info("success")
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
//            logger.warning("failed", e)
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    private fun handleUpdatedAmountState(amountState: SendAmountAdvancedService.State) {
        this.amountState = amountState
        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendStellarAddressService.State) {
        this.addressState = addressState
        emitState()
    }


    private fun emitState() {
        uiState = SendUiState(
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            proceedEnabled = amountState.canBeSend && addressState.canBeSend && ! addressState.isInactiveAddress,
            sendEnabled = cautions.isEmpty()&& ! addressState.isInactiveAddress  ,
            feeViewState = feeState.viewState,
            cautions = cautions
        )
    }
}

sealed class FeeState {
    object Loading : FeeState()
    data class Success(val fees: List<Fee>) : FeeState()
    data class Error(val error: Throwable) : FeeState()

    val viewState: ViewState
        get() = when (this) {
            is Error -> ViewState.Error(error)
            Loading -> ViewState.Loading
            is Success -> ViewState.Success
        }

    val feeLimit: Long?
        get() = when (this) {
            is Error -> null
            Loading -> null
            is Success -> {
                (fees.find { it is Fee.Energy } as? Fee.Energy)?.feeInSuns
            }
        }
}
