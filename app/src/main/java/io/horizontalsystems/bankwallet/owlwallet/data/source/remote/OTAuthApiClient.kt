package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface OTAuthApiClient {

    @POST("token")
    suspend fun signInByEmail(
        @Body request: SignInByEmailRequest
    ): SignInByEmailResponse

    @POST("passwordForgot")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): ResetPasswordResponse

    companion object {
        @Volatile
        private var instance: OTAuthApiClient? = null

        fun clear() {
            synchronized(this) {
                instance = null
            }
        }

        fun getInstance(): OTAuthApiClient {
            val baseUrl =
                Translator.getString(R.string.owlauthBaseUrl) + "api/project/" + Translator.getString(
                    R.string.owlauthProjectCode
                ) + "/"

            Timber.d("baseUrl: $baseUrl")

            return instance ?: synchronized(this) {
                if (instance == null) {
                    val logging = HttpLoggingInterceptor {
                        Timber.d(it)
                    }
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY)

                    val builder = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .addInterceptor(logging)
                        .cache(null)

                    val gsonBuilder = GsonBuilder().setLenient()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                        .client(builder.build())
                        .build()
                    instance = retrofit.create(OTAuthApiClient::class.java)
                }
                return instance!!
            }
        }
    }
}

data class SignInByEmailRequest(
    val email: String,
    val password: String
)

data class SignInByEmailResponse(
    val status: Boolean,
    val uuid: String = "",
    val secret: String = "",
    val code: String = "",
    val error: String = ""
)

data class ResetPasswordRequest(
    val email: String,
    val expectTo: String = "",
    val subject: String? = null,
    val tmpl: String? = null,
)

data class ResetPasswordResponse(
    val status: Boolean,
    val msg: String = "",
    val code: String = "",
    val error: String = "",
)