package com.location_tracker.di

import com.location_tracker.RequestContentHolder
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun platformAppModule(): Module

fun appModule(): List<Module> =
    listOf(
        module {
            single { RequestContentHolder() }
        },
        platformAppModule(),
    )
