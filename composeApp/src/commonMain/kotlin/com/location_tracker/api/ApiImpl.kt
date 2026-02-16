package com.location_tracker.api

import com.location_tracker.data.remote.dto.LocationTrackingRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class ApiImpl(
    engine: HttpClientEngine,
) : Api {
    private val client =
        HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        useArrayPolymorphism = true
                        encodeDefaults = false
                        explicitNulls = false
                        coerceInputValues = true
                    },
                )
            }

            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            println("[Ktor DEBUG] $message")
                        }
                    }
                level = LogLevel.ALL
            }

            defaultRequest {
                url("https://www.api.com/api")
            }

            install(HttpTimeout) {
                val timeout = 30000L
                connectTimeoutMillis = timeout
                requestTimeoutMillis = timeout
                socketTimeoutMillis = timeout
            }
        }

    override suspend fun sendLocationTracking(request: LocationTrackingRequestDto): Response<Unit> =
        request {
            client
                .post("tracking/location") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            Unit
        }
}

sealed class Response<T> {
    data class Success<T>(
        val result: T,
    ) : Response<T>()

    data class Failure<T>(
        val error: String,
    ) : Response<T>()

    data class NoConnection<T>(
        val error: String,
    ) : Response<T>()
}

internal suspend inline fun <T> request(block: () -> T): Response<T> =
    try {
        Response.Success(block())
    } catch (clientRequestException: ClientRequestException) {
        val errorBody =
            runCatching {
                clientRequestException.response.bodyAsText()
            }.getOrNull()

        println("[Ktor] RESPONSE BODY: $errorBody")

        when (clientRequestException.response.status) {
            HttpStatusCode.UnprocessableEntity,
            HttpStatusCode.BadRequest,
            HttpStatusCode.NotFound,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            -> {
                val fallbackMessage = clientRequestException.response.status.description
                Response.Failure(errorBody ?: fallbackMessage)
            }

            else -> Response.Failure(errorBody ?: "failure")
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Throwable) {
        Response.NoConnection(ex.message ?: ex.toString())
    }
