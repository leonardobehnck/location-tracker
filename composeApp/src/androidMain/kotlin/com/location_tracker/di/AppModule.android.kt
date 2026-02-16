package com.location_tracker.di

import com.location_tracker.data.cache.InMemoryLocationCache
import com.location_tracker.data.cache.LocationCache
import com.location_tracker.data.network.AndroidNetworkAvailability
import com.location_tracker.data.network.NetworkAvailability
import com.location_tracker.location.LocationTrackingManager
import com.location_tracker.models.LocationTrackingRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun platformAppModule() =
    module {
        single { com.location_tracker.api.ApiImpl(provideHttpClientEngine()) }
        single<com.location_tracker.api.Api> { get<com.location_tracker.api.ApiImpl>() }
        single<LocationCache> { InMemoryLocationCache() }
        single<NetworkAvailability> { AndroidNetworkAvailability(get()) }
        singleOf(::LocationTrackingRepository)
        singleOf(::LocationTrackingManager)
    }
