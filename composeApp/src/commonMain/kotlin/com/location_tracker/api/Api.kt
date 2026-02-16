package com.location_tracker.api

import com.location_tracker.data.remote.dto.LocationTrackingRequestDto

interface Api {
    suspend fun sendLocationTracking(request: LocationTrackingRequestDto): Response<Unit>
}
