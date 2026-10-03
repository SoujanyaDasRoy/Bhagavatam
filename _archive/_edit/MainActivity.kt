package com.bhagavatam.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.bhagavatam.app.data.ContentDb
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.AppNav

class MainActivity : ComponentActivity() {
    private val state: AppState by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContentDb.open(applicationContext)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(state.keepScreenOn) {
                if (state.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            AppNav(state)
        }
    }
}
