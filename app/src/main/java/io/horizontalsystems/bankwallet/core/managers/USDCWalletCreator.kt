package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.supportedTokens
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService

class USDCWalletCreator {
    companion object {
        fun create(account: Account): List<Wallet> {
            val wallets = mutableListOf<Wallet>()

            val restoreSettingsService =
                RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider)
            val coinSettingsService = CoinSettingsService()
            val coinTokensService = CoinTokensService()
            val enableCoinService =
                EnableCoinService(coinTokensService, restoreSettingsService, coinSettingsService)
            val manageWalletsService = ManageWalletsService(
                App.marketKit,
                App.walletManager,
                App.accountManager,
                enableCoinService
            )

            manageWalletsService.getUSDCFullCoin()?.supportedTokens?.forEach { token ->
                wallets.add(Wallet(token, account))
            }
            return wallets
        }

        fun create(account: Account, enableCoinService: EnableCoinService): List<Wallet> {
            val wallets = mutableListOf<Wallet>()

//            val restoreSettingsService =
//                RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider)
//            val coinSettingsService = CoinSettingsService()
//            val coinTokensService = CoinTokensService()
//            val enableCoinService =
//                EnableCoinService(coinTokensService, restoreSettingsService, coinSettingsService)
            val manageWalletsService = ManageWalletsService(
                App.marketKit,
                App.walletManager,
                App.accountManager,
                enableCoinService
            )

            manageWalletsService.getUSDCFullCoin()?.supportedTokens?.forEach { token ->
                wallets.add(Wallet(token, account))
            }
            return wallets
        }
    }

}
