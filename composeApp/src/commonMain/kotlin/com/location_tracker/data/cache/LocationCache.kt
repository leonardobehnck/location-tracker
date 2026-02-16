package com.location_tracker.data.cache

import com.location_tracker.entity.LocationData

interface LocationCache {
    fun saveLocation(location: LocationData)

    fun getPendingLocations(): List<LocationData>

    fun removeLocations(locationIds: List<String>)

    fun clearAll()

    fun getPendingCount(): Int
}
