package com.location_tracker.data.cache

import com.location_tracker.entity.LocationData

/**
 * Cache em memória para armazenar localizações pendentes de envio no iOS.
 *
 * Uso esperado:
 * - Quando o envio falhar, o repositório pode salvar localizações como pendentes
 *   para sincronizar mais tarde.
 */
class InMemoryLocationCacheIos : LocationCache {
    private val pending = mutableListOf<LocationData>()

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
