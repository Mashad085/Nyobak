package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username")    val username: String,
    @Json(name = "password")    val password: String,
    @Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "accessToken")  val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "user")         val user: UserResponse,
    // Hanya hadir jika role >= moderator
    @Json(name = "adminToken")   val adminToken: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponse(
    @Json(name = "accessToken") val accessToken: String
)

// ─────────────────────────────────────────────────────────────
// User
// Backend mengembalikan field snake_case dari SQLite (display_name, is_banned, dll.)
// CATATAN: endpoint berbeda mengembalikan subset field yang berbeda — semua nullable.
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id")             val id: String,
    @Json(name = "username")       val username: String,

    // GET /api/users/me → display_name; GET /api/rooms → displayName (camelCase alias)
    @Json(name = "display_name")   val displayName: String? = null,

    @Json(name = "role")           val role: String? = null,   // "user"|"moderator"|"admin"|"super_admin"
    @Json(name = "bio")            val bio: String? = null,

    // Path relatif file, bukan URL penuh. Susun URL lengkap: "$API_URL/api/files/$avatarPath"
    @Json(name = "avatar_path")    val avatarPath: String? = null,

    // 0 = tidak banned, 1 = banned (SQLite INTEGER)
    @Json(name = "is_banned")      val isBanned: Int? = null,
    @Json(name = "ban_reason")     val banReason: String? = null,
    @Json(name = "muted_until")    val mutedUntil: Long? = null,  // epoch ms, null = tidak dimute
    @Json(name = "spam_score")     val spamScore: Int? = null,

    // 0 = offline, 1 = online
    @Json(name = "is_online")      val isOnline: Int? = null,
    @Json(name = "last_seen")      val lastSeen: Long? = null,    // epoch ms

    // Hanya hadir di endpoint admin
    @Json(name = "registered_ip")  val registeredIp: String? = null,
    @Json(name = "created_at")     val createdAt: Long? = null,
    @Json(name = "status")         val status: String? = null
) {
    /** Helper: true jika user sedang dibanned */
    val banned: Boolean get() = isBanned == 1

    /** Helper: true jika sedang dimute dan waktu mute belum berakhir */
    val muted: Boolean get() = mutedUntil != null && mutedUntil > System.currentTimeMillis()

    /** Status ringkas untuk tampilan UI */
    val statusLabel: String get() = when {
        banned -> "Banned"
        muted  -> "Dimute"
        isOnline == 1 -> "Online"
        else -> "Offline"
    }
}

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "bio")         val bio: String? = null
)

// ─────────────────────────────────────────────────────────────
// Room
// ─────────────────────────────────────────────────────────────

/** Lawan chat di room personal (hanya ada untuk type = "personal") */
@JsonClass(generateAdapter = true)
data class OtherUser(
    @Json(name = "id")          val id: String,
    @Json(name = "username")    val username: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "isOnline")    val isOnline: Boolean = false,
    @Json(name = "lastSeen")    val lastSeen: Long? = null
)

