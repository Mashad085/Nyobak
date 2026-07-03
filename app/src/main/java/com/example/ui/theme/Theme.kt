package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = OffChatPrimary,
    primaryContainer = Color(0xFF1E2E42), // Outgoing message bubble (Dark navy)
    secondary = OffChatSecondary,
    secondaryContainer = OffChatSecondaryContainer, // 0xFF86F2E4 (Vigilant Mint)
    background = Color(0xFF070F1E), // Premium deep dark
    surface = Color(0xFF0F2135), // Dark surface for cards/panels
    surfaceVariant = Color(0xFF152238), // Incoming message bubble (Slate-blue dark)
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFFEAF1FF),
    onSecondary = Color.Black,
    onSecondaryContainer = Color(0xFF070F1E),
    onBackground = Color(0xFFEAF1FF),
    onSurface = Color(0xFFEAF1FF),
    onSurfaceVariant = Color(0xFFC2C7D1)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = OffChatPrimary,
    primaryContainer = Color(0xFFE0F2FE), // Outgoing message bubble (Light sky blue)
    secondary = OffChatSecondary,
    secondaryContainer = OffChatSecondaryContainer, // 0xFF86F2E4 (Mint accents)
    background = OffChatBackground, // Soft light blue-gray F8F9FF
    surface = OffChatSurface, // Pure white Surface
    surfaceVariant = Color(0xFFF1F5F9), // Incoming message bubble (Light slate gray)
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF00355F),
    onSecondary = Color.White,
    onSecondaryContainer = Color(0xFF00355F),
    onBackground = OffChatOnSurface,
    onSurface = OffChatOnSurface,
    onSurfaceVariant = OffChatOnSurfaceVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce the distinctive branded theme
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
