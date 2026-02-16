package com.location_tracker

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.location_tracker.location.LocationTrackingManager
import org.koin.compose.koinInject

private const val TAG = "LocationTracker"
private const val TRACKING_ID = "id"

private val LOCATION_PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

@Composable
actual fun MainScreen() {
    LocationTracker(locationTrackingManager = koinInject())
}

@Composable
fun LocationTracker(
    locationTrackingManager: LocationTrackingManager = koinInject(),
    requestContentHolder: RequestContentHolder = koinInject(),
) {
    val context = LocalContext.current
    var hasPermission by rememberSaveable { mutableStateOf(locationTrackingManager.hasLocationPermission()) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.d(TAG, "Permissão de localização concedida")
                Toast.makeText(context, "Permissão concedida", Toast.LENGTH_SHORT).show()
                hasPermission = true
            } else {
                Log.w(TAG, "Permissão de localização negada")
                Toast.makeText(context, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
                hasPermission = locationTrackingManager.hasLocationPermission()
            }
        }

    LocationTrackerScreen(
        isTracking = locationTrackingManager.isTracking,
        requestContent = requestContentHolder.content,
        hasPermission = hasPermission,
        onRequestPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
        onStartTracking = {
            val started = locationTrackingManager.startTracking(TRACKING_ID)
            if (started) {
                Toast.makeText(context, "Rastreamento iniciado", Toast.LENGTH_SHORT).show()
            }
        },
        onStopTracking = {
            locationTrackingManager.stopTracking()
            Toast.makeText(context, "Rastreamento parado", Toast.LENGTH_SHORT).show()
        },
        onPermissionMissing = {
            Toast
                .makeText(
                    context,
                    "Solicite a permissão de localização primeiro",
                    Toast.LENGTH_SHORT,
                ).show()
        },
        onShowMessage = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )
}
