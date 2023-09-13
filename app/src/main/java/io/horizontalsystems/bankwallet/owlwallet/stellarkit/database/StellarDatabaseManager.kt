package io.horizontalsystems.bankwallet.owlwallet.stellarkit.database

import android.content.Context
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.Network


internal object StellarDatabaseManager {
    fun getMainDatabase(context: Context, network: Network, walletId: String): MainDatabase {
        return MainDatabase.getInstance(context, getDatabaseName(network, walletId))
    }

    fun clear(context: Context, network: Network, walletId: String) {
        synchronized(this) {
            context.deleteDatabase(getDatabaseName(network, walletId))
        }
    }

    private fun getDatabaseName(network: Network, walletId: String): String {
        return "Stellar-${network.name}-$walletId"
    }
}