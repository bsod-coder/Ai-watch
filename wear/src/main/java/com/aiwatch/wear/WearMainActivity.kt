package com.aiwatch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aiwatch.wear.ui.WearApp
import com.aiwatch.wear.ui.theme.WearAppTheme

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearAppTheme {
                WearApp()
            }
        }
    }
}
