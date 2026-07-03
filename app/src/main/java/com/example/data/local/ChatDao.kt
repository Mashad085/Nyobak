package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // --- Users ---
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("UPDATE users SET spamScore = :spamScore WHERE id = :userId")
    suspend fun updateUserSpamScore(userId: String, spamScore: Double)

    // --- Chat Rooms ---
    @Query("SELECT * FROM chat_rooms WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM chat_rooms WHERE id = :roomId")
    suspend fun getRoomById(roomId: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Query("UPDATE chat_rooms SET unreadCount = 0 WHERE id = :roomId")
    suspend fun clearRoomUnreadCount(roomId: String)

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDeleted = 1, deletedByAdmin = :byAdmin WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String, byAdmin: Boolean)

    // --- Banned Words ---
    @Query("SELECT * FROM banned_words ORDER BY word ASC")
    fun getAllBannedWords(): Flow<List<BannedWordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBannedWords(bannedWords: List<BannedWordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBannedWord(bannedWord: BannedWordEntity)

    @Query("DELETE FROM banned_words WHERE id = :id")
    suspend fun deleteBannedWordById(id: String)

    // --- Violation Reports ---
    @Query("SELECT * FROM violation_reports ORDER BY detectedAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE violation_reports SET status = :status, notes = :notes WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String, notes: String)

    // --- System Logs ---
    @Query("SELECT * FROM system_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntity>)

    // --- General Clear ---
    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
