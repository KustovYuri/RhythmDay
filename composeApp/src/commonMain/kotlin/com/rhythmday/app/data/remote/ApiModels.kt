package com.rhythmday.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val passwordConfirmation: String,
    val timezone: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class LogoutRequestDto(val refreshToken: String)

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
    val newPasswordConfirmation: String
)

@Serializable
data class ForgotPasswordRequestDto(val email: String)

@Serializable
data class ResetPasswordRequestDto(
    val token: String,
    val newPassword: String,
    val newPasswordConfirmation: String
)

@Serializable
data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val timezone: String,
    val createdAt: String
)

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
    val user: UserDto
)

@Serializable
data class DayMetricsDto(
    val energy: Double,
    val mood: Double,
    val wellbeing: Double,
    val mentalClarity: Double,
    val sociality: Double,
    val sleepQuality: Double,
    val stress: Double
)

@Serializable
data class UpsertDayEntryRequestDto(
    val date: String,
    val metrics: DayMetricsDto,
    val overallScore: Double,
    val tags: List<String>,
    val title: String?,
    val note: String?,
    val version: Long?
)

@Serializable
data class DayEntryDto(
    val id: String,
    val date: String,
    val metrics: DayMetricsDto,
    val overallScore: Double,
    val tags: List<String>,
    val title: String?,
    val note: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val version: Long
)

@Serializable
data class CreateDayTagRequestDto(val name: String)

@Serializable
data class DayTagDto(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val defaultTag: Boolean
)

@Serializable
data class DiaryExportDto(
    val exportedAt: String,
    val user: UserDto,
    val entries: List<DayEntryDto>
)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val timestamp: String? = null,
    val requestId: String? = null,
    val fieldErrors: Map<String, String>? = null
)
