package com.location_tracker.location

import com.location_tracker.entity.LocationData
import com.location_tracker.models.LocationTrackingRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSISO8601DateFormatter
import platform.darwin.NSObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementação iOS do rastreamento de localização usando Core Location (CLLocationManager).
 *
 * Responsabilidades:
 * - Solicitar permissão de localização (quando necessário)
 * - Iniciar/parar a captura de localizações
 * - Converter CLLocation em LocationData (model comum do KMP)
 * - Enviar as localizações via LocationTrackingRepository
 *
 * Observações:
 * - O envio pode falhar offline; o repositório decide cachear e sincronizar depois.
 * - Este manager é o equivalente iOS do `LocationTrackingManager` no Android.
 */
class LocationTrackingManager :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isoFormatter = NSISO8601DateFormatter()

    private val locationManager =
        CLLocationManager().apply {
            delegate = this@LocationTrackingManager
            desiredAccuracy = 10.0
            distanceFilter = 10.0
        }

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    var repository: LocationTrackingRepository? = null

    private var trackingId: String? = null

    /**
     * Inicia o rastreamento.
     *
     * Fluxo:
     * - Verifica permissão.
     * - Se não tiver permissão, solicita e retorna false.
     * - Caso tenha, guarda o trackingId e começa a receber updates via Core Location.
     */
    fun startTracking(trackingId: String): Boolean {
        if (!hasLocationPermission()) {
            locationManager.requestWhenInUseAuthorization()
            return false
        }
        this.trackingId = trackingId
        locationManager.startUpdatingLocation()
        _isTracking.value = true
        return true
    }

    /**
     * Para o rastreamento.
     *
     * Interrompe updates do Core Location e limpa o trackingId.
     */
    fun stopTracking() {
        locationManager.stopUpdatingLocation()
        _isTracking.value = false
        trackingId = null
    }

    /**
     * Indica se o app já tem permissão de localização no iOS.
     *
     * Considera permitido:
     * - When In Use
     * - Always
     */
    fun hasLocationPermission(): Boolean {
        val status = locationManager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }

    /**
     * Callback do Core Location.
     *
     * Recebe uma lista de localizações e processa a última.
     * Converte para `LocationData` e delega o envio ao repositório.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val repo = repository ?: return
        val lastLocation = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        val trackingId = trackingId ?: return

        val trackedAt = isoFormatter.stringFromDate(NSDate())
        val coordinate = lastLocation.coordinate
        val (latitude, longitude) = coordinate.useContents { latitude to longitude }
        val entity =
            LocationData(
                id = generateLocationId(),
                latitude = latitude,
                longitude = longitude,
                accuracy = lastLocation.horizontalAccuracy.toFloat(),
                speed = lastLocation.speed.toFloat().takeIf { it >= 0f },
                heading = lastLocation.course.toFloat().takeIf { it >= 0f },
                altitude = lastLocation.altitude,
                trackedAt = trackedAt,
            )

        scope.launch {
            // O repositório trata cenários offline/erro (cache + sync posterior).
            repo.sendLocation(entity)
        }
    }

    /**
     * Callback de erro do Core Location.
     *
     * No momento não fazemos nada além de receber o erro. Se precisar, aqui é o local para:
     * - Log
     * - Telemetria
     * - Exibir erro na UI
     */
    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        // Handle errors if needed
    }

    /**
     * Gera um ID único por amostra de localização.
     *
     * Importante:
     * - Não reutilizar trackingId como ID da localização.
     * - O cache remove localizações por LocationData.id, então ele precisa ser único.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateLocationId(): String = Uuid.random().toString()
}
