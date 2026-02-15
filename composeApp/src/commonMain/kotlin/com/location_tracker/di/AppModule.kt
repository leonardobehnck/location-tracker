package com.location_tracker.di

import com.location_tracker.RequestContentHolder
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.Module

internal expect fun platformAppModule(): Module

fun appModule(): Module =
    module {
        single { RequestContentHolder() }
    } + platformAppModule()
