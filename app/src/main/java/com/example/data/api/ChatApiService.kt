package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface untuk ChatApp backend.
 *
 * CATATAN PENTING — Authorization header:
 *   - Endpoint biasa (/api/rooms, /api/users, dll.) → pakai accessToken
 *     Format: "Bearer <accessToken>"
 *   - Endpoint admin (/api/admin/...) → pakai adminToken (secret berbeda!)
 *     Format: "Bearer <adminToken>"
 *     adminToken hanya hadir di response login/register jika role >= moderator.
 *
 * CATATAN — Semua response list DIBUNGKUS dalam objek JSON, bukan array mentah.
 * Contoh: GET /api/rooms → { "rooms": [...] }, bukan langsung [...].
 * Karena itu setiap return type menggunakan kelas Wrapper yang sesuai.
 *
 * CATATAN — WebSocket:
 *   Backend pakai protokol `ws` native (bukan Socket.IO).
 *   Sambung ke: ws://<host>:<port>/ws?token=<accessToken>
 *   Jangan pakai socket.io-client — tidak kompatibel.
 */
interface ChatApiService {

    // ─────────────────────────────────────────────
    // Authentication
    // ─────────────────────────────────────────────

    /** Daftarkan akun baru. Rate limit: 3x per 10 menit per IP. */
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    /** Login. Rate limit: 5x per 15 menit per (IP+username) + 20x per IP. */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    /** Perbarui access token menggunakan refresh token. */
    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse

    // ─────────────────────────────────────────────
    // Users & Profiles  (accessToken)
    // ─────────────────────────────────────────────

    /**
     * Profil user yang sedang login.
     * Response: { "user": UserResponse }
     */
    @GET("api/users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): UserWrapper

    /**
     * Update profil sendiri (displayName dan/atau bio).
     * Response: { "ok": true }
     */
    @PATCH("api/users/me")
    suspend fun updateMyProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): OkResponse