/** Objek preview pesan terakhir di list room */
@JsonClass(generateAdapter = true)
data class LastMessagePreview(
    @Json(name = "senderId")  val senderId: String,
    @Json(name = "type")      val type: String,           // "text"|"image"|"file"|"audio"|"voice"
    @Json(name = "content")   val content: String? = null, // null jika pesan dihapus
    @Json(name = "createdAt") val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class RoomResponse(
    @Json(name = "id")          val id: String,
    @Json(name = "type")        val type: String,          // "personal"|"group"|"broadcast"
    @Json(name = "name")        val name: String? = null,  // nama grup, atau nama lawan chat (personal)
    @Json(name = "avatarPath")  val avatarPath: String? = null,
    @Json(name = "otherUser")   val otherUser: OtherUser? = null,
    @Json(name = "lastMessage") val lastMessage: LastMessagePreview? = null,
    @Json(name = "unreadCount") val unreadCount: Int = 0,
    @Json(name = "createdAt")   val createdAt: Long
)

/** Wrapper JSON: GET /api/rooms → { "rooms": [...] } */
@JsonClass(generateAdapter = true)
data class RoomsWrapper(
    @Json(name = "rooms") val rooms: List<RoomResponse>
)

@JsonClass(generateAdapter = true)
data class CreateRoomRequest(
    @Json(name = "type")      val type: String,                    // "personal"|"group"|"broadcast"
    @Json(name = "name")      val name: String? = null,
    @Json(name = "memberIds") val memberIds: List<String>
)

// ─────────────────────────────────────────────────────────────
// Message
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class MessageResponse(
    @Json(name = "id")             val id: String,
    @Json(name = "senderId")       val senderId: String,
    @Json(name = "senderName")     val senderName: String? = null,  // display_name atau username
    @Json(name = "roomId")         val roomId: String? = null,
    @Json(name = "type")           val type: String = "text",        // "text"|"image"|"file"|"audio"|"voice"|"system"
    @Json(name = "content")        val content: String? = null,
    @Json(name = "replyTo")        val replyTo: String? = null,
    @Json(name = "createdAt")      val createdAt: Long = 0L,
    // Alias timestamp = createdAt (keduanya hadir di response)
    @Json(name = "timestamp")      val timestamp: Long = 0L,
    @Json(name = "wasOffline")     val wasOffline: Boolean = false,
    @Json(name = "isDeleted")      val isDeleted: Boolean = false,
    @Json(name = "deletedByAdmin") val deletedByAdmin: Boolean = false
) {
    /** Waktu pesan (pakai yang tersedia, kreatedAt lebih diutamakan) */
    val messageTime: Long get() = if (createdAt > 0) createdAt else timestamp
}

/** Wrapper JSON: GET /api/rooms/:id/messages → { "messages": [...] } */
@JsonClass(generateAdapter = true)
data class MessagesWrapper(
    @Json(name = "messages") val messages: List<MessageResponse>
)

// ─────────────────────────────────────────────────────────────
// Dashboard Admin
// GET /api/admin/dashboard/stats mengembalikan KEDUA format:
//  - Nested (untuk admin panel web): totals, charts, topActiveUsers, recentReports
//  - Flat alias (untuk Android): totalUsers, activeNow, bannedAccounts, avgSpamScore
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DashboardTotals(
    @Json(name = "totalUsers")     val totalUsers: Int,
    @Json(name = "activeUsers")    val activeUsers: Int,
    @Json(name = "messagesToday")  val messagesToday: Int,
    @Json(name = "pendingReports") val pendingReports: Int,
    @Json(name = "bannedUsers")    val bannedUsers: Int
)

@JsonClass(generateAdapter = true)
data class ChartPoint(
    @Json(name = "hour")  val hour: Int? = null,
    @Json(name = "day")   val day: String? = null,
    @Json(name = "count") val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class DashboardCharts(
    @Json(name = "messagesPerHour")   val messagesPerHour: List<ChartPoint> = emptyList(),
    @Json(name = "registrationsPerDay") val registrationsPerDay: List<ChartPoint> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TopActiveUser(
    @Json(name = "id")            val id: String,
    @Json(name = "username")      val username: String,
    @Json(name = "message_count") val messageCount: Int
)

@JsonClass(generateAdapter = true)
data class RecentReport(
    @Json(name = "id")             val id: String,
    @Json(name = "status")         val status: String,
    @Json(name = "created_at")     val createdAt: Long,
    @Json(name = "violation_type") val violationType: String,
    @Json(name = "username")       val username: String
)

/** Response lengkap dari GET /api/admin/dashboard/stats */
@JsonClass(generateAdapter = true)
data class DashboardStats(
    // Nested (admin panel web)
    @Json(name = "totals")         val totals: DashboardTotals? = null,
    @Json(name = "charts")         val charts: DashboardCharts? = null,
    @Json(name = "topActiveUsers") val topActiveUsers: List<TopActiveUser> = emptyList(),
    @Json(name = "recentReports")  val recentReports: List<RecentReport> = emptyList(),
    // Flat alias (Android)
    @Json(name = "totalUsers")     val totalUsers: Int = 0,
    @Json(name = "activeNow")      val activeNow: Int = 0,
    @Json(name = "bannedAccounts") val bannedAccounts: Int = 0,
    @Json(name = "avgSpamScore")   val avgSpamScore: Double = 0.0
)

// ─────────────────────────────────────────────────────────────
// Banned Words Admin
// Backend mengembalikan snake_case dari SQLite (is_active, is_regex)
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BannedWordResponse(
    @Json(name = "id")         val id: String,
    @Json(name = "word")       val word: String,
    @Json(name = "category")   val category: String? = null,
    @Json(name = "severity")   val severity: Int = 1,
    // Backend menyimpan sebagai INTEGER (0/1), bukan Boolean
    @Json(name = "is_regex")   val isRegex: Int = 0,
    @Json(name = "is_active")  val isActive: Int = 1,
    @Json(name = "added_by")   val addedBy: String? = null,
    @Json(name = "created_at") val createdAt: Long = 0L
) {
    val regex: Boolean  get() = isRegex == 1
    val active: Boolean get() = isActive == 1
}

@JsonClass(generateAdapter = true)
data class BannedWordRequest(
    @Json(name = "word")     val word: String,
    @Json(name = "category") val category: String = "profanity",
    @Json(name = "severity") val severity: Int = 1,
    @Json(name = "isRegex")  val isRegex: Boolean = false
)

/** Wrapper JSON: GET /api/admin/banned-words → { "words": [...] } */
@JsonClass(generateAdapter = true)
data class BannedWordsWrapper(
    @Json(name = "words") val words: List<BannedWordResponse>
)

// ─────────────────────────────────────────────────────────────
// Reports Admin
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ReportResponse(
    // Field asli backend
    @Json(name = "id")               val id: String,
    @Json(name = "status")           val status: String,     // "pending"|"reviewed"|"action_taken"|"dismissed"
    @Json(name = "action_taken")     val actionTaken: String? = null,
    @Json(name = "notes")            val notes: String? = null,
    @Json(name = "created_at")       val createdAt: Long = 0L,
    @Json(name = "resolved_at")      val resolvedAt: Long? = null,
    @Json(name = "violation_id")     val violationId: String = "",
    @Json(name = "violation_type")   val violationType: String = "",
    @Json(name = "message_id")       val messageId: String? = null,
    @Json(name = "user_id")          val userId: String = "",
    @Json(name = "username")         val username: String = "",
    @Json(name = "display_name")     val displayName: String? = null,
    // Alias flat (kompatibilitas Android)
    @Json(name = "violationType")    val violationTypeAlias: String? = null,
    @Json(name = "userId")           val userIdAlias: String? = null,
    @Json(name = "messageContent")   val messageContent: String? = null,   // plaintext sudah didekripsi
    @Json(name = "score")            val score: Int = 0,                   // spam_score user
    @Json(name = "detectedAt")       val detectedAt: Long = 0L             // waktu pelanggaran terdeteksi
) {
    val effectiveUserId: String         get() = userIdAlias ?: userId
    val effectiveViolationType: String  get() = violationTypeAlias ?: violationType
}

@JsonClass(generateAdapter = true)
data class ReportActionRequest(
    @Json(name = "action")          val action: String,                // "warn"|"mute"|"ban"|"delete_message"|"dismiss"
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,  // untuk mute
    @Json(name = "reason")          val reason: String? = null
)

/** Wrapper JSON: GET /api/admin/reports → { "reports": [...] } */
@JsonClass(generateAdapter = true)
data class ReportsWrapper(
    @Json(name = "reports") val reports: List<ReportResponse>
)

// ─────────────────────────────────────────────────────────────
// Logs Admin
// GET /api/admin/logs?type=spam|actions|violations
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LogResponse(
    // Field asli (bervariasi tergantung type)
    @Json(name = "id")         val id: String = "",          // UUID string, BUKAN Int
    @Json(name = "ip")         val ip: String? = null,
    @Json(name = "user_id")    val userId: String? = null,
    @Json(name = "type")       val type: String? = null,
    @Json(name = "detail")     val detail: String? = null,
    @Json(name = "created_at") val createdAt: Long = 0L,
    // Alias untuk Android (ditambahkan backend)
    @Json(name = "timestamp")   val timestamp: String? = null,  // ISO 8601
    @Json(name = "eventType")   val eventType: String? = null,
    @Json(name = "actor")       val actor: String? = null,
    @Json(name = "ipAddress")   val ipAddress: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "status")      val status: String? = null
)

/** Wrapper JSON: GET /api/admin/logs → { "logs": [...] } */
@JsonClass(generateAdapter = true)
data class LogsWrapper(
    @Json(name = "logs") val logs: List<LogResponse>
)

// ─────────────────────────────────────────────────────────────
// Broker / BullMQ Admin
// GET /api/admin/broker/stats → { "queues": { "message.outbox": {...}, ... } }
// Nama queue memakai titik (.), wajib pakai @Json(name = ...) eksplisit
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class QueueStats(
    @Json(name = "waiting")   val waiting: Int = 0,
    @Json(name = "active")    val active: Int = 0,
    @Json(name = "completed") val completed: Int = 0,
    @Json(name = "failed")    val failed: Int = 0,
    @Json(name = "delayed")   val delayed: Int = 0
)

/** Isi objek "queues" di dalam response broker stats */
@JsonClass(generateAdapter = true)
data class BrokerQueues(
    @Json(name = "message.outbox")      val messageOutbox: QueueStats? = null,
    @Json(name = "profanity.scan")      val profanityScan: QueueStats? = null,
    @Json(name = "spam.check")          val spamCheck: QueueStats? = null,
    @Json(name = "account.register")    val accountRegister: QueueStats? = null,
    @Json(name = "admin.report")        val adminReport: QueueStats? = null,
    @Json(name = "audit.log")           val auditLog: QueueStats? = null,
    @Json(name = "notification.push")   val notificationPush: QueueStats? = null
)

/** Response lengkap: GET /api/admin/broker/stats → { "queues": {...} } */
@JsonClass(generateAdapter = true)
data class BrokerStatsResponse(
    @Json(name = "queues") val queues: BrokerQueues
)

// ─────────────────────────────────────────────────────────────
// Users Admin Wrapper
// ─────────────────────────────────────────────────────────────

/** Wrapper JSON: GET /api/users → { "users": [...] }
 *  dan GET /api/admin/users → { "users": [...] } */
@JsonClass(generateAdapter = true)
data class UsersWrapper(
    @Json(name = "users") val users: List<UserResponse>
)

/** Wrapper JSON: GET /api/users/me → { "user": {...} } */
@JsonClass(generateAdapter = true)
data class UserWrapper(
    @Json(name = "user") val user: UserResponse
)

// ─────────────────────────────────────────────────────────────
// Generic ok response
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class OkResponse(
    @Json(name = "ok")      val ok: Boolean = false,
    @Json(name = "error")   val error: String? = null,
    @Json(name = "message") val message: String? = null
)