package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import io.horizontalsystems.bankwallet.owlwallet.data.*
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTAuthDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OTAuthRemoteDataSource(
    private val apiClient: OTAuthApiClient = OTAuthApiClient.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OTAuthDataSource {

    override suspend fun signInByEmail(
        email: String,
        password: String
    ): OTResult<SignInByEmailResponse> = withContext(ioDispatcher) {
        try {
            val response = apiClient.signInByEmail(SignInByEmailRequest(email, password))
            if (response.status) {
                OTResult.Success(response)
            } else {
                OTResult.Error(
                    when (response.code) {
                        "00003" -> WrongPasswordException(response.error)
                        "30003" -> AccountTerminatedException(response.error)
                        else -> UnknownException("Unknown exception from signInByEmail: ${response.code} ${response.error}")
                    }
                )
            }
        } catch (e: Exception) {
            OTResult.Error(e)
        }
    }

    override suspend fun resetPasswordByEmail(
        email: String
    ): OTResult<ResetPasswordResponse> = withContext(ioDispatcher) {
        try {
            val response = apiClient.resetPassword(ResetPasswordRequest(email))
            if (response.status) {
                OTResult.Success(response)
            } else {
                OTResult.Error(
                    when (response.code) {
                        "10000" -> NotExistException(response.msg)
                        else -> UnknownException("Unknown exception from resetPasswordByEmail: ${response.code} ${response.error} ${response.msg}")
                    }
                )
            }
        } catch (e: Exception) {
            OTResult.Error(e)
        }
    }
}