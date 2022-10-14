package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface OTWalletApiClient {

    @POST("api/external/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("api/external/auth/logout")
    suspend fun logout(
    ): LogoutResponse

    @POST("api/external/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse

    @GET("api/external/customer/wallet")
    suspend fun getWallets(
    ): GetWalletsResponse

    @POST("api/external/customer/wallet/create")
    suspend fun syncWallets(
        @Body request: SyncWalletsRequest
    ): SyncWalletsResponse

    companion object {
        @Volatile
        private var instance: OTWalletApiClient? = null

        fun clear() {
            synchronized(this) {
                instance = null
            }
        }

        fun getInstance(
            preferenceHelper: PreferenceHelper,
            useRefreshToken: Boolean = false
        ): OTWalletApiClient {
            Timber.d("getInstance $useRefreshToken")
            val baseUrl =
                Translator.getString(R.string.owlwalletBaseUrl)
            return instance ?: synchronized(this) {
                if (instance == null) {
                    val pair = preferenceHelper.getTokenPair()
                    val token = if (!useRefreshToken) pair.first else pair.second
                    val logging = HttpLoggingInterceptor {
                        Timber.d(it)
                    }
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY)

                    val builder = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .cache(null)

                    builder.addInterceptor(Interceptor { chain ->
                        val newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .build()
                        chain.proceed(newRequest)
                    })

                    if (BuildConfig.DEBUG) {
                        builder.addInterceptor(logging)
                    }

                    val gsonBuilder = GsonBuilder().setLenient()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                        .client(builder.build())
                        .build()
                    Timber.d("create retrofit")
                    instance = retrofit.create(OTWalletApiClient::class.java)
                }
                return instance!!
            }
        }
    }
}

data class LoginRequest(
    val uuid: String,
    val secret: String,
    val expire: Float? = /*if (BuildConfig.DEBUG) 0.0167f else*/ null,
)

data class LoginResponse(
    val status: Boolean,
    val token: Token,
    val msg: String = ""
)

data class LogoutResponse(
    val status: Boolean,
)

data class RefreshTokenRequest(
    val expire: Float? = /*if (BuildConfig.DEBUG) 0.0167f else*/ null,
)

data class RefreshTokenResponse(
    val status: Boolean,
    val token: Token
)

data class Token(
    val access: String,
    val refresh: String,
)

data class GetWalletsResponse(
    val status: Boolean,
)

data class SyncWalletsRequest(
    val wallet: List<OTWallet>
)

data class OTWallet(
    val address: String,
    val currency: String,
    val symbol: String,
    val decimals: String,
    val vendor: String = "OwlTing",
    val type: String = "Withdrawal",
    val data: String? = null,
)

data class SyncWalletsResponse(
    val status: Boolean,
    val data: List<SyncErrorData>,
)

data class SyncErrorData(
    val check: Boolean,
    val code: Int,
    val msg: String,
)



