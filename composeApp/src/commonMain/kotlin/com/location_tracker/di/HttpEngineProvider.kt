package com.location_tracker.di

import io.ktor.client.engine.HttpClientEngine

internal expect fun provideHttpClientEngine(): HttpClientEngine
