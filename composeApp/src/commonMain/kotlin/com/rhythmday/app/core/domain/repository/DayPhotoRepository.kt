package com.rhythmday.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface DayPhotoRepository {
    fun observePhotos(entryId: Long): Flow<List<ByteArray>>

    suspend fun replacePhotos(entryId: Long, photos: List<ByteArray>)
}