    /**
     * Profil publik user lain berdasarkan userId.
     * Response: { "user": UserResponse }
     */
    @GET("api/users/{id}")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): UserWrapper

    /**
     * Cari user berdasarkan username atau display name.
     * Response: { "users": [...] }
     */
    @GET("api/users")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("q") query: String = ""
    ): UsersWrapper

    // ─────────────────────────────────────────────
    // Rooms & Messages  (accessToken)
    // ─────────────────────────────────────────────

    /**
     * Daftar room yang diikuti user, lengkap dengan:
     * - name (nama lawan chat atau nama grup)
     * - otherUser (hanya untuk type="personal": status online, lastSeen)
     * - lastMessage (preview pesan terakhir sebagai objek, bukan string)
     * - unreadCount
     *
     * Response: { "rooms": [...] }
     */
    @GET("api/rooms")
    suspend fun getRooms(
        @Header("Authorization") token: String
    ): RoomsWrapper

    /**
     * Buat room baru.
     * - type="personal": memberIds harus berisi tepat 1 userId lawan chat
     * - type="group": memberIds boleh lebih dari 1, name wajib diisi
     * Response: { "room": RoomResponse }  — NOTE: dibungkus "room" bukan "rooms"
     */
    @POST("api/rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): CreateRoomResponse

    /**
     * Ambil riwayat pesan dalam sebuah room (paginasi via ?before=<epochMs>).
     * Sekaligus menandai semua pesan yang belum dibaca sebagai 'read',
     * dan mengirim event 'message:read' ke pengirim via WebSocket.
     *
     * Response: { "messages": [...] }
     * Setiap MessageResponse mengandung: id, senderId, senderName, roomId,
     * type, content, replyTo, createdAt, timestamp (alias), wasOffline, isDeleted, deletedByAdmin
     */
    @GET("api/rooms/{id}/messages")
    suspend fun getRoomMessages(
        @Header("Authorization") token: String,
        @Path("id") roomId: String,
        @Query("limit") limit: Int = 100,
        @Query("before") before: Long? = null
    ): MessagesWrapper

    // ─────────────────────────────────────────────
    // File Upload  (accessToken)
    // ─────────────────────────────────────────────

    // Upload file menggunakan Multipart — dideklarasikan di FileUploadService terpisah
    // karena perlu @Multipart dan @Part, yang tidak bisa digabung di interface ini
    // tanpa header Multipart. Lihat FileUploadService.kt.

    // ─────────────────────────────────────────────
    // Admin — Dashboard  (adminToken)
    // ─────────────────────────────────────────────

    /**
     * Statistik dashboard admin.
     * Response berisi KEDUA format:
     * - Nested: totals { totalUsers, activeUsers, messagesToday, pendingReports, bannedUsers }
     *           charts { messagesPerHour, registrationsPerDay }
     *           topActiveUsers, recentReports
     * - Flat alias: totalUsers, activeNow, bannedAccounts, avgSpamScore
     */
    @GET("api/admin/dashboard/stats")
    suspend fun getAdminDashboardStats(
        @Header("Authorization") adminToken: String
    ): DashboardStats

    // ─────────────────────────────────────────────
    // Admin — Reports  (adminToken)
    // ─────────────────────────────────────────────

    /**
     * List laporan pelanggaran. Filter opsional via query param:
     * ?status=pending|reviewed|action_taken|dismissed
     * ?type=profanity|spam|duplicate_message|...
     * ?from=<epochMs>&to=<epochMs>
     *
     * Response: { "reports": [...] }
     * Setiap ReportResponse mengandung alias flat (userId, violationType,
     * messageContent [sudah didekripsi], score, detectedAt) selain field asli.
     */
    @GET("api/admin/reports")
    suspend fun getReports(
        @Header("Authorization") adminToken: String,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null
    ): ReportsWrapper

    /**
     * Detail satu laporan (termasuk plaintext pesan dan riwayat tindakan).
     * Response: { "report": {...}, "message": {...}, "user": {...} }
     */
    @GET("api/admin/reports/{id}")
    suspend fun getReportDetail(
        @Header("Authorization") adminToken: String,
        @Path("id") reportId: String
    ): ReportDetailResponse

    /**
     * Ambil tindakan terhadap laporan.
     * action: "warn" | "mute" | "ban" | "delete_message" | "dismiss"
     * Response: { "ok": true }
     */
    @POST("api/admin/reports/{id}/action")
    suspend fun takeReportAction(
        @Header("Authorization") adminToken: String,
        @Path("id") reportId: String,
        @Body request: ReportActionRequest
    ): OkResponse

    // ─────────────────────────────────────────────
    // Admin — Banned Words  (adminToken)
    // ─────────────────────────────────────────────

    /**
     * List semua kata terlarang.
     * Response: { "words": [...] }
     * PERHATIAN: field menggunakan snake_case (is_active, is_regex) karena
     * langsung dari SQLite. Gunakan helper .active dan .regex di BannedWordResponse.
     */
    @GET("api/admin/banned-words")
    suspend fun getBannedWords(
        @Header("Authorization") adminToken: String
    ): BannedWordsWrapper

    /**
     * Tambah kata terlarang baru.
     * Response: { "ok": true }  (bukan BannedWordResponse!)
     */
    @POST("api/admin/banned-words")
    suspend fun addBannedWord(
        @Header("Authorization") adminToken: String,
        @Body request: BannedWordRequest
    ): OkResponse

    /**
     * Import bulk kata terlarang (array kata).
     * Response: { "ok": true, "imported": N }
     */
    @POST("api/admin/banned-words/import")
    suspend fun importBannedWords(
        @Header("Authorization") adminToken: String,
        @Body request: BulkImportRequest
    ): ImportResponse

    /**
     * Update kata terlarang (isActive, severity, category).
     * Response: { "ok": true }
     */
    @PATCH("api/admin/banned-words/{id}")
    suspend fun updateBannedWord(
        @Header("Authorization") adminToken: String,
        @Path("id") id: String,
        @Body request: UpdateBannedWordRequest
    ): OkResponse

    /**
     * Hapus kata terlarang.
     * Response: { "ok": true }
     */
    @DELETE("api/admin/banned-words/{id}")
    suspend fun deleteBannedWord(
        @Header("Authorization") adminToken: String,
        @Path("id") id: String
    ): OkResponse

    // ─────────────────────────────────────────────
    // Admin — User Management  (adminToken)
    // ─────────────────────────────────────────────

    /**
     * List semua user (admin). Filter: ?status=banned|muted|active&q=<cari>
     * Response: { "users": [...] }
     */
    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Header("Authorization") adminToken: String,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null
    ): UsersWrapper

    /**
     * Detail user termasuk riwayat pelanggaran dan tindakan admin.
     * Response: { "user": {...}, "violations": [...], "recentActions": [...] }
     */
    @GET("api/admin/users/{id}")
    suspend fun getAdminUserDetail(
        @Header("Authorization") adminToken: String,
        @Path("id") userId: String
    ): UserDetailResponse

    /** Kirim peringatan ke user (WS). Response: { "ok": true } */
    @POST("api/admin/users/{id}/warn")
    suspend fun warnUser(
        @Header("Authorization") adminToken: String,
        @Path("id") userId: String,
        @Body body: Map<String, String>   // { "message": "..." }
    ): OkResponse

    /** Bisukan user. Body: { "durationMinutes": 15 }. Response: { "ok": true, "mutedUntil": epochMs } */
    @POST("api/admin/users/{id}/mute")
    suspend fun muteUser(
        @Header("Authorization") adminToken: String,
        @Path("id") userId: String,
        @Body body: Map<String, Int>      // { "durationMinutes": N }
    ): MuteResponse

    /**
     * Ban user (hanya role admin/super_admin).
     * Body: { "reason": "..." }. Response: { "ok": true }
     */
    @POST("api/admin/users/{id}/ban")
    suspend fun banUser(
        @Header("Authorization") adminToken: String,
        @Path("id") userId: String,
        @Body body: Map<String, String>   // { "reason": "..." }
    ): OkResponse

    /** Cabut ban user. Response: { "ok": true } */
    @POST("api/admin/users/{id}/unban")
    suspend fun unbanUser(
        @Header("Authorization") adminToken: String,
        @Path("id") userId: String
    ): OkResponse

    /** Reset spam_score dan cabut mute. Response: { "ok": true } */
    @POST("api/admin/users/{id}/reset-spam")
    suspend fun resetUserSpam(
        @Header("Authorization") adminToken: String,
        @Path("id") userId: String
    ): OkResponse

    // ─────────────────────────────────────────────
    // Admin — Logs  (adminToken)
    // ─────────────────────────────────────────────

    /**
     * Log sistem.
     * ?type=spam (default) | actions | violations
     * ?format=json (default) | csv
     * Response: { "logs": [...] }
     * Setiap LogResponse mengandung field asli + alias Android
     * (timestamp, eventType, actor, ipAddress, description, status).
     */
    @GET("api/admin/logs")
    suspend fun getSystemLogs(
        @Header("Authorization") adminToken: String,
        @Query("type") type: String = "spam",
        @Query("limit") limit: Int = 200
    ): LogsWrapper

    // ─────────────────────────────────────────────
    // Admin — Broker / BullMQ  (adminToken)
    // ─────────────────────────────────────────────

    /**
     * Status semua queue BullMQ.
     * Response: { "queues": { "message.outbox": {waiting,active,completed,failed,delayed}, ... } }
     * PERHATIAN: nama queue mengandung titik (.), gunakan BrokerQueues dengan @Json eksplisit.
     */
    @GET("api/admin/broker/stats")
    suspend fun getBrokerStats(
        @Header("Authorization") adminToken: String
    ): BrokerStatsResponse

    /**
     * Daftar job gagal di sebuah queue (max 50).
     * queueName: "messageOutbox"|"profanityScan"|"spamCheck"|"accountRegister"|"adminReport"|"auditLog"|"notificationPush"
     */
    @GET("api/admin/broker/failed/{queueName}")
    suspend fun getFailedJobs(
        @Header("Authorization") adminToken: String,
        @Path("queueName") queueName: String
    ): FailedJobsResponse

    /** Coba ulang job yang gagal. Response: { "ok": true } */
    @POST("api/admin/broker/retry/{queueName}/{jobId}")
    suspend fun retryJob(
        @Header("Authorization") adminToken: String,
        @Path("queueName") queueName: String,
        @Path("jobId") jobId: String
    ): OkResponse

    // ─────────────────────────────────────────────
    // Admin — Settings  (adminToken super_admin)
    // ─────────────────────────────────────────────

    /** Ambil semua pengaturan. Response: { "settings": { key: value, ... } } */
    @GET("api/admin/settings")
    suspend fun getSettings(
        @Header("Authorization") adminToken: String
    ): SettingsResponse

    /** Update pengaturan (hanya super_admin). Body: { key: value, ... }. Response: { "ok": true } */
    @PATCH("api/admin/settings")
    suspend fun updateSettings(
        @Header("Authorization") adminToken: String,
        @Body settings: Map<String, String>
    ): OkResponse
}

