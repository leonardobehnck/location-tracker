package com.location_tracker.data.remote.mapper

import com.location_tracker.data.remote.dto.LocationItemDto
import com.location_tracker.data.remote.dto.LocationTrackingRequestDto
import com.location_tracker.entity.LocationData
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class BatchedRequest(
    val request: LocationTrackingRequestDto,
    val locationIds: List<String>,
)

private const val BATCH_SIZE = 50

fun LocationData.toLocationItemDto(): LocationItemDto =
    LocationItemDto(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        speed = speed,
        heading = heading,
        altitude = altitude,
        trackedAt = trackedAt,
    )

@OptIn(ExperimentalUuidApi::class)
fun List<LocationData>.toBatchedRequests(): List<BatchedRequest> =
    chunked(BATCH_SIZE).map { batch ->
        BatchedRequest(
            request =
                LocationTrackingRequestDto(
                    id = Uuid.random().toString(),
                    locations = batch.map { it.toLocationItemDto() },
                ),
            locationIds = batch.map { it.id },
        )
    }
