package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ChatViewModel,
    onLogout: () -> Unit
) {
    val isModeratorMode by viewModel.isModeratorMode.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val activeRoomId by viewModel.activeRoomId.collectAsState()
    
    var clientTab by remember { mutableStateOf(0) } // 0: Chats, 1: Contacts, 2: SafeMode, 3: Account
    var adminTab by remember { mutableStateOf(0) }  // 0: Stats, 1: Reports, 2: Banned Words, 3: Users, 4: Logs
    
    var isEditingProfile by remember { mutableStateOf(false) }

    MyApplicationTheme(darkTheme = isModeratorMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Connection & Mode Header Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isModeratorMode) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary)
                        .statusBarsPadding()
                        .padding(vertical = 6.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isDemoMode) Color(0xFFF59E0B) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDemoMode) "DEMO OFFLINE MODE" else "SERVER ONLINE MODE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Role tag
                    val role = viewModel.prefs.userRole ?: "User"
                    Text(
                        text = "ROLE: ${role.uppercase()}",
                        color = Color(0xFF86F2E4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SWAP MODE",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = { viewModel.setDemoMode(it) },
                        modifier = Modifier.scale(0.6f)
                    )
                }
            }

            // Screen Dispatcher
            if (isModeratorMode) {
                AdminDashboard(
                    viewModel = viewModel,
                    activeTab = adminTab,
                    onTabChange = { adminTab = it },
                    onExitModerator = { viewModel.setModeratorMode(false) }
                )
            } else {
                if (activeRoomId != null) {
                    ChatDetailScreen(
                        viewModel = viewModel,
                        roomId = activeRoomId!!,
                        onBack = { viewModel.setActiveRoom(null) }
                    )
                } else if (isEditingProfile) {
                    EditProfileScreen(
                        viewModel = viewModel,
                        onBack = { isEditingProfile = false }
                    )
                } else {
                    ClientDashboard(
                        viewModel = viewModel,
                        activeTab = clientTab,
                        onTabChange = { clientTab = it },
                        onEditProfile = { isEditingProfile = true },
                        onEnterModerator = { viewModel.setModeratorMode(true) },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
    }
}

// --- Client Dashboard Composable ---
@Composable
fun ClientDashboard(
    viewModel: ChatViewModel,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onEditProfile: () -> Unit,
    onEnterModerator: () -> Unit,
    onLogout: () -> Unit
) {
    val rooms by viewModel.rooms.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when(activeTab) {
                                0 -> "Percakapan"
                                1 -> "Contacts"
                                2 -> "Security"
                                else -> "Account"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        // Show moderator mode button if user has privileges or in demo mode
                        val role = viewModel.prefs.userRole ?: ""
                        if (role == "admin" || role == "super_admin" || role == "moderator" || isDemoMode) {
                            IconButton(onClick = onEnterModerator) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Moderator Mode",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(imageVector = if (activeTab == 0) Icons.Default.Forum else Icons.Outlined.Forum, contentDescription = "Messages") },
                    label = { Text("Messages", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { onTabChange(1) },
                    icon = { Icon(imageVector = if (activeTab == 1) Icons.Default.Group else Icons.Outlined.Group, contentDescription = "People") },
                    label = { Text("People", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { onTabChange(2) },
                    icon = { Icon(imageVector = if (activeTab == 2) Icons.Default.Security else Icons.Outlined.Security, contentDescription = "SafeMode") },
                    label = { Text("SafeMode", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { onTabChange(3) },
                    icon = { Icon(imageVector = if (activeTab == 3) Icons.Default.Person else Icons.Outlined.Person, contentDescription = "Account") },
                    label = { Text("Account", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> {
                    // --- Chats List Tab ---
                    // Stories Row
                    StoriesRow()

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    // Conversation List
                    if (rooms.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada percakapan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(rooms) { room ->
                                RoomItem(
                                    room = room,
                                    isActive = false,
                                    onClick = { viewModel.setActiveRoom(room.id) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
                1 -> {
                    // --- Contacts Tab ---
                    ContactsTab()
                }
                2 -> {
                    // --- SafeMode Tab ---
                    SafeModeTab(viewModel)
                }
                3 -> {
                    // --- Account Tab ---
                    AccountTab(viewModel, onEditProfile)
                }
            }
        }
    }
}

// --- Stories Row Component (OffChat spec) ---
@Composable
fun StoriesRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status self item
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Status", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text("Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }

        // Budi
        StoryItem(name = "Budi", color = Color(0xFF10B981))
        // Siska
        StoryItem(name = "Siska", color = Color(0xFF10B981))
        // Sistem
        StoryItem(name = "Sistem", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

@Composable
fun StoryItem(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(2.dp, color, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
    }
}

// --- Contacts Tab ---
@Composable
fun ContactsTab() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Quick Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clickable {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = MaterialTheme.colorScheme.secondaryContainer)
                    Text("New Contact", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clickable {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Add Group", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("New Group", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // Section A
        Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        ContactItem(name = "Aditya Pratama", bio = "Laporan keamanan sudah siap...")
        ContactItem(name = "Alice Amalia", bio = "Creative designer")

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

        // Section B
        Text("B", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        ContactItem(name = "Budi Santoso", bio = "Securing networks")

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

        // Section H
        Text("H", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        ContactItem(name = "Hendrawan CTO", bio = "Rencana migrasi database...")
    }
}

@Composable
fun ContactItem(name: String, bio: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(2).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Chat, contentDescription = "Start Chat", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// --- SafeMode Tab ---
@Composable
fun SafeModeTab(viewModel: ChatViewModel) {
    var enhancedProtection by remember { mutableStateOf(viewModel.prefs.enhancedProtectionEnabled) }
    var autoDelete by remember { mutableStateOf(viewModel.prefs.autoDeletePreference) }
    var hideOnline by remember { mutableStateOf(viewModel.prefs.hideOnlineStatus) }
    var readReceipts by remember { mutableStateOf(viewModel.prefs.readReceiptsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EnhancedEncryption,
                    contentDescription = "Encryption",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("End-to-End Encryption", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Pesan & panggilan diamankan penuh dengan standar 256-bit AES.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Switches Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Enhanced
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enhanced Protection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Verifikasi metadata ekstra untuk melindungi data sensitif.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = enhancedProtection,
                        onCheckedChange = {
                            enhancedProtection = it
                            viewModel.prefs.enhancedProtectionEnabled = it
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                // Hide online status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sembunyikan Status Online", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Cegah orang melihat kapan Anda aktif.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hideOnline,
                        onCheckedChange = {
                            hideOnline = it
                            viewModel.prefs.hideOnlineStatus = it
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                // Read Receipts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Laporan Dibaca (Read Receipts)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Kirim dan terima tanda centang biru.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = readReceipts,
                        onCheckedChange = {
                            readReceipts = it
                            viewModel.prefs.readReceiptsEnabled = it
                        }
                    )
                }
            }
        }

        // Auto Delete Card Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Retensi Pesan (Auto-Delete)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Secara otomatis menghapus percakapan setelah jangka waktu tertentu.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Off", "24h", "7d", "30d").forEach { option ->
                        val isSelected = autoDelete == option
                        OutlinedButton(
                            onClick = {
                                autoDelete = option
                                viewModel.prefs.autoDeletePreference = option
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Device Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, contentDescription = "Devices", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Sessions (Devices)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                DeviceItem(name = "Android Device (This Phone)", meta = "Jakarta, Indonesia • Online Now", isCurrent = true)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                DeviceItem(name = "MacBook Pro M2", meta = "Singapore • 2 hours ago", isCurrent = false)
            }
        }
    }
}

@Composable
fun DeviceItem(name: String, meta: String, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCurrent) Icons.Default.Smartphone else Icons.Default.DesktopWindows,
            contentDescription = "Device",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("ACTIVE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- Account Tab ---
@Composable
fun AccountTab(viewModel: ChatViewModel, onEditProfile: () -> Unit) {
    val profile by viewModel.myProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (profile?.displayName ?: "U").take(2).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = profile?.displayName ?: "User",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "@" + (profile?.username ?: "username"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = onEditProfile,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile", fontWeight = FontWeight.Bold)
        }

        // Bio Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile?.bio ?: "No bio set.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Contact info section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Contact Information", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = "Phone", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = profile?.phone ?: "+62 ••• ••••", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mail, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = profile?.email ?: "e••••@offchat.sec", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        OutlinedButton(
            onClick = {},
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Block, contentDescription = "Block")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Account Permanently", fontWeight = FontWeight.Bold)
        }
    }
}

// --- Edit Profile Screen (Sub-page) ---
@Composable
fun EditProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.myProfile.collectAsState()
    var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onBack) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    Text("Edit Profil", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = {
                        viewModel.updateProfile(displayName, bio)
                        onBack()
                    }) {
                        Text("Simpan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo trigger Box
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {},
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Edit photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Text("Ubah Foto", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(12.dp))

            // Display Name Input
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nama Tampilan") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Locked Username Outlined
            OutlinedTextField(
                value = "@" + (profile?.username ?: "username"),
                onValueChange = {},
                label = { Text("Username (Locked)") },
                singleLine = true,
                enabled = false,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Locked") },
                modifier = Modifier.fillMaxWidth()
            )

            // Bio Input Textarea
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 150) bio = it },
                label = { Text("Bio") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${bio.length}/150",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// --- Chat Detail Screen ---
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    roomId: String,
    onBack: () -> Unit
) {
    val rooms by viewModel.rooms.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    
    val room = rooms.find { it.id == roomId }
    var textInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8F9FF),
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00355F))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F4C81)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (room?.name ?: "P").take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = room?.name ?: "Chat Room",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1C30)
                        )
                        Text(
                            text = "Online",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(onClick = {}) { Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF42474F)) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color(0xFF42474F)) }
                    }
                }
                HorizontalDivider(color = Color(0xFFC2C7D1).copy(alpha = 0.3f))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add attachment", tint = Color(0xFF42474F))
                }
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ketik pesan di sini...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Mood, contentDescription = "Emoji", tint = Color(0xFF42474F))
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotEmpty()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00355F))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FF))
        ) {
            // Secure Session Notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFFE5EEFF),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFC2C7D1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Secured", tint = Color(0xFF00355F), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End-to-End Encrypted Session", color = Color(0xFF00355F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Message list
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Kirim pesan untuk memulai percakapan", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            currentUserId = myProfile?.id ?: "user_id"
                        )
                    }
                }
            }
        }
    }
}