// ─────────────────────────────────────────────────────────────
// Model tambahan yang dipakai oleh ChatApiService
// ─────────────────────────────────────────────────────────────

/** Wrapper untuk POST /api/rooms → { "room": {...} } */
@JsonClass(generateAdapter = true)
data class CreateRoomResponse(
    @Json(name = "room") val room: RoomResponse
)

/** GET /api/admin/reports/:id → { "report": {...}, "message": {...}, "user": {...} } */
@JsonClass(generateAdapter = true)
data class ReportDetailResponse(
    @Json(name = "report")  val report: ReportResponse,
    @Json(name = "message") val message: MessageResponse? = null,
    @Json(name = "user")    val user: UserResponse? = null
)

/** GET /api/admin/users/:id → { "user": {...}, "violations": [...], "recentActions": [...] } */
@JsonClass(generateAdapter = true)
data class UserDetailResponse(
    @Json(name = "user")          val user: UserResponse,
    @Json(name = "violations")    val violations: List<ViolationItem> = emptyList(),
    @Json(name = "recentActions") val recentActions: List<AdminActionItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ViolationItem(
    @Json(name = "id")         val id: String,
    @Json(name = "type")       val type: String,
    @Json(name = "detail")     val detail: String? = null,
    @Json(name = "is_resolved") val isResolved: Int = 0,
    @Json(name = "created_at") val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class AdminActionItem(
    @Json(name = "id")         val id: String,
    @Json(name = "action")     val action: String,
    @Json(name = "detail")     val detail: String? = null,
    @Json(name = "created_at") val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class MuteResponse(
    @Json(name = "ok")         val ok: Boolean = false,
    @Json(name = "mutedUntil") val mutedUntil: Long? = null
)

@JsonClass(generateAdapter = true)
data class BulkImportRequest(
    @Json(name = "words")    val words: List<String>,
    @Json(name = "category") val category: String = "profanity",
    @Json(name = "severity") val severity: Int = 1
)

@JsonClass(generateAdapter = true)
data class ImportResponse(
    @Json(name = "ok")       val ok: Boolean,
    @Json(name = "imported") val imported: Int = 0
)

@JsonClass(generateAdapter = true)
data class UpdateBannedWordRequest(
    @Json(name = "isActive")  val isActive: Boolean? = null,
    @Json(name = "severity")  val severity: Int? = null,
    @Json(name = "category")  val category: String? = null
)

@JsonClass(generateAdapter = true)
data class FailedJobsResponse(
    @Json(name = "jobs") val jobs: List<FailedJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FailedJob(
    @Json(name = "id")           val id: String,
    @Json(name = "name")         val name: String,
    @Json(name = "failedReason") val failedReason: String? = null,
    @Json(name = "attemptsMade") val attemptsMade: Int = 0
)

@JsonClass(generateAdapter = true)
data class SettingsResponse(
    @Json(name = "settings") val settings: Map<String, String>
)