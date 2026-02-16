package com.location_tracker

import androidx.compose.ui.window.ComposeUIViewController
import com.location_tracker.di.KoinInitializer

fun MainViewController() =
    ComposeUIViewController {
        KoinInitializer.setupKoin()
        App()
    }
