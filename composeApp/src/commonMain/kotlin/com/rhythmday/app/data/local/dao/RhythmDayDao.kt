package com.rhythmday.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rhythmday.app.data.local.entity.DatabaseMetadataEntity
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryTagCrossRef
import com.rhythmday.app.data.local.entity.DayEntryWithTags
import com.rhythmday.app.data.local.entity.DayTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RhythmDayDao {
    @Transaction
    @Query("SELECT * FROM day_entries ORDER BY date_epoch_day ASC")
    abstract fun observeEntries(): Flow<List<DayEntryWithTags>>

    @Query("SELECT * FROM day_tags ORDER BY sort_order ASC, name ASC")
    abstract fun observeTags(): Flow<List<DayTagEntity>>

    @Upsert
    protected abstract suspend fun upsertEntryEntity(entry: DayEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagEntity(tag: DayTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagEntities(tags: List<DayTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEntryTags(crossRefs: List<DayEntryTagCrossRef>)

    @Query("DELETE FROM day_entry_tags WHERE entry_id = :entryId")
    protected abstract suspend fun deleteEntryTags(entryId: Long)

    @Query("DELETE FROM day_tags WHERE name = :tag")
    abstract suspend fun deleteTag(tag: String)

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM day_tags")
    protected abstract suspend fun nextTagSortOrder(): Long

    @Query("SELECT value FROM database_metadata WHERE `key` = :key LIMIT 1")
    protected abstract suspend fun metadataValue(key: String): String?

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

    private suspend fun insertMissingTags(tags: List<String>) {
        var sortOrder = nextTagSortOrder()
        tags.forEach { tag ->
            val insertedRowId = insertTagEntity(DayTagEntity(name = tag, sortOrder = sortOrder))
            if (insertedRowId != -1L) sortOrder++
        }
    }

    private companion object {
        const val DEFAULT_TAGS_INITIALIZED_KEY = "default_tags_initialized"
    }
}
