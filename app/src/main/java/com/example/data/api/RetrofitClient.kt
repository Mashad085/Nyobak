package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client untuk ChatApp backend.
 *
 * Penggunaan dasar:
 *   // Login biasa
 *   val service = RetrofitClient.getService("http://192.168.1.10:3000")
 *   val resp = service.login(LoginRequest("aqif", "password"))
 *   val accessToken = RetrofitClient.bearerToken(resp.accessToken)
 *   val rooms = service.getRooms(accessToken).rooms
 *
 *   // Endpoint admin (hanya jika resp.adminToken != null)
 *   val adminToken = RetrofitClient.bearerToken(resp.adminToken!!)
 *   val stats = service.getAdminDashboardStats(adminToken)
 *
 * CATATAN: server backend TIDAK memakai Socket.IO — WebSocket tersambung ke:
 *   ws://<host>:<port>/ws?token=<accessToken>        (user biasa)
 *   ws://<host>:<port>/ws/admin?token=<adminToken>   (admin)
 * Gunakan OkHttp WebSocket atau library ws-compatible di Android, bukan socket.io-client.
 */
object RetrofitClient {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Ubah ke Level.NONE di produksi agar tidak membocorkan token di logcat
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)   // lebih panjang untuk response besar
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private var currentBaseUrl: String = ""
    private var cachedService: ChatApiService? = null

    /**
     * Dapatkan instance ChatApiService.
     * Instance di-cache selama baseUrl tidak berubah (caching Retrofit).
     *
     * @param baseUrl Base URL backend, contoh: "http://192.168.1.10:3000"
     *                atau "https://chat.domain.lan:3000" (jika pakai HTTPS).
     *                Trailing slash TIDAK perlu — fungsi ini menambahkan sendiri.
     */
    fun getService(baseUrl: String): ChatApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (normalizedUrl == currentBaseUrl && cachedService != null) {
            return cachedService!!
        }

        currentBaseUrl = normalizedUrl
        cachedService = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChatApiService::class.java)

        return cachedService!!
    }

    /**
     * Helper: tambahkan prefix "Bearer " ke raw token.
     * Semua endpoint @Header("Authorization") mengharapkan format ini.
     *
     * Contoh:
     *   val authHeader = RetrofitClient.bearerToken(authResponse.accessToken)
     *   service.getRooms(authHeader)
     */
    fun bearerToken(rawToken: String): String = "Bearer $rawToken"

    /**
     * Buat header Authorization dari AuthResponse dengan null-safety.
     * Mengembalikan null jika token yang dimaksud tidak tersedia
     * (misalnya adminToken hanya hadir jika role >= moderator).
     */
    fun accessTokenHeader(response: AuthResponse): String =
        bearerToken(response.accessToken)

    fun adminTokenHeader(response: AuthResponse): String? =
        response.adminToken?.let { bearerToken(it) }

    /** Reset cache (berguna saat user logout atau ganti server). */
    fun reset() {
        currentBaseUrl = ""
        cachedService = null
    }
}