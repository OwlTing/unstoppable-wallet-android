package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.*
import kotlinx.coroutines.flow.Flow

interface OTWalletDataSource {

    fun loginStateFlow(): Flow<Boolean>

    suspend fun login(uuid: String, secret: String): OTResult<LoginResponse>

    suspend fun logout(): OTResult<LogoutResponse>

    suspend fun getWallets(): OTResult<GetWalletsResponse>

    suspend fun syncWallets(request: SyncWalletsRequest): OTResult<SyncWalletsResponse>
}