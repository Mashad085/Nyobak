package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.BrokerStatsResponse
import com.example.data.api.DashboardStats
import com.example.data.local.*
import com.example.data.repository.ChatRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit

@Suppress("OPT_IN_USAGE")
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val chatDao = database.chatDao()
    val prefs = PreferencesManager(application)
    val repository = ChatRepository(chatDao, prefs)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // --- State Flows ---
    private val _isLoggedIn = MutableStateFlow(prefs.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isDemoMode = MutableStateFlow(prefs.isDemoMode)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isModeratorMode = MutableStateFlow(false)
    val isModeratorMode: StateFlow<Boolean> = _isModeratorMode.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    // Profile State
    private val _myProfile = MutableStateFlow<UserEntity?>(null)
    val myProfile: StateFlow<UserEntity?> = _myProfile.asStateFlow()

    // Loading / Error States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Live Flows from Local Cache DB
    val rooms: StateFlow<List<RoomEntity>> = repository.allRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bannedWords: StateFlow<List<BannedWordEntity>> = repository.allBannedWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemLogs: StateFlow<List<LogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Specific Live Data
    private val _adminStats = MutableStateFlow<DashboardStats?>(null)
    val adminStats: StateFlow<DashboardStats?> = _adminStats.asStateFlow()

    private val _brokerStats = MutableStateFlow<BrokerStatsResponse?>(null)
    val brokerStats: StateFlow<BrokerStatsResponse?> = _brokerStats.asStateFlow()

    // WebSocket state
    private var webSocket: WebSocket? = null
    private val _isWsConnected = MutableStateFlow(false)
    val isWsConnected: StateFlow<Boolean> = _isWsConnected.asStateFlow()

    init {
        // Automatically fetch current profile and sync if logged in
        viewModelScope.launch {
            if (prefs.isLoggedIn()) {
                loadMyProfile()
                syncData()
                if (!prefs.isDemoMode) {
                    connectWebSocket()
                }
            }
        }
    }

    // Toggle between Offline Demo and Live Server
    fun setDemoMode(enabled: Boolean) {
        prefs.isDemoMode = enabled
        _isDemoMode.value = enabled
        if (enabled) {
            disconnectWebSocket()
            viewModelScope.launch {
                repository.initializeDemoData()
                loadMyProfile()
            }
        } else {
            if (prefs.isLoggedIn()) {
                connectWebSocket()
                syncData()
            }
        }
    }

    fun setModeratorMode(enabled: Boolean) {
        _isModeratorMode.value = enabled
        if (enabled) {
            refreshAdminStats()
        }
    }

    fun setActiveRoom(roomId: String?) {
        _activeRoomId.value = roomId
        if (roomId != null) {
            viewModelScope.launch {
                chatDao.clearRoomUnreadCount(roomId)
            }
        }
    }

    // Observe active chat messages
    val activeMessages: StateFlow<List<MessageEntity>> = _activeRoomId
        .flatMapLatest { roomId ->
            if (roomId == null) flowOf(emptyList()) else repository.getRoomMessages(roomId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication Actions ---
    fun login(username: String, secret: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.login(username, secret)
            _isLoading.value = false
            if (result.isSuccess) {
                _isLoggedIn.value = true
                loadMyProfile()
                if (!prefs.isDemoMode) {
                    connectWebSocket()
                }
                onResult(true)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Login Gagal"
                onResult(false)
            }
        }
    }

    fun register(username: String, secret: String, displayName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.register(username, secret, displayName)
            _isLoading.value = false
            if (result.isSuccess) {
                _isLoggedIn.value = true
                loadMyProfile()
                if (!prefs.isDemoMode) {
                    connectWebSocket()
                }
                onResult(true)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Registrasi Gagal"
                onResult(false)
            }
        }
    }

    fun logout() {
        disconnectWebSocket()
        prefs.logout()
        _isLoggedIn.value = false
        _isModeratorMode.value = false
        _activeRoomId.value = null
        viewModelScope.launch {
            chatDao.clearAllMessages()
        }
    }

    private suspend fun loadMyProfile() {
        val myId = prefs.userId ?: return
        val localProfile = chatDao.getUserById(myId)
        if (localProfile != null) {
            _myProfile.value = localProfile
        } else {
            // If in demo mode and no self found, write default
            if (prefs.isDemoMode) {
                val dummyUser = UserEntity(
                    id = myId,
                    username = prefs.username ?: "admin",
                    displayName = prefs.displayName ?: "Admin Moderator",
                    email = "admin@offchat.sec",
                    role = "super_admin",
                    status = "Active",
                    bio = "Vigilant explorer. Crypto enthusiast. Always secure.",
                    phone = "+62 812 3456 7890",
                    avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCDqcWul243t5wZ7TXEP7SFf9tcbHWeYvqFYrRyVu6hu6FYy3QIjwpG7sZ1NtrhaAkcBMWc2E_DI-RZPSXoni-2GU-XjnQC-CyA-7qCqi9unMXKpDtkwv-lK-3L409lPCXO4XK1R24AEw1yThvyMjdqlXXmvnDAb_ehU6X674o0P99A0Qdsb-TJ3YlGUZeJzUUE_emRWyuf3uOW8cFJ_lXMSe7v_lRBZJVs7e7a_S3nbKvyxzZz_qNPQllow0IeQE625ivvEXRH18wn"
                )
                chatDao.insertUser(dummyUser)
                _myProfile.value = dummyUser
            }
        }
    }

    // --- Message & Profile Actions ---
    fun sendMessage(content: String, type: String = "text") {
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            if (!prefs.isDemoMode && _isWsConnected.value) {
                // If connected, send via WS
                sendWebSocketMessage(roomId, content, type)
            } else {
                // Otherwise save locally (Offline/Demo Mode)
                repository.sendMessage(roomId, content, type)
            }
        }
    }

    fun updateProfile(displayName: String, bio: String) {
        viewModelScope.launch {
            val result = repository.updateProfile(displayName, bio)
            if (result.isSuccess) {
                loadMyProfile()
            }
        }
    }

    // --- Admin Actions ---
    fun addBannedWord(word: String, category: String, severity: Int, isRegex: Boolean) {
        viewModelScope.launch {
            repository.addBannedWord(word, category, severity, isRegex)
        }
    }

    fun deleteBannedWord(id: String) {
        viewModelScope.launch {
            repository.deleteBannedWord(id)
        }
    }

    fun resolveReport(reportId: String, action: String, reason: String? = null) {
        viewModelScope.launch {
            repository.takeReportAction(reportId, action, null, reason)
            refreshAdminStats()
        }
    }

    fun warnUser(userId: String, message: String) {
        viewModelScope.launch {
            repository.warnUser(userId, message)
        }
    }

    fun muteUser(userId: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.muteUser(userId, durationMinutes)
        }
    }

    fun banUser(userId: String, reason: String) {
        viewModelScope.launch {
            repository.banUser(userId, reason)
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            repository.unbanUser(userId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- Sync & Refresh ---
    fun syncData() {
        viewModelScope.launch {
            repository.syncOnlineData()
            refreshAdminStats()
        }
    }

    fun refreshAdminStats() {
        viewModelScope.launch {
            val statsRes = repository.getAdminDashboardStats()
            if (statsRes.isSuccess) {
                _adminStats.value = statsRes.getOrNull()
            }
            val brokerRes = repository.getBrokerStats()
            if (brokerRes.isSuccess) {
                _brokerStats.value = brokerRes.getOrNull()
            }
        }
    }

    // --- WebSocket Implementation (OkHttp) ---
    private fun connectWebSocket() {
        if (prefs.isDemoMode || prefs.accessToken == null) return
        disconnectWebSocket()

        val rawUrl = prefs.serverUrl
        val wsUrl = rawUrl.replace("https://", "wss://")
            .replace("http://", "ws://")
            .let { if (it.endsWith("/")) it else "$it/" } + "ws?token=${prefs.accessToken}"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isWsConnected.value = true
                viewModelScope.launch {
                    syncRoomsFromWS()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseWSMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isWsConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isWsConnected.value = false
                // Auto reconnect after 5 seconds if still logged in
                viewModelScope.launch {
                    delay(5000)
                    if (prefs.isLoggedIn() && !prefs.isDemoMode) {
                        connectWebSocket()
                    }
                }
            }
        })
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Logout")
        webSocket = null
        _isWsConnected.value = false
    }

    private fun sendWebSocketMessage(roomId: String, content: String, type: String) {
        val payload = mapOf(
            "type" to "message:send",
            "payload" to mapOf(
                "roomId" to roomId,
                "content" to content,
                "type" to type
            )
        )
        val adapter = moshi.adapter(Map::class.java)
        val jsonText = adapter.toJson(payload)
        webSocket?.send(jsonText)
    }

    private fun parseWSMessage(jsonText: String) {
        try {
            val adapter = moshi.adapter(Map::class.java)
            val data = adapter.fromJson(jsonText) ?: return
            val type = data["type"] as? String ?: return

            if (type == "message:new") {
                val payload = data["payload"] as? Map<*, *> ?: return
                viewModelScope.launch {
                    val msgId = payload["id"] as? String ?: UUID.randomUUID().toString()
                    val roomId = payload["roomId"] as? String ?: ""
                    val senderId = payload["senderId"] as? String ?: ""
                    val senderName = payload["senderName"] as? String ?: "User"
                    val content = payload["content"] as? String ?: ""
                    val timestamp = (payload["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val msgType = payload["type"] as? String ?: "text"

                    val msg = MessageEntity(
                        id = msgId,
                        roomId = roomId,
                        senderId = senderId,
                        senderName = senderName,
                        content = content,
                        timestamp = timestamp,
                        type = msgType
                    )
                    chatDao.insertMessage(msg)

                    // Also update room's last message
                    val room = chatDao.getRoomById(roomId)
                    if (room != null) {
                        chatDao.insertRoom(room.copy(
                            lastMessage = content,
                            lastMessageTime = timestamp,
                            unreadCount = if (roomId == _activeRoomId.value) 0 else room.unreadCount + 1
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncRoomsFromWS() {
        // After opening the socket, force a Room list sync to capture missed messages
        repository.syncOnlineData()
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}
