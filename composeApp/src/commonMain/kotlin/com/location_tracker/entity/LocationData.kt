package com.location_tracker.entity

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
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
