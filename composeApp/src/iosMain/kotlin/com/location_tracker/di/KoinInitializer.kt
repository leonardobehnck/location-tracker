package com.location_tracker.di

import org.koin.core.context.startKoin

/**
 * Must be called from Swift (iOSApp.init) BEFORE any Compose/UI code.
 * Safe to call multiple times: startKoin runs only once.
 * Method name avoids Swift reserved words: "start" and "init*" (init prefix = initializer).
 */
object KoinInitializer {
    private var started = false

    fun setupKoin() {
        if (started) return
        started = true
        startKoin {
            modules(appModule())
        }
    }
}
