package com.location_tracker.di

import com.location_tracker.location.LocationTrackingManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun platformAppModule() = module {
    singleOf(::LocationTrackingManager)
}
