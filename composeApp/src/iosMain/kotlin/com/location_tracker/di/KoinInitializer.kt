package com.location_tracker.di

import org.koin.core.Koin
import org.koin.core.context.startKoin

/**
 * Este inicializador deve ser chamado de Swift (iOSApp.init) ANTES de qualquer código Compose/UI.
 */
object KoinInitializer {
    private var started = false
    private var koin: Koin? = null

    fun setupKoin() {
        if (started) return
        started = true
        koin =
            startKoin {
                modules(appModule())
            }.koin
    }

    fun getKoin(): Koin {
        setupKoin()
        return requireNotNull(koin)
    }
}