// --- Admin Dashboard View ---
@Composable
fun AdminDashboard(
    viewModel: ChatViewModel,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onExitModerator: () -> Unit
) {
    val stats by viewModel.adminStats.collectAsState()
    val brokerStats by viewModel.brokerStats.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val bannedWords by viewModel.bannedWords.collectAsState()
    val logs by viewModel.systemLogs.collectAsState()
    val users by viewModel.users.collectAsState()

    Scaffold(
        containerColor = Color(0xFF070F1E),
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F2135))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("OffChat Admin", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Control Panel Dashboard", fontSize = 11.sp, color = Color(0xFF86F2E4), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onExitModerator,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = Color(0xFF1E2E42))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F2135),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Stats") },
                    label = { Text("Stats", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00355F),
                        selectedTextColor = Color(0xFF86F2E4),
                        indicatorColor = Color(0xFF86F2E4),
                        unselectedIconColor = Color(0xFFC2C7D1),
                        unselectedTextColor = Color(0xFFC2C7D1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Default.Report, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00355F),
                        selectedTextColor = Color(0xFF86F2E4),
                        indicatorColor = Color(0xFF86F2E4),
                        unselectedIconColor = Color(0xFFC2C7D1),
                        unselectedTextColor = Color(0xFFC2C7D1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { onTabChange(2) },
                    icon = { Icon(Icons.Default.Spellcheck, contentDescription = "Words") },
                    label = { Text("Words") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00355F),
                        selectedTextColor = Color(0xFF86F2E4),
                        indicatorColor = Color(0xFF86F2E4),
                        unselectedIconColor = Color(0xFFC2C7D1),
                        unselectedTextColor = Color(0xFFC2C7D1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { onTabChange(3) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Users") },
                    label = { Text("Users") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00355F),
                        selectedTextColor = Color(0xFF86F2E4),
                        indicatorColor = Color(0xFF86F2E4),
                        unselectedIconColor = Color(0xFFC2C7D1),
                        unselectedTextColor = Color(0xFFC2C7D1)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { onTabChange(4) },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Logs") },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00355F),
                        selectedTextColor = Color(0xFF86F2E4),
                        indicatorColor = Color(0xFF86F2E4),
                        unselectedIconColor = Color(0xFFC2C7D1),
                        unselectedTextColor = Color(0xFFC2C7D1)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF070F1E))
        ) {
            when (activeTab) {
                0 -> {
                    // --- Admin Dashboard Stats Tab ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bento Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminStatCard(
                                title = "Total Users",
                                value = stats?.totalUsers?.toString() ?: "12,482",
                                icon = Icons.Default.Person,
                                statusText = "+4.2%",
                                statusColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                title = "Active Now",
                                value = stats?.activeNow?.toString() ?: "1,104",
                                icon = Icons.Default.Forum,
                                statusText = "Stable",
                                statusColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminStatCard(
                                title = "Banned",
                                value = stats?.bannedAccounts?.toString() ?: "142",
                                icon = Icons.Default.Block,
                                statusText = "High",
                                statusColor = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                title = "Avg Spam Score",
                                value = stats?.avgSpamScore?.toString() ?: "1.4",
                                icon = Icons.Default.Report,
                                statusText = "Safe",
                                statusColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // BullMQ Broker Monitor Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2135)),
                            border = BorderStroke(1.dp, Color(0xFF1E2E42))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Dns, contentDescription = "Broker", tint = Color(0xFF86F2E4))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("BullMQ Live Queue Monitor", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                QueueRow(name = "message.outbox", waiting = brokerStats?.queues?.messageOutbox?.waiting ?: 42, active = brokerStats?.queues?.messageOutbox?.active ?: 2, completed = brokerStats?.queues?.messageOutbox?.completed ?: 45200, failed = brokerStats?.queues?.messageOutbox?.failed ?: 12)
                                Divider(color = Color(0xFF1E2E42), modifier = Modifier.padding(vertical = 8.dp))
                                QueueRow(name = "spam.check", waiting = brokerStats?.queues?.spamCheck?.waiting ?: 156, active = brokerStats?.queues?.spamCheck?.active ?: 12, completed = brokerStats?.queues?.spamCheck?.completed ?: 128000, failed = brokerStats?.queues?.spamCheck?.failed ?: 421)
                                Divider(color = Color(0xFF1E2E42), modifier = Modifier.padding(vertical = 8.dp))
                                QueueRow(name = "profanity.scan", waiting = brokerStats?.queues?.profanityScan?.waiting ?: 8, active = brokerStats?.queues?.profanityScan?.active ?: 2, completed = brokerStats?.queues?.profanityScan?.completed ?: 892000, failed = brokerStats?.queues?.profanityScan?.failed ?: 5)
                            }
                        }
                    }
                }
                1 -> {
                    // --- Violation Reports Tab ---
                    if (reports.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada laporan pelanggaran.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(reports) { report ->
                                ReportItem(
                                    report = report,
                                    onAction = { action ->
                                        viewModel.resolveReport(report.id, action, "Sanksi $action dijalankan.")
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // --- Banned Words Tab ---
                    BannedWordsView(
                        bannedWords = bannedWords,
                        onAddWord = { word, category, severity ->
                            viewModel.addBannedWord(word, category, severity, false)
                        },
                        onDeleteWord = { wordId ->
                            viewModel.deleteBannedWord(wordId)
                        }
                    )
                }
                3 -> {
                    // --- Users Management Tab ---
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(users) { u ->
                            AdminUserItem(
                                user = u,
                                onWarn = { viewModel.warnUser(u.id, "Harap mematuhi aturan komunitas.") },
                                onMute = { viewModel.muteUser(u.id, 15) },
                                onBan = { viewModel.banUser(u.id, "Melanggar kebijakan keamanan.") },
                                onUnban = { viewModel.unbanUser(u.id) }
                            )
                        }
                    }
                }
                4 -> {
                    // --- Audit Logs Tab ---
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(logs) { log ->
                            AdminLogItem(log = log)
                        }
                    }
                }
            }
        }
    }
}

