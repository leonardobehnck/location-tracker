package com.location_tracker.background

import com.location_tracker.di.KoinInitializer
import com.location_tracker.models.LocationTrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ponto de entrada (Kotlin) para sincronização de localizações pendentes em background no iOS.
 *
 * Contexto:
 * - O iOS dispara tarefas de background via BGAppRefreshTask (lado Swift).
 * - O Swift precisa chamar uma API simples e “Swift-friendly”. Este objeto expõe essa API.
 *
 * Como funciona:
 * - Inicializa/acessa o Koin via KoinInitializer.getKoin().
 * - Resolve o LocationTrackingRepository.
 * - Executa syncPendingLocations() em coroutine.
 * - Ao terminar, chama onComplete com a quantidade de localizações sincronizadas.
 */
object BackgroundSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Dispara a sincronização das localizações pendentes.
     *
     * Observações:
     * - A execução é assíncrona.
     * - onComplete é chamado quando a sincronização termina (com sucesso ou retornando 0).
     * - O repositório respeita o NetworkAvailability e o LocationCache configurados na DI.
     */
    fun syncPendingLocations(onComplete: (syncedCount: Int) -> Unit) {
        val koin = KoinInitializer.getKoin()
        val repository = koin.get<LocationTrackingRepository>()

        scope.launch {
            val synced = repository.syncPendingLocations()
            onComplete(synced)
        }
    }
}
