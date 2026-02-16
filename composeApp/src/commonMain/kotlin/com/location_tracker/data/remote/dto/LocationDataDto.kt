package com.location_tracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationDataDto(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float? = null,
    val heading: Float? = null,
    val altitude: Double? = null,
    val trackedAt: String,
    val isSynced: Boolean = false,
)
