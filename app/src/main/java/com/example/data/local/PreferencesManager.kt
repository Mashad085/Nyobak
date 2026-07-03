package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("offchat_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_IS_DEMO_MODE = "is_demo_mode"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ADMIN_TOKEN = "admin_token"
        
        // Security Settings
        private const val KEY_AUTO_DELETE = "auto_delete" // "Off", "24h", "7d", "30d"
        private const val KEY_HIDE_ONLINE = "hide_online"
        private const val KEY_READ_RECEIPTS = "read_receipts"
        private const val KEY_ENHANCED_PROTECTION = "enhanced_protection"
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "https://chatapp.ljngresik.app/") ?: "https://chatapp.ljngresik.app/"
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var isDemoMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DEMO_MODE, true) // Start in demo mode by default for instant review!
        set(value) = prefs.edit().putBoolean(KEY_IS_DEMO_MODE, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var displayName: String?
        get() = prefs.getString(KEY_DISPLAY_NAME, null)
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value).apply()

    var userRole: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var adminToken: String?
        get() = prefs.getString(KEY_ADMIN_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ADMIN_TOKEN, value).apply()

    var autoDeletePreference: String
        get() = prefs.getString(KEY_AUTO_DELETE, "24h") ?: "24h" // Default 24h as per HTML UI
        set(value) = prefs.edit().putString(KEY_AUTO_DELETE, value).apply()

    var hideOnlineStatus: Boolean
        get() = prefs.getBoolean(KEY_HIDE_ONLINE, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_ONLINE, value).apply()

    var readReceiptsEnabled: Boolean
        get() = prefs.getBoolean(KEY_READ_RECEIPTS, true)
        set(value) = prefs.edit().putBoolean(KEY_READ_RECEIPTS, value).apply()

    var enhancedProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENHANCED_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_ENHANCED_PROTECTION, value).apply()

    fun isLoggedIn(): Boolean {
        return userId != null && (isDemoMode || accessToken != null)
    }

    fun isAdmin(): Boolean {
        val role = userRole ?: return false
        return role == "admin" || role == "super_admin" || role == "moderator"
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_ROLE)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ADMIN_TOKEN)
            .apply()
    }
}
