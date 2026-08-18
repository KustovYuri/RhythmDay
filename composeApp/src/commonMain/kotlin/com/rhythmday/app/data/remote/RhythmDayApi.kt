package com.rhythmday.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class RhythmDayApi(
    private val client: HttpClient,
    baseUrl: String
) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val errorJson = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    suspend fun register(request: RegisterRequestDto): TokenResponseDto =
        client.post(url("/api/v1/auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrThrow()

    suspend fun login(request: LoginRequestDto): TokenResponseDto =
        client.post(url("/api/v1/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrThrow()

    suspend fun refresh(refreshToken: String): TokenResponseDto =
        client.post(url("/api/v1/auth/refresh")) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequestDto(refreshToken))
        }.bodyOrThrow()

    suspend fun logout(refreshToken: String) {
        client.post(url("/api/v1/auth/logout")) {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequestDto(refreshToken))
        }.ensureSuccess()
    }

    suspend fun changePassword(
        accessToken: String,
        request: ChangePasswordRequestDto
    ) {
        client.post(url("/api/v1/auth/password/change")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.ensureSuccess()
    }

    suspend fun forgotPassword(email: String) {
        client.post(url("/api/v1/auth/password/forgot")) {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequestDto(email))
        }.ensureSuccess()
    }

    suspend fun resetPassword(request: ResetPasswordRequestDto) {
        client.post(url("/api/v1/auth/password/reset")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.ensureSuccess()
    }

    suspend fun listEntries(accessToken: String, from: String, to: String): List<DayEntryDto> =
        client.get(url("/api/v1/entries")) {
            bearerAuth(accessToken)
            parameter("from", from)
            parameter("to", to)
        }.bodyOrThrow()

    suspend fun entryChanges(accessToken: String, since: String): List<DayEntryDto> =
        client.get(url("/api/v1/entries/changes")) {
            bearerAuth(accessToken)
            parameter("since", since)
        }.bodyOrThrow()

    suspend fun getEntry(accessToken: String, entryId: String): DayEntryDto =
        client.get(url("/api/v1/entries/$entryId")) {
            bearerAuth(accessToken)
        }.bodyOrThrow()

    suspend fun upsertEntry(
        accessToken: String,
        entryId: String,
        request: UpsertDayEntryRequestDto
    ): DayEntryDto = client.put(url("/api/v1/entries/$entryId")) {
        bearerAuth(accessToken)
        contentType(ContentType.Application.Json)
        setBody(request)
    }.bodyOrThrow()

    suspend fun deleteEntry(accessToken: String, entryId: String, version: Long) {
        client.delete(url("/api/v1/entries/$entryId")) {
            bearerAuth(accessToken)
            parameter("version", version)
        }.ensureSuccess()
    }

    suspend fun listTags(accessToken: String): List<DayTagDto> =
        client.get(url("/api/v1/tags")) {
            bearerAuth(accessToken)
        }.bodyOrThrow()

    suspend fun createTag(accessToken: String, name: String): DayTagDto =
        client.post(url("/api/v1/tags")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(CreateDayTagRequestDto(name))
        }.bodyOrThrow()

    suspend fun deleteTag(accessToken: String, tagId: String) {
        client.delete(url("/api/v1/tags/$tagId")) {
            bearerAuth(accessToken)
        }.ensureSuccess()
    }

    suspend fun exportDiary(accessToken: String): DiaryExportDto =
        client.get(url("/api/v1/export/diary")) {
            bearerAuth(accessToken)
        }.bodyOrThrow()

    private fun url(path: String): String {
        check(isConfigured) { "RhythmDay API base URL is not configured" }
        return "$baseUrl$path"
    }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        ensureSuccess()
        return body()
    }

    private suspend fun HttpResponse.ensureSuccess() {
        if (status.value in 200..299) return

        val responseText = bodyAsText()
        val error = runCatching {
            errorJson.decodeFromString<ApiErrorDto>(responseText)
        }.getOrNull()
        throw ApiException(
            status = status,
            code = error?.code,
            fieldErrors = error?.fieldErrors,
            message = error?.message ?: "Backend request failed with status ${status.value}"
        )
    }
}

class ApiException(
    val status: HttpStatusCode,
    val code: String?,
    val fieldErrors: Map<String, String>?,
    override val message: String
) : Exception(message)
