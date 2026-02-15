package com.location_tracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.location_tracker.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication

@Composable
actual fun App() {
    val context = LocalContext.current.applicationContext
    KoinApplication(application = {
        androidContext(context)
        modules(appModule())
    }) {
        MainScreen()
    }
}
