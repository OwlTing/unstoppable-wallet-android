package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import io.horizontalsystems.bankwallet.owlwallet.data.AccountDeletedException
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

    override fun verifyStateFlow(): Flow<VerifyState> {
        return preferenceHelper.verifyStateFlow()
    }

    override suspend fun login(
        email: String, password: String,
    ): OTResult<LoginResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.login(LoginRequest(email, password))
                if (response.status) {
                    preferenceHelper.login(
                        accessToken = response.token.access,
                        refreshToken = response.token.refresh,
                        user = response.user,
                    )
                    resetApiClient()
                    OTResult.Success(response)
                } else {
                    if (response.code == "30003") {
                        OTResult.Error(AccountDeletedException("Account deleted"))
                    } else {
                        OTResult.Error(Exception(response.msg))
                    }
                }
            } catch (e: Exception) {
                OTResult.Error(e)
            }
        }

    override suspend fun loginByToken(uuid: String, token: String): OTResult<LoginResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.loginByToken(uuid, LoginByTokenRequest(token))
                if (response.status) {
                    preferenceHelper.login(
                        accessToken = response.token.access,
                        refreshToken = response.token.refresh,
                        user = response.user,
                    )
                    resetApiClient()
                    OTResult.Success(response)
                } else {
                    if (response.code == "30003") {
                        OTResult.Error(AccountDeletedException("Account deleted"))
                    } else {
                        OTResult.Error(Exception(response.msg))
                    }
                }
            } catch (e: Exception) {
                OTResult.Error(e)
            }
        }

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        gender: String?,
        birthday: String?,
    ): OTResult<RegisterResponse> =
        withContext(ioDispatcher) {
            try {
                val response =
                    apiClient.register(RegisterRequest(email, password, name, gender, birthday))
                Timber.d("response: $response")
                if (response.status) {
                    preferenceHelper.login(
                        accessToken = response.token.access,
                        refreshToken = response.token.refresh,
                        user = response.user,
                    )
                    resetApiClient()
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception())
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
                    preferenceHelper.logout()
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

    override suspend fun resetPassword(email: String): OTResult<ResetPasswordResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.resetPassword(ResetPasswordRequest(email))
                if (response.status) {
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception("Reset Password failed"))
                }
            } catch (e: Exception) {
                OTResult.Error(Exception("Reset Password failed"))
            }
        }

    override suspend fun deleteAccount(): OTResult<DeleteAccountResponse> = withContext(ioDispatcher) {
            try {
                val response = apiClient.deleteAccount()
                if (response.status) {
                    preferenceHelper.logout()
                    resetApiClient()
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception("Delete account failed"))
                }
            } catch (e: Exception) {
                handleException(e) {
                    deleteAccount()
                }
            }
        }

    override suspend fun getCountries(
        lang: String,
        filterType: String,
        nameFormat: String
    ): OTResult<CountriesResponse> = withContext(ioDispatcher) {
        try {
            val response = apiClient.getCountries(
                lang, filterType, nameFormat
            )
            if (response.status) {
                response.data.forEach {
                    try {
                        it.name = it.name.split(" - ")[1]
                    } catch (_: Exception) {
                    }
                }
                OTResult.Success(response)
            } else {
                OTResult.Error(Exception("Get Countries failed"))
            }
        } catch (e: Exception) {
            handleException(e) {
                getCountries(lang, filterType, nameFormat)
            }
        }
    }

    override suspend fun getUserMeta(
        lang: String,
    ): OTResult<UserMetaResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.getUserMeta(lang)
                if (response.status) {
                    preferenceHelper.setUserMeta(response.data)
                    OTResult.Success(response)
                } else if (response.code == 10032) {
                    preferenceHelper.setUserMeta(null)
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception("Get User Meta failed"))
                }
            } catch (e: Exception) {
                handleException(e) {
                    getUserMeta(lang)
                }
            }
        }

    override suspend fun amlMetaRegister(request: AmlMetaRegisterRequest): OTResult<UserMetaResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.amlMetaRegister(request)
                if (response.status) {
                    preferenceHelper.setUserMeta(response.data)
                    OTResult.Success(response)
                } else if (response.code == 10032) {
                    preferenceHelper.setUserMeta(null)
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception("aml meta register failed"))
                }
            } catch (e: Exception) {
                handleException(e) {
                    amlMetaRegister(request)
                }
            }
        }

    override suspend fun amlChainRegister(request: AmlChainRegisterRequest): OTResult<UserMetaResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiClient.amlChainRegister(request)
                if (response.status) {
                    OTResult.Success(response)
                } else {
                    OTResult.Error(Exception("aml chain register failed"))
                }
            } catch (e: Exception) {
                handleException(e) {
                    amlChainRegister(request)
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
                            preferenceHelper.logout()
                            resetApiClient()
                            OTResult.Error(RefreshTokenExpiredException("Refresh token expired"))
                        }
                    } catch (e: HttpException) {
                        preferenceHelper.logout()
                        resetApiClient()
                        if (e.code() == 401) {
                            OTResult.Error(RefreshTokenExpiredException("Refresh token expired"))
                        } else {
                            OTResult.Error(e)
                        }
                    } catch (e: Exception) {
                        preferenceHelper.logout()
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