package com.rhythmday.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSProcessInfo

actual fun createAiHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformAiBaseUrl(): String =
    NSProcessInfo.processInfo.environment["RHYTHMDAY_API_BASE_URL"] as? String
        ?: "http://127.0.0.1:8080"