// --- QueueRow Component for Broker Monitor ---
@Composable
fun QueueRow(name: String, waiting: Int, active: Int, completed: Int, failed: Int) {
    Column {
        Text(text = name, color = Color(0xFF86F2E4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QueueMetric(label = "WAITING", value = waiting.toString(), color = Color.LightGray)
            QueueMetric(label = "ACTIVE", value = active.toString(), color = Color(0xFF86F2E4))
            QueueMetric(label = "COMPLETED", value = completed.toString(), color = Color(0xFF10B981))
            QueueMetric(label = "FAILED", value = failed.toString(), color = Color(0xFFEF4444))
        }
    }
}

@Composable
fun QueueMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = Color(0xFFC2C7D1), fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, color = color, fontWeight = FontWeight.ExtraBold)
    }
}

// --- Banned Words Editor View ---
@Composable
fun BannedWordsView(
    bannedWords: List<BannedWordEntity>,
    onAddWord: (String, String, Int) -> Unit,
    onDeleteWord: (String) -> Unit
) {
    var newWord by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Profanity") }
    var selectedSeverity by remember { mutableStateOf(1) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2135)),
            border = BorderStroke(1.dp, Color(0xFF1E2E42))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tambah Kata Terlarang", fontWeight = FontWeight.Bold, color = Color.White)
                
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    placeholder = { Text("Ketik kata...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Spinner simulation
                    listOf("Profanity", "Hate Speech", "Spam", "Harassment").forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF86F2E4) else Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF00355F) else Color.Transparent)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Severity:", color = Color.White, fontSize = 12.sp)
                    listOf(1, 2, 3).forEach { sev ->
                        val isSelected = selectedSeverity == sev
                        Text(
                            text = sev.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFB91C1C) else Color(0xFF1E2E42))
                                .clickable { selectedSeverity = sev }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (newWord.isNotEmpty()) {
                            onAddWord(newWord, selectedCategory, selectedSeverity)
                            newWord = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tambah Kata", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(bannedWords) { word ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2135))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = word.word, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(text = "Category: ${word.category} • Severity: ${word.severity}", color = Color.LightGray, fontSize = 11.sp)
                        }

                        IconButton(onClick = { onDeleteWord(word.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}

// --- Admin User Row ---
@Composable
fun AdminUserItem(
    user: UserEntity,
    onWarn: () -> Unit,
    onMute: () -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2135))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(user.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text("@" + user.username + " • Role: " + user.role, color = Color.LightGray, fontSize = 12.sp)
                }

                // Status Badge
                val badgeColor = when (user.status) {
                    "Banned" -> Color(0xFFEF4444)
                    "Muted" -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(user.status.uppercase(), color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (user.status == "Banned") {
                    OutlinedButton(
                        onClick = onUnban,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Unban", fontSize = 10.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onWarn,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Warn", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = onMute,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Mute 15m", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = onBan,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Ban", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// --- Admin Log Item ---
@Composable
fun AdminLogItem(log: LogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2135))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.eventType,
                    color = when (log.eventType) {
                        "SPAM_DETECT" -> Color(0xFFF59E0B)
                        "ADMIN_ACTION" -> Color(0xFF86F2E4)
                        "AUTH_FAILURE" -> Color(0xFFEF4444)
                        else -> Color(0xFF10B981)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(log.timestamp, color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(log.description, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Actor: ${log.actor} (${log.ipAddress})", color = Color.LightGray, fontSize = 11.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(log.status, color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

