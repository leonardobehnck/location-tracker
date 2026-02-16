package com.location_tracker.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun provideHttpClientEngine(): HttpClientEngine = Darwin.create { }
