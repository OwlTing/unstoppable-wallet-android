package io.horizontalsystems.bankwallet.modules.send.stellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.StellarAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.send.SendAmountAdvancedService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.RoundingMode

object SendStellarModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet))
            ?: throw IllegalStateException("SendStellarAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendStellarViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountAdvancedService(
                        (adapter as StellarAdapter).balanceData.available.setScale(
                            coinMaxAllowedDecimals,
                            RoundingMode.DOWN
                        ),
                        wallet.token,
                        amountValidator,
                    )
                    val addressService = SendStellarAddressService(adapter, wallet.token)
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
                    val feeToken = App.coinManager.getToken(
                        TokenQuery(
                            BlockchainType.Stellar,
                            TokenType.Native
                        )
                    ) ?: throw IllegalArgumentException()
//
//                    SendTronViewModel(
//                        wallet,
//                        wallet.token,
//                        feeToken,
//                        adapter,
//                        xRateService,
//                        amountService,
//                        addressService,
//                        coinMaxAllowedDecimals,
//                        App.contactsRepository
                    SendStellarViewModel(
                        wallet,
                        wallet.token,
                        feeToken,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals,
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }
}