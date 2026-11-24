package com.example.melati.data.remote

import com.example.melati.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Perakitan HTTP client. Timeout wajib ada (PRD §6) agar layar tidak menggantung.
 *
 * API key: server Melati menolak request tanpa key (401). Nilainya dibaca dari
 * `BuildConfig.API_KEY` (local.properties `API_KEY`, fallback `-Pmelati.apiKey`),
 * sehingga tidak pernah tertulis di source. Header `X-API-Key` sudah diverifikasi
 * terhadap server test: tanpa key 401, dengan key lolos.
 */
object NetworkModule {

    private const val API_KEY_HEADER = "X-API-KEY"

    fun createApi(baseUrl: String = BuildConfig.BASE_URL): MelatiApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("X-API-KEY")
            redactHeader("X-API-Key")
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS) // upload lampiran
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val key = BuildConfig.API_KEY
                val builder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                if (key.isNotBlank()) {
                    builder.header(API_KEY_HEADER, key)
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(MelatiApi::class.java)
    }
}
