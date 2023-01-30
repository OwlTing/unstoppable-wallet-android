package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.*
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class DefaultOTRepository(
    private val otWalletRemote: OTWalletDataSource,
) : OTRepository {

    override fun loginStateFlow(): Flow<Boolean> {
        return otWalletRemote.loginStateFlow()
    }

    override fun verifyStateFlow(): Flow<VerifyState> {
        return otWalletRemote.verifyStateFlow()
    }

    override suspend fun login(email: String, password: String): OTResult<LoginResponse> {
        return otWalletRemote.login(
            email,
            password
        )
    }

    override suspend fun loginByToken(uuid: String, token: String): OTResult<LoginResponse> {
        return otWalletRemote.loginByToken(uuid, token)
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        gender: String?,
        birthday: String?
    ): OTResult<Boolean> {
        val registerResult = otWalletRemote.register(
            email,
            password,
            name,
            gender,
            birthday,
        )

        if (!registerResult.succeeded) {
            return registerResult as OTResult.Error
        }

        val registerResp = (registerResult as OTResult.Success).data
        Timber.d("registerResp: $registerResp")
        return OTResult.Success(true)
    }

    override suspend fun resetPassword(email: String): OTResult<ResetPasswordResponse> {
        return otWalletRemote.resetPassword(email);
    }

    override suspend fun deleteAccount(): OTResult<DeleteAccountResponse> {
        return otWalletRemote.deleteAccount()
    }

    override suspend fun logout(): OTResult<Boolean> {
        val logoutResult = otWalletRemote.logout()
        if (logoutResult is OTResult.Error) {
            Timber.e("logout Failed")
        }
        return OTResult.Success(true)
    }

    override suspend fun getCountries(
        lang: String,
        filterType: String,
        nameFormat: String
    ): OTResult<CountriesResponse> {
        return otWalletRemote.getCountries(lang, filterType, nameFormat)
    }

    override suspend fun getUserMeta(lang: String): OTResult<UserMetaResponse> {
        return otWalletRemote.getUserMeta(lang)
    }

    override suspend fun amlMetaRegister(request: AmlMetaRegisterRequest): OTResult<UserMetaResponse> {
        return otWalletRemote.amlMetaRegister(request)
    }

    override suspend fun amlChainRegister(request: AmlChainRegisterRequest): OTResult<UserMetaResponse> {
        return otWalletRemote.amlChainRegister(request)
    }
}