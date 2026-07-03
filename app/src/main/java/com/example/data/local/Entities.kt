package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val role: String, // "user", "moderator", "admin", "super_admin"
    val status: String, // "Active", "Muted", "Banned"
    val bio: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val spamScore: Double = 0.0,
    val mutedUntil: Long? = null,
    val isOnline: Boolean = false
)

@Entity(tableName = "chat_rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "personal", "group", "broadcast"
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val avatarUrl: String = "",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val type: String = "text", // "text", "file"
    val replyTo: String? = null,
    val wasOffline: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedByAdmin: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentSize: String? = null
)

@Entity(tableName = "banned_words")
data class BannedWordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val category: String, // "Hate Speech", "Profanity", "Spam", "Harassment"
    val severity: Int, // 1, 2, 3
    val isRegex: Boolean = false,
    val isActive: Boolean = true
)

@Entity(tableName = "violation_reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    var violationType: String, // "Spam", "Profanity", "Regex", "Manual"
    val messageContent: String,
    val score: Int,
    val status: String, // "Pending", "Reviewed"
    val detectedAt: Long,
    val notes: String = ""
)

@Entity(tableName = "system_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: String,
    val eventType: String, // "SPAM_DETECT", "ADMIN_ACTION", "AUTH_FAILURE", "AUTH_SUCCESS", "SYS_WARN"
    val actor: String,
    val ipAddress: String,
    val description: String,
    val status: String // "MUTED", "SUCCESS", "BLOCKED", "ACTIVE", "LOGGED"
)
