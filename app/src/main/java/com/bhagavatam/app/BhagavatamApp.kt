package com.bhagavatam.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.bhagavatam.app.state.AppState

/**
 * Holds the app state for the life of the process, not of one screen. That is what lets the voice keep reading after
 * the screen is closed or the activity is gone: the playback service and the activity both talk to this one object.
 */
class BhagavatamApp : Application() {
    private val store = ViewModelStore()
    val state: AppState by lazy { ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory.getInstance(this))[AppState::class.java] }
}
