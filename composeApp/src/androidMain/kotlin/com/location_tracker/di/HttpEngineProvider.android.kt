package com.location_tracker.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create { }
