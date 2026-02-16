package com.location_tracker.data.network

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Implementação iOS de `NetworkAvailability` baseada no `NWPathMonitor`.
 *
 * Finalidade:
 * - Informar ao repositório/camada de sync se há conectividade disponível.
 * - Evitar tentativas de envio quando não há rede, permitindo que as localizações fiquem em cache
 *   para sincronização posterior.
 *
 * Como funciona:
 * - Cria um `nw_path_monitor_t` e registra um `update_handler`.
 * - A cada mudança de estado, atualiza um flag (`AtomicInt`) com 1 (online) ou 0 (offline).
 * - `isNetworkAvailable()` apenas lê esse flag (operação barata e thread-safe).
 *
 * Observações:
 * - O monitor é associado à main queue (`dispatch_get_main_queue()`).
 */
@OptIn(ExperimentalAtomicApi::class)
class NWPathMonitorNetworkAvailability : NetworkAvailability {
    private val availableFlag = AtomicInt(1)

    @OptIn(ExperimentalForeignApi::class)
    private val monitor = nw_path_monitor_create()

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val isAvailable = path != null && nw_path_get_status(path) == nw_path_status_satisfied
            availableFlag.store(if (isAvailable) 1 else 0)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    }

    override fun isNetworkAvailable(): Boolean = availableFlag.load() == 1

    fun stop() {
        nw_path_monitor_cancel(monitor)
    }
}
