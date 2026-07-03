package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel

@Composable
fun AuthScreen(
    viewModel: ChatViewModel,
    onAuthSuccess: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(viewModel.prefs.serverUrl) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00355F),
            Color(0xFF070F1E)
        )
    )

    // Validasi terpusat, dipakai untuk enable/disable tombol.
    val trimmedUsername = username.trim()
    val trimmedServerUrl = serverUrl.trim()
    val isPasswordValid = password.length >= 8
    val isDisplayNameValid = !isRegisterMode || displayName.trim().isNotEmpty()
    val isFormValid = trimmedUsername.isNotEmpty() &&
        trimmedServerUrl.isNotEmpty() &&
        isPasswordValid &&
        isDisplayNameValid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon & Logo Header
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "OffChat Security",
                tint = Color(0xFF86F2E4),
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "OffChat",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Vigilant & Secure",
                fontSize = 14.sp,
                color = Color(0xFFC2C7D1),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Auth Card Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F2135).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Buat Akun Baru" else "Masuk Akun",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Error Notification
                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFFBA1A1A).copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFFDAD6),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Server Address (only if not in offline demo mode)
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            viewModel.prefs.serverUrl = it
                        },
                        label = { Text("Server URL", color = Color(0xFFC2C7D1)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF86F2E4),
                            unfocusedBorderColor = Color(0xFF727780),
                            cursorColor = Color(0xFF86F2E4)
                        ),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = "Server", tint = Color(0xFFC2C7D1)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", color = Color(0xFFC2C7D1)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF86F2E4),
                            unfocusedBorderColor = Color(0xFF727780),
                            cursorColor = Color(0xFF86F2E4)
                        ),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User", tint = Color(0xFFC2C7D1)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Display Name (Only in Register Mode)
                    AnimatedVisibility(visible = isRegisterMode) {
                        Column {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Nama Tampilan", color = Color(0xFFC2C7D1)) },
                                isError = isRegisterMode && displayName.isNotEmpty() && displayName.trim().isEmpty(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF86F2E4),
                                    unfocusedBorderColor = Color(0xFF727780),
                                    cursorColor = Color(0xFF86F2E4)
                                ),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "Display Name", tint = Color(0xFFC2C7D1)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Wajib diisi untuk mendaftar",
                                color = Color(0xFFC2C7D1).copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    // Password Input
                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFFC2C7D1)) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF86F2E4),
                                unfocusedBorderColor = Color(0xFF727780),
                                cursorColor = Color(0xFF86F2E4)
                            ),
                            singleLine = true,
                            isError = password.isNotEmpty() && !isPasswordValid,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFFC2C7D1)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = Color(0xFFC2C7D1)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Feedback jelas kenapa tombol bisa disabled, bukan cuma diam mati.
                        if (password.isNotEmpty() && !isPasswordValid) {
                            Text(
                                text = "Password minimal 8 karakter",
                                color = Color(0xFFFFB4AB),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    // Connect/Login Action
                    Button(
                        onClick = {
                            viewModel.setDemoMode(false)
                            if (isRegisterMode) {
                                viewModel.register(trimmedUsername, password, displayName.trim()) { success ->
                                    if (success) onAuthSuccess()
                                }
                            } else {
                                viewModel.login(trimmedUsername, password) { success ->
                                    if (success) onAuthSuccess()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F4C81),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = isFormValid && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(if (isRegisterMode) "Daftar & Hubungkan" else "Hubungkan ke Server")
                        }
                    }

                    // Toggle register/login link
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            viewModel.clearError()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar",
                            color = Color(0xFF86F2E4),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Demo Toggle
            Text(
                text = "— ATAU COBA STANDALONE —",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC2C7D1).copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    viewModel.clearError()
                    viewModel.setDemoMode(true)
                    viewModel.login("admin", "demomode") { success ->
                        if (success) onAuthSuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006A61),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(32.dp),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsAccessibility,
                    contentDescription = "Demo Mode",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Masuk Offline Demo Mode", fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Demo Mode tidak memerlukan server, instan memuat data mockup OffChat lengkap.",
                color = Color(0xFFC2C7D1).copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
            )
        }
    }
}