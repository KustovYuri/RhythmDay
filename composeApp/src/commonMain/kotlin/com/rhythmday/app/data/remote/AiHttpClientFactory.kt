package com.rhythmday.app.data.remote

import io.ktor.client.HttpClient

expect fun createAiHttpClient(): HttpClient

expect fun platformAiBaseUrl(): String
