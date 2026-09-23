package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.repository.DayPhotoRepository
import com.rhythmday.app.data.local.dao.RhythmDayDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDayPhotoRepository(private val dao: RhythmDayDao) : DayPhotoRepository {
    override fun observePhotos(entryId: Long): Flow<List<ByteArray>> =
        dao.observePhotos(entryId).map { photos -> photos.map { it.imageBytes } }

    override suspend fun replacePhotos(entryId: Long, photos: List<ByteArray>) {
        dao.replacePhotos(entryId, photos)
    }
}
