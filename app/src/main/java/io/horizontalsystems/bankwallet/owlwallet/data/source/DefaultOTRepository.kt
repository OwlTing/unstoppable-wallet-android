package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.GetWalletsResponse
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.ResetPasswordResponse
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.SyncWalletsRequest
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class DefaultOTRepository(
    private val otAuthRemote: OTAuthDataSource,
    private val otWalletRemote: OTWalletDataSource,
) : OTRepository {

    override fun loginStateFlow(): Flow<Boolean> {
        return otWalletRemote.loginStateFlow()
    }

    override suspend fun doLogin(email: String, password: String): OTResult<Boolean> {
        val signInByEmailResult = otAuthRemote.signInByEmail(
            email, password
        )

        if (!signInByEmailResult.succeeded) {
            return signInByEmailResult as OTResult.Error
        }

        val signInResp = (signInByEmailResult as OTResult.Success).data
        Timber.d("signInResp: $signInResp")

        val loginResult = otWalletRemote.login(
            signInResp.uuid,
            signInResp.secret
        )

        if (!loginResult.succeeded) {
            return loginResult as OTResult.Error
        }

        val loginResp = (loginResult as OTResult.Success).data
        Timber.d("loginResp: $loginResp")
        return OTResult.Success(true)
    }

    override suspend fun doLogout(): OTResult<Boolean> {
        val logoutResult = otWalletRemote.logout()
        if (logoutResult is OTResult.Error) {
            Timber.e("logout Failed")
        }
        return OTResult.Success(true)
    }

    override suspend fun resetPassword(email: String): OTResult<ResetPasswordResponse> {
        val resetResult = otAuthRemote.resetPasswordByEmail(email)
        if (!resetResult.succeeded) {
            return resetResult as OTResult.Error
        }
        return OTResult.Success((resetResult as OTResult.Success).data)
    }


    override suspend fun getWallets(): OTResult<GetWalletsResponse> {
        return otWalletRemote.getWallets()
    }

    override suspend fun syncWallets(request: SyncWalletsRequest): OTResult<Boolean> {
        val result = otWalletRemote.syncWallets(request)
        return if (result.succeeded) {
            OTResult.Success(true)
        } else {
            result as OTResult.Error
        }
    }
}