package com.location_tracker.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.location_tracker.R
import com.location_tracker.api.LocationSendResponse
import com.location_tracker.entity.LocationData
import com.location_tracker.models.LocationTrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Serviço de rastreamento de localização.
 * Captura localizações a intervalos de 30 segundos e envia para a API.
 */
class LocationTrackingService : Service() {
    private val repository: LocationTrackingRepository by inject()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isTracking = false
    private var trackingId: String? = null

    /**
     * Inicializa dependências do serviço.
     *
     * Aqui:
     * - Inicializa o FusedLocationProviderClient`.
     * - Cria o canal de notificação (obrigatório para rodar em foreground no Android).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    /**
     * Entry point do Service.
     *
     * Processa as ações suportadas via Intent:
     * - ACTION_START: inicia o rastreamento para um trackingId.
     * - ACTION_STOP: interrompe o rastreamento e encerra o serviço.
     */
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getStringExtra(EXTRA_TRACKING_ID)
                if (id != null) {
                    Log.d(TAG, "Starting tracking for id: $id")
                    startTracking(id)
                } else {
                    Log.e(TAG, "Cannot start without tracking_id")
                    stopSelf()
                }
            }
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    /**
     * Este serviço não oferece binding (é usado via startForegroundService/startService).
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Cleanup ao destruir o serviço.
     *
     * Para atualizações de localização e cancela o escopo de coroutines para evitar leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }

    /**
     * Inicia o rastreamento para o trackingId informado.
     *
     * Responsabilidades:
     * - Atualizar estado interno (isTracking/trackingId).
     * - Configurar o callback de localização.
     * - Subir o serviço para foreground com notificação.
     * - Iniciar atualizações periódicas e tentar enviar a última localização imediatamente.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("ForegroundServiceType")
    private fun startTracking(id: String) {
        if (isTracking && trackingId == id) return

        if (isTracking) {
            stopLocationUpdates()
        }

        trackingId = id
        setupLocationCallback(id)

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startLocationUpdates()
        requestLastLocationAndSend(id)
        isTracking = true
    }

    /**
     * Interrompe o rastreamento em andamento e encerra o serviço.
     */
    private fun stopTracking() {
        stopLocationUpdates()
        isTracking = false
        trackingId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Configura o LocationCallback que recebe as localizações periódicas.
     *
     * Quando uma nova localização chega, delega para sendLocationToApi().
     */
    private fun setupLocationCallback(id: String) {
        locationCallback =
            object : LocationCallback() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        sendLocationToApi(location, id)
                    } else {
                        Log.d(TAG, "onLocationResult sem localização")
                    }
                }
            }
    }

    /**
     * Faz um request pontual para obter a última localização conhecida e tenta enviar imediatamente.
     *
     * Isso ajuda a não esperar o primeiro tick do requestLocationUpdates().
     */
    @SuppressLint("MissingPermission")
    private fun requestLastLocationAndSend(trackingId: String) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "getLastLocation capturou localização, enviando imediatamente")
                    sendLocationToApi(location, trackingId)
                } else {
                    Log.d(TAG, "getLastLocation retornou null")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "getLastLocation falhou: ${e.message}")
            }
    }

    /**
     * Converte o Location do Android em LocationData e envia para o repositório.
     * - O repositório decide se envia para a API ou se coloca em cache (offline/erro), retornando
     *   um LocationSendResponse.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendLocationToApi(
        location: Location,
        trackingId: String,
    ) {
        val trackedAt =
            Instant
                .now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val locationData =
            LocationData(
                id = UUID.randomUUID().toString(),
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = if (location.hasSpeed()) location.speed else null,
                heading = if (location.hasBearing()) location.bearing else null,
                altitude = if (location.hasAltitude()) location.altitude else null,
                trackedAt = trackedAt,
            )

        Log.d(
            TAG,
            "Localização: lat=${locationData.latitude}, lng=${locationData.longitude}, accuracy=${locationData.accuracy}m",
        )
        Log.d(TAG, "Enviando localização para API: $trackingId")

        serviceScope.launch {
            when (val sendResult = repository.sendLocation(locationData)) {
                is LocationSendResponse.Success -> {
                    Log.i(TAG, "Localização enviada com sucesso")
                }
                is LocationSendResponse.Cached -> {
                    Log.w(TAG, "Localização cacheada para sincronização posterior")
                    updateNotificationWithPendingCount()
                }
                is LocationSendResponse.Error -> {
                    Log.e(TAG, "Erro ao enviar localização: ${sendResult.message}")
                }
            }
        }
    }

    /**
     * Inicia a coleta de localização em intervalos fixos.
     *
     * Regras:
     * - Valida permissão antes de registrar o callback.
     * - Em caso de SecurityException, encerra o serviço.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Permissão de localização faltando")
            stopSelf()
            return
        }

        Log.d(TAG, "Iniciando atualizações de localização a cada ${LOCATION_INTERVAL_MS / 1000}s")

        val locationRequest =
            LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS)
                .setWaitForAccurateLocation(false)
                .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
            Log.i(TAG, "Atualizações de localização iniciadas com sucesso")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            stopSelf()
        }
    }

    /**
     * Para as atualizações de localização registradas no FusedLocationProviderClient.
     */
    private fun stopLocationUpdates() {
        Log.d(TAG, "Parando atualizações de localização")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Verifica se a permissão de localização fina foi concedida.
     */
    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Cria o canal de notificação usado pelo foreground service (Android O+).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Location tracking service notifications"
                setShowBadge(false)
            }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Monta a notificação do serviço.
     *
     * Se houver pendências em cache, exibe a contagem no texto.
     */
    private fun createNotification(pendingCount: Int = 0): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                launchIntent ?: Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val contentText =
            if (pendingCount > 0) {
                "Rastreando localização ($pendingCount pendentes)"
            } else {
                "Rastreando localização"
            }

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Location Tracker")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * Atualiza a notificação do serviço com a quantidade atual de localizações pendentes.
     *
     * Usado principalmente quando o envio foi cacheado (offline), para dar feedback visual.
     */
    private fun updateNotificationWithPendingCount() {
        val pendingCount = repository.getPendingLocationsCount()
        val notification = createNotification(pendingCount)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_START = "com.location_tracker.location.START"
        const val ACTION_STOP = "com.location_tracker.location.STOP"
        const val EXTRA_TRACKING_ID = "tracking_id"

        private const val CHANNEL_ID = "location_tracking_channel"
        private const val CHANNEL_NAME = "Location Tracking"
        private const val NOTIFICATION_ID = 1001

        private const val LOCATION_INTERVAL_MS = 30_000L
        private const val TAG = "LocationTracking"

        /**
         * Helper para iniciar o serviço em foreground.
         *
         * Deve ser chamado pela UI/camada de controle quando o rastreamento precisa começar.
         */
        fun start(
            context: Context,
            id: String,
        ) {
            val intent =
                Intent(context, LocationTrackingService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TRACKING_ID, id)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Helper para solicitar a parada do serviço.
         */
        fun stop(context: Context) {
            val intent =
                Intent(context, LocationTrackingService::class.java).apply {
                    action = ACTION_STOP
                }
            context.startService(intent)
        }
    }
}
