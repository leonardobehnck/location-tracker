package com.location_tracker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
