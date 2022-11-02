package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.GetWalletsResponse
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.ResetPasswordResponse
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.SyncWalletsRequest
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.VersionData
import kotlinx.coroutines.flow.Flow

interface OTRepository {

    fun loginStateFlow(): Flow<Boolean>

    suspend fun doLogin(email: String, password: String): OTResult<Boolean>

    suspend fun doLogout(): OTResult<Boolean>

    suspend fun resetPassword(email: String): OTResult<ResetPasswordResponse>

    suspend fun getWallets(): OTResult<GetWalletsResponse>

    suspend fun syncWallets(request: SyncWalletsRequest): OTResult<Boolean>
}