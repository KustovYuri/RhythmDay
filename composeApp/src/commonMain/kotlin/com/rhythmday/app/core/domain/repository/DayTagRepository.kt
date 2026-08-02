package com.rhythmday.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface DayTagRepository {
    fun observeTags(): Flow<List<String>>

    suspend fun addTag(tag: String)

    suspend fun deleteTag(tag: String)
}
