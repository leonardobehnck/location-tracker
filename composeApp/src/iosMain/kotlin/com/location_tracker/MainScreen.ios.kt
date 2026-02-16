package com.location_tracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.location_tracker.location.LocationTrackingManager
import com.location_tracker.models.LocationTrackingRepository
import org.koin.compose.koinInject

private const val TAG = "LocationTracker"
private const val ID = "id"

@Composable
actual fun MainScreen() {
    val locationTrackingManager = remember { LocationTrackingManager() }
    LocationTracker(locationTrackingManager = locationTrackingManager)
}

@Composable
fun LocationTracker(
    locationTrackingManager: LocationTrackingManager,
    requestContentHolder: RequestContentHolder = koinInject(),
    locationTrackingRepository: LocationTrackingRepository = koinInject(),
) {
    LaunchedEffect(locationTrackingRepository) {
        locationTrackingManager.repository = locationTrackingRepository
    }

    val hasPermission = locationTrackingManager.hasLocationPermission()

    LocationTrackerScreen(
        isTracking = locationTrackingManager.isTracking,
        requestContent = requestContentHolder.content,
        hasPermission = hasPermission,
        onRequestPermission = {
            locationTrackingManager.startTracking(ID)
        },
        onStartTracking = {
            locationTrackingManager.startTracking(ID)
        },
        onStopTracking = {
            locationTrackingManager.stopTracking()
        },
        onPermissionMissing = {
            locationTrackingManager.startTracking(ID)
        },
        onShowMessage = { message ->
            println("$TAG: $message")
        },
    )
}
