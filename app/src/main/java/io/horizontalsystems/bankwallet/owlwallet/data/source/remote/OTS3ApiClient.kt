package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface OTS3ApiClient {

    @GET("app_version/owlwallet_dev_version_control.json")
    suspend fun getVersionData(
    ): List<VersionData>

    companion object {
        @Volatile
        private var instance: OTS3ApiClient? = null
        fun getInstance(): OTS3ApiClient {
            val baseUrl = "https://owlting-cdn.s3.ap-northeast-1.amazonaws.com/"
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
                    instance = retrofit.create(OTS3ApiClient::class.java)
                }
                return instance!!
            }
        }
    }
}

data class VersionData(
    val type: String,
    val version: String,
    val build: Int,
    val description: String = "",
)