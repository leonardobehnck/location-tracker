package com.location_tracker.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.location_tracker.models.LocationTrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Classe que gerencia o serviço de rastreamento de localização.
 * Orquestra o serviço de rastreamento de localização.
 * Fornece uma API simples para iniciar/parar o rastreamento e monitora a conectividade da rede.
 */

private const val TAG = "LocationTracking"

class LocationTrackingManager(
    private val context: Context,
    private val repository: LocationTrackingRepository,
) {
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _trackingId = MutableStateFlow<String?>(null)
    val trackingId: StateFlow<String?> = _trackingId.asStateFlow()

    private val _pendingLocationsCount = MutableStateFlow(0)
    val pendingLocationsCount: StateFlow<Int> = _pendingLocationsCount.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Inicia o serviço de rastreamento de localização.
     * Requer que as permissões de localização sejam concedidas.
     *
     * @param trackingId, id para associar as localizações
     * @return true se o rastreamento foi iniciado com sucesso, false se as permissões estão faltando
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun startTracking(trackingId: String): Boolean {
        Log.d(TAG, "startTracking iniciado para: $trackingId")

        if (!hasLocationPermission()) {
            Log.w(TAG, "Permissões de localização não concedidas")
            return false
        }

        Log.d(TAG, "Permissões concedidas, iniciando LocationTrackingService")
        LocationTrackingService.Companion.start(context, trackingId)
        registerNetworkCallback()
        _isTracking.value = true
        _trackingId.value = trackingId
        updatePendingCount()

        Log.i(TAG, "Rastreamento de localização iniciado com sucesso")
        return true
    }

    /**
     * Para o serviço de rastreamento de localização.
     */
    fun stopTracking() {
        Log.d(TAG, "stopTracking iniciado")
        LocationTrackingService.Companion.stop(context)
        unregisterNetworkCallback()
        LocationSyncWorker.cancel(context)
        _isTracking.value = false
        _trackingId.value = null
        Log.i(TAG, "Rastreamento de localização parado")
    }

    /**
     * Verifica se as permissões de localização foram concedidas.
     */
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Atualiza a contagem de localizações pendentes.
     */
    fun updatePendingCount() {
        _pendingLocationsCount.value = repository.getPendingLocationsCount()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun registerNetworkCallback() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

        networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (repository.hasPendingLocations()) {
                        LocationSyncWorker.schedule(context)
                    }
                }

                override fun onLost(network: Network) {
                    updatePendingCount()
                }
            }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let { callback ->
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: IllegalArgumentException) {
                // Callback was not registered
            }
            networkCallback = null
        }
    }
}
