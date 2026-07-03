package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.ChatViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Obtain the main ChatViewModel
    val viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val isLoggedIn by viewModel.isLoggedIn.collectAsState()

          if (isLoggedIn) {
            DashboardScreen(
              viewModel = viewModel,
              onLogout = { viewModel.logout() }
            )
          } else {
            AuthScreen(
              viewModel = viewModel,
              onAuthSuccess = {
                // Done automatically by VM, state flow triggers transition
              }
            )
          }
        }
      }
    }
  }
}

