package com.location_tracker.data.cache

import com.location_tracker.entity.LocationData
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Cache em memória para armazenar localizações pendentes de envio.
 * Quando não há internet, as localizações são salvas aqui para tentar
 * sincronizar mais tarde.
 */

class InMemoryLocationCache : LocationCache {
    private val pending = CopyOnWriteArrayList<LocationData>()

    override fun saveLocation(location: LocationData) {
        pending.add(location)
    }

    override fun getPendingLocations(): List<LocationData> = pending.toList()

    override fun removeLocations(locationIds: List<String>) {
        val ids = locationIds.toSet()
        pending.removeAll { it.id in ids }
    }

    override fun clearAll() {
        pending.clear()
    }

    override fun getPendingCount(): Int = pending.size
}
