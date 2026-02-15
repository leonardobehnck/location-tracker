package com.location_tracker

import androidx.compose.runtime.Composable
import com.location_tracker.di.appModule
import org.koin.compose.KoinApplication

@Composable
actual fun App() {
    KoinApplication(application = { modules(appModule()) }) {
        MainScreen()
    }
}
