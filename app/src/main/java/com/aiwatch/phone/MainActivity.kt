package com.aiwatch.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aiwatch.phone.ui.PhoneApp
import com.aiwatch.phone.ui.theme.AiWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiWatchTheme {
                PhoneApp()
            }
        }
    }
}
