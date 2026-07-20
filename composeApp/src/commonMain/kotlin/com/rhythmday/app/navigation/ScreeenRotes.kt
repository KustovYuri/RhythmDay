package com.rhythmday.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppRoute : NavKey

@Serializable
data object HomeRouteKey : AppRoute

@Serializable
data object HistoryRoute : AppRoute

@Serializable
data object CreateEntryRoute : AppRoute

@Serializable
data class EditEntryRoute(val entryId: Long) : AppRoute

@Serializable
data object AnalyticsRoute : AppRoute

@Serializable
data object AiRoute : AppRoute

@Serializable
data object BlitzRoute : AppRoute

@Serializable
data object SettingsRoute : AppRoute

@Serializable
data class EntryDetailsRoute(val entryId: Long) : AppRoute
