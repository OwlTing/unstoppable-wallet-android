package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
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
import retrofit2.http.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface OTWalletApiClient {

    @POST("api/external/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("api/external/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse

    @POST("api/external/auth/logout")
    suspend fun logout(
    ): LogoutResponse

    @POST("api/external/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse

    @POST("api/external/auth/passwordForget")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): ResetPasswordResponse

    @POST("api/external/auth/terminate")
    suspend fun deleteAccount(
    ): DeleteAccountResponse

    @POST("api/external/customer/{uuid}/profile")
    suspend fun loginByToken(
        @Path("uuid") uuid: String,
        @Body request: LoginByTokenRequest
    ): LoginResponse

    @GET("api/external/customer/aml/country")
    suspend fun getCountries(
        @Query("lang") lang: String = "en",
        @Query("filterType") filterType: String = "eu",
        @Query("nameFormat") nameFormat: String = "iso_code",
    ): CountriesResponse

    @GET("api/external/customer/aml/meta")
    suspend fun getUserMeta(
        @Query("lang") lang: String = "en",
    ): UserMetaResponse

    @POST("api/external/customer/aml/register")
    suspend fun amlMetaRegister(
        @Body request: AmlMetaRegisterRequest
    ): UserMetaResponse

    @POST("api/external/customer/aml/chain/binding")
    suspend fun amlChainRegister(
        @Body request: AmlChainRegisterRequest
    ): UserMetaResponse

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
                            .addHeader("device", "Android")
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
    val email: String,
    val password: String,
    val expire: Float? = /*if (BuildConfig.DEBUG) 0.0167f else*/ null,
)

data class LoginByTokenRequest(
    val token: String,
)

data class LoginResponse(
    val status: Boolean,
    val code: String = "",
    val token: Token,

    @SerializedName("customer")
    val user: User = User(),
    val msg: String = ""
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val gender: String?,
    val birthday: String?,
)

data class RegisterResponse(
    val status: Boolean,
    val code: String = "",
    val token: Token,

    @SerializedName("customer")
    val user: User = User(),
    val msg: String = ""
)

data class User(
    val uuid: String = "",
    val name: String = "",
    val email: String = "",
    val avatar: String = "",

    @SerializedName("created_at")
    val createdAt: String = "",

    @SerializedName("updated_at")
    val updatedAt: String = "",
)

data class LogoutResponse(
    val status: Boolean,
)

data class DeleteAccountResponse(
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

data class ResetPasswordRequest(
    val email: String,
)

data class ResetPasswordResponse(
    val status: Boolean,
)

data class CountriesResponse(
    val status: Boolean,
    val data: List<Country>,
)

data class Country(
    val isoCode: String,
    var name: String,
    val flagUrl: String
)

data class UserMetaResponse(
    val status: Boolean,
    val code: Int = 0,
    val data: UserMeta,
)

enum class VerifyState(val stateName: String) {
    NOT_FOUND(""),
    UNFINISHED("unfinished"),
    UNVERIFIED("unverified"),
    VERIFIED("verified"),
    REJECTED("rejected"),
}

data class UserMeta(
    val ssoUserId: Int = 0,
    val ssoId: String = "",
    val integratedStatus: String = "",
    val ssoUserMeta: Meta,
    val ssoUserChains: List<Chain> = listOf(),
)

data class Meta(
    val ssoUserMetaId: Int = 0,
    val ssoUserId: Int = 0,
    val name: String = "",
    val birthday: String = "",
    val country: Country,
    val email: String = "",
)

data class Chain(
    val ssoUserChainId: Int = 0,
    val ssoUserId: Int = 0,
    val chainNetwork: String = "",
    val chainAsset: String = "",
    val chainAddress: String = "",
    val chainIsBinding: Int = 0,
)

data class AmlMetaRegisterRequest(
    val country: String,
    val birthday: String,
    val name: String,
    val chains: List<AmlMetaRegisterChain>,
)

data class AmlMetaRegisterChain(
    val address: String,
    val network: String,
    val asset: String,
)

data class AmlChainRegisterRequest(
    val chains: List<AmlChainRegisterChain>,
)

data class AmlChainRegisterChain(
    val address: String,
    val network: String,
    val asset: String,
    val isBinding: Int,
)
