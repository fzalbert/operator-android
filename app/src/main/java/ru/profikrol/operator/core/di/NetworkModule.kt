package ru.profikrol.operator.core.di

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import ru.profikrol.operator.BuildConfig
import ru.profikrol.operator.data.remote.auth.AccessTokenAuthenticator
import ru.profikrol.operator.data.remote.auth.AuthApi
import ru.profikrol.operator.data.remote.auth.AuthTokenInterceptor
import ru.profikrol.operator.data.remote.profile.ProfileApi
import ru.profikrol.operator.data.remote.rabbit.RabbitApi
import ru.profikrol.operator.data.remote.cell.CellApi
import ru.profikrol.operator.data.remote.production.ProductionTaskApi
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Gateway: сам раскидывает запросы по Auth, основному API и ProductionProgram.
    private val BASE_URL = BuildConfig.API_BASE_URL
    private const val AUTH_LOG_TAG = "RabbitAuth"
    private const val HTTP_LOG_TAG = "RabbitHttp"
    private const val LOG_CHUNK_SIZE = 3_000
    private val ALLOW_UNSAFE_CERTIFICATES = BuildConfig.DEBUG

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            // Logcat обрезает строки длиннее ~4 КБ, поэтому длинные тела режем на куски.
            message.chunked(LOG_CHUNK_SIZE).forEach { Log.d(HTTP_LOG_TAG, it) }
        }.apply {
            // В debug видно запросы и ответы целиком, в release ничего не логируется.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }

    @Provides
    @Singleton
    @Named("authless")
    fun provideAuthlessClient(logging: HttpLoggingInterceptor): OkHttpClient =
        baseClient(logging).build()

    @Provides
    @Singleton
    @Named("authless")
    fun provideAuthlessRetrofit(
        @Named("authless") client: OkHttpClient,
        json: Json,
    ): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @Named("authless")
    fun provideAuthApi(@Named("authless") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        tokenInterceptor: AuthTokenInterceptor,
        tokenAuthenticator: AccessTokenAuthenticator,
    ): OkHttpClient = baseClient(logging)
        .addInterceptor(tokenInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json)

    @Provides
    @Singleton
    fun provideProfileApi(
        retrofit: Retrofit,
    ): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideProductionTaskApi(
        retrofit: Retrofit,
    ): ProductionTaskApi = retrofit.create(ProductionTaskApi::class.java)

    @Provides
    @Singleton
    fun provideRabbitApi(
        retrofit: Retrofit,
    ): RabbitApi = retrofit.create(RabbitApi::class.java)

    @Provides
    @Singleton
    fun provideCellApi(
        retrofit: Retrofit,
    ): CellApi = retrofit.create(CellApi::class.java)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun baseClient(logging: HttpLoggingInterceptor): OkHttpClient.Builder =
        OkHttpClient.Builder()
            // Сетевой интерцептор: в лог попадает запрос уже с токеном и повторы после refresh.
            .addNetworkInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .apply {
                if (ALLOW_UNSAFE_CERTIFICATES) {
                    Log.w(AUTH_LOG_TAG, "Unsafe TLS checks are enabled for API client")
                    sslSocketFactory(unsafeSslSocketFactory, unsafeTrustManager)
                    hostnameVerifier(unsafeHostnameVerifier)
                }
            }

    private val unsafeTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    private val unsafeSslSocketFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(unsafeTrustManager), SecureRandom())
    }.socketFactory
    private val unsafeHostnameVerifier = HostnameVerifier { _, _ -> true }
}
