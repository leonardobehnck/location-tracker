package com.location_tracker.models

import com.location_tracker.RequestContentHolder
import com.location_tracker.api.Api
import com.location_tracker.api.LocationSendResponse
import com.location_tracker.api.Response
import com.location_tracker.data.cache.LocationCache
import com.location_tracker.data.network.NetworkAvailability
import com.location_tracker.data.remote.dto.LocationTrackingRequestDto
import com.location_tracker.data.remote.mapper.toBatchedRequests
import com.location_tracker.data.remote.mapper.toLocationItemDto
import com.location_tracker.entity.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationTrackingRepository(
    private val api: Api,
    private val locationCache: LocationCache,
    private val networkAvailability: NetworkAvailability,
    private val requestContentHolder: RequestContentHolder,
) {
    suspend fun sendLocation(location: LocationData): LocationSendResponse =
        withContext(Dispatchers.Default) {
            val request =
                LocationTrackingRequestDto(
                    id = location.id,
                    locations = listOf(location.toLocationItemDto()),
                )

            if (!networkAvailability.isNetworkAvailable()) {
                locationCache.saveLocation(location)
                requestContentHolder.update(request.toString(), "Cached (offline)")
                return@withContext LocationSendResponse.Cached
            }

            when (val response = api.sendLocationTracking(request)) {
                is Response.Success -> {
                    requestContentHolder.update(request.toString(), "Success")
                    LocationSendResponse.Success
                }

                is Response.NoConnection -> {
                    locationCache.saveLocation(location)
                    requestContentHolder.update(request.toString(), "NoConnection")
                    LocationSendResponse.Cached
                }

                is Response.Failure -> {
                    locationCache.saveLocation(location)
                    requestContentHolder.update(request.toString(), "Failure: ${response.error}")
                    LocationSendResponse.Error(response.error)
                }
            }
        }

    suspend fun syncPendingLocations(): Int =
        withContext(Dispatchers.Default) {
            if (!networkAvailability.isNetworkAvailable()) {
                return@withContext 0
            }

            val pendingLocations = locationCache.getPendingLocations()
            if (pendingLocations.isEmpty()) {
                return@withContext 0
            }

            var totalSynced = 0
            val syncedLocationIds = mutableListOf<String>()

            val batches = pendingLocations.toBatchedRequests()

            for (batch in batches) {
                when (val response = api.sendLocationTracking(batch.request)) {
                    is Response.Success -> {
                        requestContentHolder.update(batch.request.toString(), "Success")
                        syncedLocationIds.addAll(batch.locationIds)
                        totalSynced += batch.request.locations.size
                    }

                    is Response.NoConnection -> {
                        requestContentHolder.update(batch.request.toString(), "NoConnection")
                        break
                    }

                    is Response.Failure -> {
                        requestContentHolder.update(batch.request.toString(), "Failure: ${response.error}")
                    }
                }
            }

            if (syncedLocationIds.isNotEmpty()) {
                locationCache.removeLocations(syncedLocationIds)
            }

            totalSynced
        }

    fun getPendingLocationsCount(): Int = locationCache.getPendingCount()

    fun hasPendingLocations(): Boolean = locationCache.getPendingCount() > 0
}
