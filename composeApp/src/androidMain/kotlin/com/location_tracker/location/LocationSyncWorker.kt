package com.location_tracker.location

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.location_tracker.models.LocationTrackingRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker que sincroniza localizações pendentes quando a rede estiver disponível.
 * Isso garante que localizações capturadas offline sejam enviadas à API.
 */
class LocationSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val repository: LocationTrackingRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            if (!repository.hasPendingLocations()) {
                println("[LocationSync] No pending locations to sync")
                return Result.success()
            }

            val syncedCount = repository.syncPendingLocations()
            println("[LocationSync] Synced $syncedCount locations")

            if (repository.hasPendingLocations()) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            println("[LocationSync] Error syncing locations: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "location_sync_work"

        fun schedule(context: Context) {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val workRequest =
                OneTimeWorkRequestBuilder<LocationSyncWorker>()
                    .setConstraints(constraints)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
