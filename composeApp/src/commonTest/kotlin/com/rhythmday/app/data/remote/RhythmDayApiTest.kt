package com.rhythmday.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RhythmDayApiTest {
    @Test
    fun loginUsesExpectedEndpointAndParsesSession() = runTest {
        val client = testClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/auth/login", request.url.encodedPath)
            respondJson(TokenResponseJson)
        }
        val api = RhythmDayApi(client, "http://localhost:8080")

        val response = api.login(LoginRequestDto("user@example.com", "long-password"))

        assertEquals("access-token", response.accessToken)
        assertEquals("user-id", response.user.id)
        client.close()
    }

    @Test
    fun upsertEntrySendsBearerTokenAndParsesEntry() = runTest {
        val client = testClient { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/v1/entries/entry-id", request.url.encodedPath)
            assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
            respondJson(DayEntryJson)
        }
        val api = RhythmDayApi(client, "http://localhost:8080")

        val response = api.upsertEntry(
            accessToken = "access-token",
            entryId = "entry-id",
            request = UpsertDayEntryRequestDto(
                date = "2026-08-18",
                metrics = DayMetricsDto(7.0, 6.0, 8.0, 7.0, 5.0, 8.0, 3.0),
                overallScore = 7.1,
                tags = listOf("Work"),
                title = "Productive day",
                note = null,
                version = null
            )
        )

        assertEquals("entry-id", response.id)
        assertEquals(1L, response.version)
        client.close()
    }

    @Test
    fun serverErrorIsExposedAsApiException() = runTest {
        val client = testClient {
            respondJson(
                content = """{"code":"ENTRY_VERSION_CONFLICT","message":"Conflict"}""",
                status = HttpStatusCode.Conflict
            )
        }
        val api = RhythmDayApi(client, "http://localhost:8080")

        val error = assertFailsWith<ApiException> {
            api.getEntry("access-token", "entry-id")
        }

        assertEquals(HttpStatusCode.Conflict, error.status)
        assertEquals("ENTRY_VERSION_CONFLICT", error.code)
        assertTrue(error.message.contains("Conflict"))
        client.close()
    }

    private fun testClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    private companion object {
        const val TokenResponseJson = """
            {
              "accessToken":"access-token",
              "accessTokenExpiresAt":"2026-08-18T18:00:00Z",
              "refreshToken":"refresh-token",
              "refreshTokenExpiresAt":"2026-09-18T18:00:00Z",
              "user":{
                "id":"user-id",
                "firstName":"Ivan",
                "lastName":"Ivanov",
                "email":"user@example.com",
                "timezone":"Europe/Moscow",
                "createdAt":"2026-08-18T10:00:00Z"
              }
            }
        """

        const val DayEntryJson = """
            {
              "id":"entry-id",
              "date":"2026-08-18",
              "metrics":{
                "energy":7.0,
                "mood":6.0,
                "wellbeing":8.0,
                "mentalClarity":7.0,
                "sociality":5.0,
                "sleepQuality":8.0,
                "stress":3.0
              },
              "overallScore":7.1,
              "tags":["Work"],
              "title":"Productive day",
              "note":null,
              "createdAt":"2026-08-18T10:00:00Z",
              "updatedAt":"2026-08-18T10:00:00Z",
              "deletedAt":null,
              "version":1
            }
        """
    }
}
