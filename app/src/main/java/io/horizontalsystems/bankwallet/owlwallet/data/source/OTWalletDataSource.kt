package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.*
import kotlinx.coroutines.flow.Flow

interface OTWalletDataSource {

    fun loginStateFlow(): Flow<Boolean>

    fun verifyStateFlow(): Flow<VerifyState>

    suspend fun login(email: String, password: String): OTResult<LoginResponse>

    suspend fun loginByToken(uuid: String, token: String): OTResult<LoginResponse>

    suspend fun register(
        email: String,
        password: String,
        name: String,
        gender: String?,
        birthday: String?,
    ): OTResult<RegisterResponse>

    suspend fun logout(): OTResult<LogoutResponse>

    suspend fun resetPassword(email: String): OTResult<ResetPasswordResponse>

    suspend fun deleteAccount(): OTResult<DeleteAccountResponse>

    suspend fun getCountries(
        lang: String = "en",
        filterType: String = "eu",
        nameFormat: String = "iso_code",
    ): OTResult<CountriesResponse>

    suspend fun getUserMeta(
        lang: String = "en",
    ): OTResult<UserMetaResponse>

    suspend fun amlMetaRegister(
        request: AmlMetaRegisterRequest
    ): OTResult<UserMetaResponse>

    suspend fun amlChainRegister(
        request: AmlChainRegisterRequest
    ): OTResult<UserMetaResponse>
}