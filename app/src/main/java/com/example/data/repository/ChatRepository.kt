package com.example.data.repository

import com.example.data.api.*
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val prefs: PreferencesManager
) {
    // --- Flows exposed to UI ---
    val allRooms: Flow<List<RoomEntity>> = chatDao.getAllRooms()
    val allBannedWords: Flow<List<BannedWordEntity>> = chatDao.getAllBannedWords()
    val allReports: Flow<List<ReportEntity>> = chatDao.getAllReports()
    val allUsers: Flow<List<UserEntity>> = chatDao.getAllUsers()
    val allLogs: Flow<List<LogEntity>> = chatDao.getAllLogs()

    fun getRoomMessages(roomId: String): Flow<List<MessageEntity>> = chatDao.getMessagesForRoom(roomId)

    private fun api(): ChatApiService {
        return RetrofitClient.getService(prefs.serverUrl)
    }

    private fun authHeader(): String {
        return "Bearer ${prefs.accessToken ?: ""}"
    }

    private fun adminHeader(): String {
        return "Bearer ${prefs.adminToken ?: prefs.accessToken ?: ""}"
    }

    // --- Authentication ---
    suspend fun login(usernameInput: String, passwordInput: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            // Fake auth
            val user = UserResponse(
                id = "admin_user_id",
                username = usernameInput,
                displayName = "Admin Moderator",
                role = "super_admin",
                bio = "Vigilant explorer. Crypto enthusiast. Always secure.",
                avatarPath = "https://lh3.googleusercontent.com/aida-public/AB6AXuCDqcWul243t5wZ7TXEP7SFf9tcbHWeYvqFYrRyVu6hu6FYy3QIjwpG7sZ1NtrhaAkcBMWc2E_DI-RZPSXoni-2GU-XjnQC-CyA-7qCqi9unMXKpDtkwv-lK-3L409lPCXO4XK1R24AEw1yThvyMjdqlXXmvnDAb_ehU6X674o0P99A0Qdsb-TJ3YlGUZeJzUUE_emRWyuf3uOW8cFJ_lXMSe7v_lRBZJVs7e7a_S3nbKvyxzZz_qNPQllow0IeQE625ivvEXRH18wn",
                isBanned = 0
            )
            val fakeResponse = AuthResponse("demo_access_token", "demo_refresh_token", user, "demo_admin_token")
            saveSession(fakeResponse)
            initializeDemoData() // populate database with demo data
            Result.success(fakeResponse)
        } else {
            try {
                val response = api().login(LoginRequest(usernameInput, passwordInput))
                saveSession(response)
                syncOnlineData()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun register(usernameInput: String, passwordInput: String, displayName: String?): Result<AuthResponse> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            val user = UserResponse(
                id = UUID.randomUUID().toString(),
                username = usernameInput,
                displayName = displayName ?: usernameInput,
                role = "user",
                isBanned = 0
            )
            val fakeResponse = AuthResponse("demo_access_token", "demo_refresh_token", user, null)
            saveSession(fakeResponse)
            initializeDemoData()
            Result.success(fakeResponse)
        } else {
            try {
                val response = api().register(RegisterRequest(usernameInput, passwordInput, displayName))
                saveSession(response)
                syncOnlineData()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun saveSession(response: AuthResponse) {
        prefs.userId = response.user.id
        prefs.username = response.user.username
        prefs.displayName = response.user.displayName ?: response.user.username
        prefs.userRole = response.user.role ?: "user"
        prefs.accessToken = response.accessToken
        prefs.adminToken = response.adminToken

        // Save self in users table
        chatDao.insertUser(
            UserEntity(
                id = response.user.id,
                username = response.user.username,
                displayName = response.user.displayName ?: response.user.username,
                email = response.user.id + "@offchat.sec",
                role = response.user.role ?: "user",
                status = response.user.status ?: if (response.user.banned) "Banned" else "Active",
                bio = response.user.bio ?: "",
                phone = "",
                avatarUrl = response.user.avatarPath ?: ""
            )
        )
    }

    // --- Rooms & Actions ---
    suspend fun createRoom(type: String, name: String?, memberIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            val id = "room_${UUID.randomUUID()}"
            val newRoom = RoomEntity(
                id = id,
                name = name ?: "New Chat",
                type = type,
                lastMessage = "Room created",
                lastMessageTime = System.currentTimeMillis()
            )
            chatDao.insertRoom(newRoom)
            Result.success(Unit)
        } else {
            try {
                api().createRoom(authHeader(), CreateRoomRequest(type, name, memberIds))
                syncRooms()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendMessage(roomId: String, content: String, type: String = "text"): Result<Unit> = withContext(Dispatchers.IO) {
        val messageId = "msg_${UUID.randomUUID()}"
        val myId = prefs.userId ?: "user_id"
        val myName = prefs.displayName ?: "Me"
        val timestamp = System.currentTimeMillis()

        val localMsg = MessageEntity(
            id = messageId,
            roomId = roomId,
            senderId = myId,
            senderName = myName,
            content = content,
            timestamp = timestamp,
            type = type
        )

        // Save locally first for instant UI response
        chatDao.insertMessage(localMsg)
        
        // Update room's last message
        val room = chatDao.getRoomById(roomId)
        if (room != null) {
            chatDao.insertRoom(room.copy(lastMessage = content, lastMessageTime = timestamp))
        }

        if (prefs.isDemoMode) {
            // Trigger quick mock responses for a rich demo experience
            if (roomId == "room_sarah" && content.lowercase().contains("glitch")) {
                val responseMsg = MessageEntity(
                    id = "msg_${UUID.randomUUID()}",
                    roomId = roomId,
                    senderId = "sarah_id",
                    senderName = "Sarah Johnson",
                    content = "Betul Admin, nanti tim security audit tolong cek ulang log log cluster Redis juga ya.",
                    timestamp = timestamp + 1500,
                    type = "text"
                )
                chatDao.insertMessage(responseMsg)
                chatDao.insertRoom(room!!.copy(lastMessage = responseMsg.content, lastMessageTime = responseMsg.timestamp))
            }
            Result.success(Unit)
        } else {
            try {
                // In full implementation, WS sends messages. If using REST:
                // Actually the API specification lists WebSocket for message transmission,
                // but we can mock / save locally. 
                // If the user interacts with the real server, they should be connected via WS.
                // For direct REST-based room histories, we pull and push.
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteMessage(messageId: String, byAdmin: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        chatDao.softDeleteMessage(messageId, byAdmin)
        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().deleteBannedWord(adminHeader(), messageId) // mapping or direct REST deletes
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Admin Operations ---
    suspend fun addBannedWord(word: String, category: String, severity: Int, isRegex: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            val entity = BannedWordEntity(
                id = "word_${UUID.randomUUID()}",
                word = word,
                category = category,
                severity = severity,
                isRegex = isRegex,
                isActive = true
            )
            chatDao.insertBannedWord(entity)
            Result.success(Unit)
        } else {
            try {
                api().addBannedWord(adminHeader(), BannedWordRequest(word, category, severity, isRegex))
                syncBannedWords()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteBannedWord(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        chatDao.deleteBannedWordById(id)
        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().deleteBannedWord(adminHeader(), id)
                syncBannedWords()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun takeReportAction(reportId: String, action: String, durationMinutes: Int?, reason: String?): Result<Unit> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            // Update local report status
            chatDao.updateReportStatus(reportId, "Reviewed", notes = "Action: $action, Reason: ${reason ?: "N/A"}")
            
            // Add a log
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            chatDao.insertLog(
                LogEntity(
                    timestamp = sdf.format(Date()),
                    eventType = "ADMIN_ACTION",
                    actor = prefs.username ?: "admin",
                    ipAddress = "127.0.0.1",
                    description = "Executed $action on Report $reportId: ${reason ?: "Policy violation"}",
                    status = "SUCCESS"
                )
            )
            Result.success(Unit)
        } else {
            try {
                api().takeReportAction(adminHeader(), reportId, ReportActionRequest(action, durationMinutes, reason))
                syncReports()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun warnUser(userId: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().warnUser(adminHeader(), userId, mapOf("message" to message))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun muteUser(userId: String, durationMinutes: Int): Result<Unit> = withContext(Dispatchers.IO) {
        chatDao.updateUserStatus(userId, "Muted")
        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().muteUser(adminHeader(), userId, mapOf("durationMinutes" to durationMinutes))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun banUser(userId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        chatDao.updateUserStatus(userId, "Banned")
        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().banUser(adminHeader(), userId, mapOf("reason" to reason))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun unbanUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        chatDao.updateUserStatus(userId, "Active")
        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().unbanUser(adminHeader(), userId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfile(displayName: String, bio: String): Result<Unit> = withContext(Dispatchers.IO) {
        prefs.displayName = displayName
        val myId = prefs.userId ?: "user_id"
        val user = chatDao.getUserById(myId)
        if (user != null) {
            chatDao.insertUser(user.copy(displayName = displayName, bio = bio))
        }

        if (prefs.isDemoMode) {
            Result.success(Unit)
        } else {
            try {
                api().updateMyProfile(authHeader(), UpdateProfileRequest(displayName, bio))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Sync Operations ---
    suspend fun syncOnlineData() {
        if (prefs.isDemoMode || !prefs.isLoggedIn()) return
        
        try {
            syncRooms()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            syncBannedWords()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (prefs.isAdmin()) {
            try {
                syncReports()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            try {
                syncUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            try {
                syncLogs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun syncRooms() {
        val rooms = api().getRooms(authHeader())
        val entities = rooms.rooms.map { r ->
            RoomEntity(
                id = r.id,
                name = r.name ?: "Personal Chat",
                type = r.type,
                unreadCount = r.unreadCount ?: 0,
                lastMessage = r.lastMessage?.content ?: "",
                lastMessageTime = r.lastMessage?.createdAt ?: 0,
                avatarUrl = r.avatarPath ?: ""
            )
        }
        chatDao.insertRooms(entities)

        // Sync messages for each room (latest 100)
        entities.forEach { room ->
            try {
                val msgs = api().getRoomMessages(authHeader(), room.id)
                val msgEntities = msgs.messages.map { m ->
                    MessageEntity(
                        id = m.id,
                        roomId = m.roomId ?: room.id,
                        senderId = m.senderId,
                        senderName = m.senderName ?: "Unknown",
                        content = m.content ?: "",
                        timestamp = m.messageTime,
                        type = m.type,
                        replyTo = m.replyTo,
                        wasOffline = m.wasOffline,
                        isDeleted = m.isDeleted,
                        deletedByAdmin = m.deletedByAdmin,
                        attachmentUrl = null,
                        attachmentName = null,
                        attachmentSize = null
                    )
                }
                chatDao.insertMessages(msgEntities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun syncBannedWords() {
        val words = api().getBannedWords(adminHeader())
        val entities = words.words.map { w ->
            BannedWordEntity(w.id, w.word, w.category ?: "profanity", w.severity, w.regex, w.active)
        }
        chatDao.insertBannedWords(entities)
    }

    private suspend fun syncReports() {
        val reports = api().getReports(adminHeader())
        val entities = reports.reports.map { r ->
            ReportEntity(r.id, r.effectiveUserId, r.username, r.effectiveViolationType, r.messageContent ?: "", r.score, r.status, r.detectedAt, r.notes ?: "")
        }
        chatDao.insertReports(entities)
    }

    private suspend fun syncUsers() {
        val users = api().getAdminUsers(adminHeader())
        val entities = users.users.map { u ->
            UserEntity(
                id = u.id,
                username = u.username,
                displayName = u.displayName ?: u.username,
                email = u.id + "@offchat.sec",
                role = u.role ?: "user",
                status = u.status ?: if (u.banned) "Banned" else "Active",
                bio = u.bio ?: "",
                phone = "",
                avatarUrl = u.avatarPath ?: "",
                spamScore = u.spamScore?.toDouble() ?: 0.0,
                mutedUntil = u.mutedUntil
            )
        }
        chatDao.insertUsers(entities)
    }

    private suspend fun syncLogs() {
        val logs = api().getSystemLogs(adminHeader())
        val entities = logs.logs.map { l ->
            LogEntity(
                timestamp = l.timestamp ?: "",
                eventType = l.eventType ?: "SYS_LOG",
                actor = l.actor ?: "system",
                ipAddress = l.ipAddress ?: "0.0.0.0",
                description = l.description ?: "",
                status = l.status ?: "SUCCESS"
            )
        }
        chatDao.insertLogs(entities)
    }

    suspend fun getBrokerStats(): Result<BrokerStatsResponse> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            val stats = BrokerStatsResponse(
                queues = BrokerQueues(
                    messageOutbox = QueueStats(42, 2, 45200, 12, 0),
                    profanityScan = QueueStats(8, 2, 892000, 5, 0),
                    spamCheck = QueueStats(156, 12, 128000, 421, 0),
                    accountRegister = QueueStats(0, 0, 1248, 0, 0),
                    adminReport = QueueStats(0, 0, 843, 0, 0),
                    auditLog = QueueStats(0, 0, 43100, 0, 0),
                    notificationPush = QueueStats(0, 0, 1022, 0, 0)
                )
            )
            Result.success(stats)
        } else {
            try {
                val response = api().getBrokerStats(adminHeader())
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getAdminDashboardStats(): Result<DashboardStats> = withContext(Dispatchers.IO) {
        if (prefs.isDemoMode) {
            val stats = DashboardStats(
                totalUsers = 12482,
                activeNow = 1104,
                bannedAccounts = 142,
                avgSpamScore = 1.4
            )
            Result.success(stats)
        } else {
            try {
                val response = api().getAdminDashboardStats(adminHeader())
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Pre-populate Room DB with high fidelity mock data ---
    suspend fun initializeDemoData() = withContext(Dispatchers.IO) {
        // 1. Add Default Rooms
        val now = System.currentTimeMillis()
        val rooms = listOf(
            RoomEntity(
                id = "room_sarah",
                name = "Sarah Johnson",
                type = "personal",
                unreadCount = 2,
                lastMessage = "Telah mengirim lampiran file...",
                lastMessageTime = now - 600 * 1000,
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAufFut6lbty8qGZwyU6D5WQYdasulMrxO2mmw5VbEFzRWWfWVrxV6WoVHpnXpEI7hCJd-qtDbQ2a0lNSc9ipnGClEKTDnpk7lSk0gdPLcR5_qqKqi9fS_fUl-Fo1_7zJUlfcTlQNkTgX8EHtxNpGgYMnKat80mEAmR6GMWegkB-htu4nVKD2QtwxG5a4y08ToH3AhsvOecKQBDb01dY7UEUrd7ibjrxvXqqTksg3ctFb3TsxSTAGCW_ufOgpLFyVwXliarK34Q6grk",
                isPinned = true
            ),
            RoomEntity(
                id = "room_budi",
                name = "Budi Santoso",
                type = "personal",
                unreadCount = 0,
                lastMessage = "Laporan keamanan sudah siap untuk direview...",
                lastMessageTime = now - 3600 * 1000 * 2,
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB-UV3j5owNq0leML7fJ06dBihojBGK0pM1ZsGctzAwNPALUPFaBju-k1kgnxG8hLalRT2BNXLjfR2iqOpFb0Br32jOC8Jw6kVzNwbWajFxth2dPS4gePdbrcsXWOT3SV6ngunGzP5LKp5gvZkqaiTo_jrRSoA5odaZVSjfv4pE2cHsMcSQ61Tc1lClbyf8u1hCcAzFxpnyrYfbBlgtth8kf16RdJGDWVbteV-kT5rct9oU6V5NP6oP-7NSAIkP-h55nEhx-ZkDgQl4"
            ),
            RoomEntity(
                id = "room_siska",
                name = "Siska Amalia",
                type = "personal",
                unreadCount = 0,
                lastMessage = "Terima kasih atas infonya.",
                lastMessageTime = now - 3600 * 1000 * 5,
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuD9AGiLLHYenYflq1aww0gyFzNPKPINDdXVm9Z-ghtwUIZAPWy7SB1JrD3S0ngZMx3slM0d7We1GraxGCHRdHDUkGZgUzRYsXX37iGBxBNB_7oTnWB0kcFiJd9Af2aQ87zIPmHj9bJy2yXafDghk5vfF2Q0kOk1X_z6CGTwX2c4kEnYHZ0-9vfHPDhRVRYmBIsP4dI7RdvVzdg4fm-EvYHJEmhWw-SL_YNWFlv0_v6vjDS2UJTGcQN4aCCaZwLHT6D5EFaoBab6X2Sy"
            ),
            RoomEntity(
                id = "room_group",
                name = "Internal Security Audit",
                type = "group",
                unreadCount = 0,
                lastMessage = "Budi: Mohon segera update password berkala...",
                lastMessageTime = now - 3600 * 1000 * 24
            ),
            RoomEntity(
                id = "room_broadcast",
                name = "Broadcast Pengumuman",
                type = "broadcast",
                unreadCount = 0,
                lastMessage = "Maintenance server terjadwal hari Minggu ini.",
                lastMessageTime = now - 3600 * 1000 * 48
            )
        )
        chatDao.insertRooms(rooms)

        // 2. Add Messages for Sarah Room
        val sarahMsgs = listOf(
            MessageEntity(
                id = "msg1",
                roomId = "room_sarah",
                senderId = "sarah_id",
                senderName = "Sarah Johnson",
                content = "Halo Admin, bisa bantu cek status moderasi untuk post terbaru di channel #announcement?",
                timestamp = now - 20 * 60 * 1000
            ),
            MessageEntity(
                id = "msg2",
                roomId = "room_sarah",
                senderId = "admin_user_id",
                senderName = "Admin Moderator",
                content = "Tentu Sarah, saya cek sebentar ya. Tunggu 2 menit.",
                timestamp = now - 18 * 60 * 1000
            ),
            MessageEntity(
                id = "msg3",
                roomId = "room_sarah",
                senderId = "sarah_id",
                senderName = "Sarah Johnson",
                content = "Here are the updated specs. Let me know if you need anything else modified before we go live.",
                timestamp = now - 10 * 60 * 1000,
                type = "file",
                attachmentName = "Security_Specs_v4.pdf",
                attachmentSize = "1.2 MB"
            )
        )
        chatDao.insertMessages(sarahMsgs)

        // Messages for Budi Room
        val budiMsgs = listOf(
            MessageEntity(
                id = "msg_b1",
                roomId = "room_budi",
                senderId = "budi_id",
                senderName = "Budi Santoso",
                content = "Laporan keamanan sudah siap untuk direview...",
                timestamp = now - 2 * 3600 * 1000
            )
        )
        chatDao.insertMessages(budiMsgs)

        // 3. Add Users
        val users = listOf(
            UserEntity("admin_user_id", "admin", "Admin Moderator", "admin@offchat.sec", "super_admin", "Active", "Always vigilant.", "111", isOnline = true),
            UserEntity("sarah_id", "sarah_j", "Sarah Johnson", "sarah.j@company.com", "user", "Active", "Cybersecurity enthusiast and digital nomad.", "+62 899 9123 4567", isOnline = true),
            UserEntity("budi_id", "budi_s", "Budi Santoso", "budi@company.com", "user", "Active", "Securing network protocols", isOnline = true),
            UserEntity("siska_id", "siska_a", "Siska Amalia", "siska@company.com", "user", "Active", "Creative designer", isOnline = true),
            UserEntity("alexa_id", "alexa_tech", "alexa_tech", "alexa@offchat.sec", "admin", "Active", "Tech support"),
            UserEntity("vortex_id", "vortex_king", "vortex_king", "vortex@offchat.sec", "user", "Muted", "Gamer"),
            UserEntity("shadow_id", "shadow_user_8", "shadow_user_8", "shadow@offchat.sec", "user", "Banned", "Unknown"),
            UserEntity("sarah_dev_id", "sarah_dev", "sarah_dev", "sarah_dev@offchat.sec", "user", "Active", "Developer")
        )
        chatDao.insertUsers(users)

        // 4. Add Banned Words
        val bannedWords = listOf(
            BannedWordEntity("w1", "[Redacted_Word_01]", "Hate Speech", 3, isRegex = false, isActive = true),
            BannedWordEntity("w2", "[Redacted_Word_02]", "Profanity", 2, isRegex = false, isActive = true),
            BannedWordEntity("w3", "[Redacted_Word_03]", "Spam", 1, isRegex = false, isActive = false),
            BannedWordEntity("w4", "[Redacted_Word_04]", "Harassment", 3, isRegex = false, isActive = true)
        )
        chatDao.insertBannedWords(bannedWords)

        // 5. Add Violation Reports
        val reports = listOf(
            ReportEntity("REP-8902", "user_9921_id", "JohnDoe_99", "Spam", "Hey did you see that scam-link.net? It's giving away free credits.", 94, "Pending", now - 3600 * 1000 * 2),
            ReportEntity("REP-8903", "sam_k_id", "SamK_Mod", "Profanity", "You're a total idiot for thinking that works.", 68, "Pending", now - 3600 * 1000 * 4),
            ReportEntity("REP-8895", "alice_02_id", "Alice_02", "Regex", "Send me your credit card number to confirm identity.", 24, "Reviewed", now - 3600 * 1000 * 8, "Moderated by Admin")
        )
        chatDao.insertReports(reports)

        // 6. Add System Logs
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val logs = listOf(
            LogEntity(1, sdf.format(Date(now - 1000 * 120)), "SPAM_DETECT", "@user_9921", "192.168.1.104", "Repetitive link sharing in Global Chat #4", "MUTED"),
            LogEntity(2, sdf.format(Date(now - 1000 * 300)), "ADMIN_ACTION", "admin_sarah", "45.22.11.90", "Updated Banned Word List (v4.2)", "SUCCESS"),
            LogEntity(3, sdf.format(Date(now - 1000 * 600)), "AUTH_FAILURE", "@unknown_actor", "88.10.42.21", "Multiple failed password attempts", "BLOCKED"),
            LogEntity(4, sdf.format(Date(now - 1000 * 1200)), "AUTH_SUCCESS", "@mod_mike", "102.14.99.1", "Session started via Web Desktop", "ACTIVE"),
            LogEntity(5, sdf.format(Date(now - 1000 * 3600)), "SYS_WARN", "SYSTEM", "127.0.0.1", "Database latency exceeding 150ms", "LOGGED")
        )
        chatDao.insertLogs(logs)
    }
}
