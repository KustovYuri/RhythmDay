package com.rhythmday.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(): HttpClient

fun createRhythmDayHttpClient(): HttpClient = createPlatformHttpClient().config {
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        )
    }
}
