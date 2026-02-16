package com.location_tracker.di

import com.location_tracker.api.Api
import com.location_tracker.api.ApiImpl
import com.location_tracker.data.cache.JsonFileLocationCacheIos
import com.location_tracker.data.cache.LocationCache
import com.location_tracker.data.network.NWPathMonitorNetworkAvailability
import com.location_tracker.data.network.NetworkAvailability
import com.location_tracker.models.LocationTrackingRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun platformAppModule() =
    module {
        single { ApiImpl(provideHttpClientEngine()) }
        single<Api> { get<ApiImpl>() }
        single<LocationCache> { JsonFileLocationCacheIos() }
        single<NetworkAvailability> { NWPathMonitorNetworkAvailability() }
        singleOf(::LocationTrackingRepository)
    }
