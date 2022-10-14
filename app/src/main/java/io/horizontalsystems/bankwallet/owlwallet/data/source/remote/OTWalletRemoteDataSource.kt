package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.RefreshTokenExpiredException
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTWalletDataSource
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

class OTWalletRemoteDataSource(
    private var apiClient: OTWalletApiClient,
    private val preferenceHelper: PreferenceHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OTWalletDataSource {

    override fun loginStateFlow(): Flow<Boolean> {
        return preferenceHelper.loginStateFlow()
    }

    override suspend fun login(
        uuid: String,
        secret: String
    ): OTResult<LoginResponse> = withContext(ioDispatcher) {
        try {
            val response = apiClient.login(LoginRequest(uuid, secret))
            if (response.status) {
                preferenceHelper.setTokenPair(
                    accessToken = response.token.access,
                    refreshToken = response.token.refresh,
                )
                resetApiClient()
                OTResult.Success(response)
            } else {
                OTResult.Error(Exception(response.msg))
            }
        } catch (e: Exception) {
            OTResult.Error(e)
        }
    }

    override suspend fun logout(): OTResult<LogoutResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.logout()
                if (response.status) {
                    preferenceHelper.setTokenPair("", "")
                    resetApiClient()
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception("Logout failed"))
                }
            } catch (e: Exception) {
                handleException(e) {
                    logout()
                }
            }
        }

    override suspend fun getWallets(
    ): OTResult<GetWalletsResponse> = withContext(ioDispatcher) {
        Timber.d("getWallets")
        try {
            val response = apiClient.getWallets()
            Timber.d("getWallets $response")
            if (response.status) {
                OTResult.Success(response)
            } else {
                OTResult.Error(Exception("Get wallets failed"))
            }
        } catch (e: Exception) {
            handleException(e) {
                getWallets()
            }
        }
    }

    override suspend fun syncWallets(
        request: SyncWalletsRequest
    ): OTResult<SyncWalletsResponse> = withContext(ioDispatcher) {
        try {
            val response = apiClient.syncWallets(request)
            Timber.d("syncWallets $response")
            if (response.status) {
                OTResult.Success(response)
            } else {
                OTResult.Error(Exception("Get wallets failed"))
            }
        } catch (e: Exception) {
            handleException(e) {
                syncWallets(request)
            }
        }
    }

    private suspend fun <T : Any> handleException(
        e: Exception,
        retryFunction: suspend () -> OTResult<T>
    ): OTResult<T> {
        Timber.e("handleException: $e")
        return when (e) {
            is HttpException -> {
                if (e.code() == 401) {
                    resetApiClient(true)

                    try {
                        val response = apiClient.refreshToken(RefreshTokenRequest())
                        if (response.status) {
                            preferenceHelper.setTokenPair(
                                response.token.access,
                                response.token.refresh
                            )
                            resetApiClient()
                            Timber.i("do retry $retryFunction")
                            retryFunction()
                        } else {
                            preferenceHelper.setTokenPair("", "")
                            resetApiClient()
                            OTResult.Error(RefreshTokenExpiredException("Refresh token expired"))
                        }
                    } catch (e: Exception) {
                        preferenceHelper.setTokenPair("", "")
                        resetApiClient()
                        OTResult.Error(e)
                    }
                } else {
                    OTResult.Error(e)
                }
            }
            else -> OTResult.Error(e)
        }
    }

    private fun resetApiClient(useRefreshToken: Boolean = false) {
        OTWalletApiClient.clear()
        apiClient = OTWalletApiClient.getInstance(preferenceHelper, useRefreshToken)
    }
}