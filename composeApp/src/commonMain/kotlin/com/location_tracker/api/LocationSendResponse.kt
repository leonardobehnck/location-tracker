package com.location_tracker.api

sealed class LocationSendResponse {
    data object Success : LocationSendResponse()

    data object Cached : LocationSendResponse()

    data class Error(
        val message: String,
    ) : LocationSendResponse()
}
