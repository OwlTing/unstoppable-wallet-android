package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.ResetPasswordResponse
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.SignInByEmailResponse

interface OTAuthDataSource {

    suspend fun signInByEmail(email: String, password: String): OTResult<SignInByEmailResponse>

    suspend fun resetPasswordByEmail(email: String): OTResult<ResetPasswordResponse>
}