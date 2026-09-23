package com.rhythmday.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rhythmday.app.data.local.entity.DatabaseMetadataEntity
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryPhotoEntity
import com.rhythmday.app.data.local.entity.DayEntryTagCrossRef
import com.rhythmday.app.data.local.entity.DayEntryWithTags
import com.rhythmday.app.data.local.entity.DayTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RhythmDayDao {
    @Transaction
    @Query("SELECT * FROM day_entries ORDER BY date_epoch_day ASC")
    abstract fun observeEntries(): Flow<List<DayEntryWithTags>>

    @Query("SELECT * FROM day_entry_photos WHERE entry_id = :entryId ORDER BY position ASC")
    abstract fun observePhotos(entryId: Long): Flow<List<DayEntryPhotoEntity>>

    @Query("SELECT * FROM day_tags ORDER BY sort_order ASC, name ASC")
    abstract fun observeTags(): Flow<List<DayTagEntity>>

    @Upsert
    protected abstract suspend fun upsertEntryEntity(entry: DayEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEntryEntities(entries: List<DayEntryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagEntity(tag: DayTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagEntities(tags: List<DayTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEntryTags(crossRefs: List<DayEntryTagCrossRef>)

    @Query("DELETE FROM day_entry_tags WHERE entry_id = :entryId")
    protected abstract suspend fun deleteEntryTags(entryId: Long)

    @Query("DELETE FROM day_entry_photos WHERE entry_id = :entryId")
    protected abstract suspend fun deleteEntryPhotos(entryId: Long)

    @Insert
    protected abstract suspend fun insertEntryPhotos(photos: List<DayEntryPhotoEntity>)

    @Query("DELETE FROM day_tags WHERE name = :tag")
    abstract suspend fun deleteTag(tag: String)

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM day_tags")
    protected abstract suspend fun nextTagSortOrder(): Long

    @Query("SELECT value FROM database_metadata WHERE `key` = :key LIMIT 1")
    protected abstract suspend fun metadataValue(key: String): String?

    @Query("SELECT COUNT(*) FROM day_entries")
    protected abstract suspend fun entryCount(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMetadata(metadata: DatabaseMetadataEntity)

    @Transaction
    open suspend fun upsertEntry(entry: DayEntryEntity, tagNames: List<String>) {
        val cleanTags = tagNames.map(String::trim).filter(String::isNotEmpty).distinct()
        insertMissingTags(cleanTags)
        upsertEntryEntity(entry)
        deleteEntryTags(entry.id)
        insertEntryTags(cleanTags.map { tag -> DayEntryTagCrossRef(entry.id, tag) })
    }

    @Transaction
    open suspend fun replacePhotos(entryId: Long, photos: List<ByteArray>) {
        deleteEntryPhotos(entryId)
        insertEntryPhotos(photos.mapIndexed { position, bytes ->
            DayEntryPhotoEntity(entryId, position, bytes)
        })
    }

    @Transaction
    open suspend fun addTag(tag: String) {
        insertMissingTags(listOf(tag))
    }

    @Transaction
    open suspend fun initializeDefaultTags(tags: List<DayTagEntity>) {
        if (metadataValue(DEFAULT_TAGS_INITIALIZED_KEY) != null) return

        insertTagEntities(tags)
        upsertMetadata(
            DatabaseMetadataEntity(
                key = DEFAULT_TAGS_INITIALIZED_KEY,
                value = "true"
            )
        )
    }

    @Transaction
    open suspend fun initializeDemoData(
        entries: List<DayEntryEntity>,
        tags: List<DayTagEntity>,
        entryTags: List<DayEntryTagCrossRef>
    ) {
        if (metadataValue(DEMO_DATA_INITIALIZED_KEY) != null) return

        if (entryCount() == 0L) {
            insertTagEntities(tags)
            insertEntryEntities(entries)
            insertEntryTags(entryTags)
        }
        upsertMetadata(
            DatabaseMetadataEntity(
                key = DEMO_DATA_INITIALIZED_KEY,
                value = "true"
            )
        )
    }

    @Transaction
    open suspend fun addMediumDemoData(
        entries: List<DayEntryEntity>,
        tags: List<DayTagEntity>,
        entryTags: List<DayEntryTagCrossRef>
    ) {
        if (metadataValue(MEDIUM_DEMO_DATA_INITIALIZED_KEY) != null) return

        insertTagEntities(tags)
        val insertedEntryIds = entries
            .zip(insertEntryEntities(entries))
            .filter { (_, rowId) -> rowId != -1L }
            .mapTo(mutableSetOf()) { (entry, _) -> entry.id }
        insertEntryTags(entryTags.filter { crossRef -> crossRef.entryId in insertedEntryIds })
        upsertMetadata(
            DatabaseMetadataEntity(
                key = MEDIUM_DEMO_DATA_INITIALIZED_KEY,
                value = "true"
            )
        )
    }

    private suspend fun insertMissingTags(tags: List<String>) {
        var sortOrder = nextTagSortOrder()
        tags.forEach { tag ->
            val insertedRowId = insertTagEntity(DayTagEntity(name = tag, sortOrder = sortOrder))
            if (insertedRowId != -1L) sortOrder++
        }
    }

    private companion object {
        const val DEFAULT_TAGS_INITIALIZED_KEY = "default_tags_initialized"
        const val DEMO_DATA_INITIALIZED_KEY = "debug_demo_data_initialized_v1"
        const val MEDIUM_DEMO_DATA_INITIALIZED_KEY = "debug_medium_demo_data_initialized_v1"
    }
}
