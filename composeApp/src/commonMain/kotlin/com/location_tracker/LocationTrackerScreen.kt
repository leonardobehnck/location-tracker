package com.location_tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LocationTrackerScreen(
    isTracking: StateFlow<Boolean>,
    requestContent: StateFlow<String>,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onPermissionMissing: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val tracking by isTracking.collectAsState()
    val request by requestContent.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RequestLocationPermissionButton(
            hasPermission = hasPermission,
            onRequestPermission = {
                onShowMessage("Solicitando permissão de localização")
                onRequestPermission()
            },
        )

        TrackingButton(
            isTracking = tracking,
            hasPermission = hasPermission,
            onStartTracking = {
                onStartTracking()
                onShowMessage("Rastreamento iniciado")
            },
            onStopTracking = {
                onStopTracking()
                onShowMessage("Rastreamento parado")
            },
            onPermissionMissing = {
                onShowMessage("Solicite a permissão de localização primeiro")
                onPermissionMissing()
            },
        )

        Text(
            text = request.ifEmpty { "Nenhuma request ainda. Inicie o rastreamento e aguarde a primeira localização (~30s)." },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = if (request.isEmpty()) Color.Gray else Color.DarkGray,
        )
    }
}

@Composable
private fun RequestLocationPermissionButton(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    OutlinedButton(
        onClick = onRequestPermission,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        enabled = !hasPermission,
    ) {
        Text(
            if (hasPermission) "Permissão de localização concedida" else "Solicitar permissão de localização",
        )
    }
}

@Composable
private fun TrackingButton(
    isTracking: Boolean,
    hasPermission: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onPermissionMissing: () -> Unit,
) {
    Button(
        onClick = {
            if (isTracking) {
                onStopTracking()
                return@Button
            }

            when {
                hasPermission -> onStartTracking()
                else -> onPermissionMissing()
            }
        },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color.Red else Color(0xFF4CAF50),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isTracking) "Parar rastreamento" else "Iniciar rastreamento")
    }
}
