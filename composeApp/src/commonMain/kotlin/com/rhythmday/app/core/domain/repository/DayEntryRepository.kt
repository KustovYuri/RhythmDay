package com.rhythmday.app.core.domain.repository

import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.coroutines.flow.Flow

interface DayEntryRepository {
    fun observeEntries(): Flow<List<DayEntry>>

    suspend fun upsertEntry(entry: DayEntry)
}
