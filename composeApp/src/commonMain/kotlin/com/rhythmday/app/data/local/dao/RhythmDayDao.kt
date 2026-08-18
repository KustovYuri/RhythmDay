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
import com.rhythmday.app.data.local.entity.PendingTagDeletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RhythmDayDao {
    @Transaction
    @Query("SELECT * FROM day_entries ORDER BY date_epoch_day ASC")
    abstract fun observeEntries(): Flow<List<DayEntryWithTags>>

    @Query("SELECT * FROM day_tags ORDER BY sort_order ASC, name ASC")
    abstract fun observeTags(): Flow<List<DayTagEntity>>

    @Transaction
    @Query("SELECT * FROM day_entries WHERE sync_pending = 1 ORDER BY date_epoch_day ASC")
    abstract suspend fun pendingEntries(): List<DayEntryWithTags>

    @Transaction
    @Query("SELECT * FROM day_entries WHERE remote_id = :remoteId LIMIT 1")
    abstract suspend fun entryByRemoteId(remoteId: String): DayEntryWithTags?

    @Transaction
    @Query("SELECT * FROM day_entries WHERE date_epoch_day = :dateEpochDay LIMIT 1")
    abstract suspend fun entryByDate(dateEpochDay: Long): DayEntryWithTags?

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM day_entries")
    abstract suspend fun nextEntryId(): Long

    @Query("SELECT * FROM day_tags ORDER BY sort_order ASC, name ASC")
    abstract suspend fun allTags(): List<DayTagEntity>

    @Query("SELECT * FROM day_tags WHERE sync_pending = 1 ORDER BY sort_order ASC")
    abstract suspend fun pendingTags(): List<DayTagEntity>

    @Query("SELECT * FROM day_tags WHERE name = :name LIMIT 1")
    abstract suspend fun tagByName(name: String): DayTagEntity?

    @Query("SELECT * FROM day_tags WHERE remote_id = :remoteId LIMIT 1")
    abstract suspend fun tagByRemoteId(remoteId: String): DayTagEntity?

    @Query("SELECT * FROM pending_tag_deletions")
    abstract suspend fun pendingTagDeletions(): List<PendingTagDeletionEntity>

    @Upsert
    protected abstract suspend fun upsertEntryEntity(entry: DayEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEntryEntities(entries: List<DayEntryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagEntity(tag: DayTagEntity): Long

    @Upsert
    protected abstract suspend fun upsertTagEntity(tag: DayTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagEntities(tags: List<DayTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEntryTags(crossRefs: List<DayEntryTagCrossRef>)

    @Query("DELETE FROM day_entry_tags WHERE entry_id = :entryId")
    protected abstract suspend fun deleteEntryTags(entryId: Long)

    @Query("SELECT * FROM day_entries WHERE id = :entryId LIMIT 1")
    protected abstract suspend fun entryEntity(entryId: Long): DayEntryEntity?

    @Query(
        "DELETE FROM day_entries WHERE id = :entryId AND sync_pending = 0 " +
            "AND local_revision = :localRevision"
    )
    abstract suspend fun deleteEntryIfUnchanged(entryId: Long, localRevision: Long): Int

    @Query("DELETE FROM day_tags WHERE name = :tag")
    protected abstract suspend fun deleteTagEntity(tag: String)

    @Query("SELECT entry_id FROM day_entry_tags WHERE tag_name = :tag")
    protected abstract suspend fun entryIdsForTag(tag: String): List<Long>

    @Query(
        "UPDATE day_entries SET sync_pending = 1, local_revision = local_revision + 1 " +
            "WHERE id IN (:entryIds)"
    )
    protected abstract suspend fun markEntriesPending(entryIds: List<Long>)

    @Query("DELETE FROM day_tags WHERE remote_id = :remoteId AND sync_pending = 0")
    protected abstract suspend fun deleteSyncedTagEntity(remoteId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPendingTagDeletion(deletion: PendingTagDeletionEntity)

    @Query("DELETE FROM pending_tag_deletions WHERE remote_id = :remoteId")
    abstract suspend fun removePendingTagDeletion(remoteId: String)

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM day_tags")
    protected abstract suspend fun nextTagSortOrder(): Long

    @Query("SELECT value FROM database_metadata WHERE `key` = :key LIMIT 1")
    abstract suspend fun metadataValue(key: String): String?

    @Query("SELECT COUNT(*) FROM day_entries")
    protected abstract suspend fun entryCount(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMetadata(metadata: DatabaseMetadataEntity)

    @Query(
        "UPDATE day_entries SET remote_id = :remoteId, server_version = :serverVersion " +
            "WHERE id = :entryId"
    )
    abstract suspend fun linkEntryToRemote(
        entryId: Long,
        remoteId: String,
        serverVersion: Long?
    )

    @Query(
        "UPDATE day_entries SET remote_id = :remoteId, server_version = :serverVersion, " +
            "sync_pending = 0, remote_updated_at_epoch_milliseconds = :remoteUpdatedAt " +
            "WHERE id = :entryId AND local_revision = :localRevision"
    )
    abstract suspend fun markEntrySynced(
        entryId: Long,
        remoteId: String,
        serverVersion: Long,
        remoteUpdatedAt: Long,
        localRevision: Long
    ): Int

    @Query(
        "UPDATE day_tags SET remote_id = :remoteId, sort_order = :sortOrder, " +
            "default_tag = :defaultTag, sync_pending = 0 WHERE name = :name"
    )
    abstract suspend fun markTagSynced(
        name: String,
        remoteId: String,
        sortOrder: Long,
        defaultTag: Boolean
    ): Int

    @Transaction
    open suspend fun upsertEntry(entry: DayEntryEntity, tagNames: List<String>) {
        val cleanTags = tagNames.map(String::trim).filter(String::isNotEmpty).distinct()
        insertMissingTags(cleanTags)
        val existing = entryEntity(entry.id)
        upsertEntryEntity(
            entry.copy(
                remoteId = existing?.remoteId,
                serverVersion = existing?.serverVersion,
                syncPending = true,
                localRevision = (existing?.localRevision ?: 0) + 1,
                remoteUpdatedAtEpochMilliseconds = existing?.remoteUpdatedAtEpochMilliseconds
            )
        )
        deleteEntryTags(entry.id)
        insertEntryTags(cleanTags.map { tag -> DayEntryTagCrossRef(entry.id, tag) })
    }

    @Transaction
    open suspend fun upsertRemoteEntry(
        entry: DayEntryEntity,
        tagNames: List<String>,
        expectedLocalRevision: Long?
    ) {
        val current = entryEntity(entry.id)
        if (current != null &&
            (current.syncPending || current.localRevision != expectedLocalRevision)
        ) {
            val remoteId = entry.remoteId ?: return
            linkEntryToRemote(entry.id, remoteId, entry.serverVersion)
            return
        }
        val cleanTags = tagNames.map(String::trim).filter(String::isNotEmpty).distinct()
        insertMissingRemoteTags(cleanTags)
        upsertEntryEntity(
            entry.copy(
                syncPending = false,
                localRevision = current?.localRevision ?: 0
            )
        )
        deleteEntryTags(entry.id)
        insertEntryTags(cleanTags.map { tag -> DayEntryTagCrossRef(entry.id, tag) })
    }

    @Transaction
    open suspend fun upsertRemoteTag(tag: DayTagEntity) {
        val existingByRemoteId = tag.remoteId?.let { remoteId -> tagByRemoteId(remoteId) }
        if (existingByRemoteId != null && existingByRemoteId.name != tag.name) {
            deleteTagEntity(existingByRemoteId.name)
        }
        upsertTagEntity(
            tag.copy(
                sortOrder = tag.sortOrder,
                syncPending = false
            )
        )
    }

    @Transaction
    open suspend fun addTag(tag: String) {
        insertMissingTags(listOf(tag))
    }

    @Transaction
    open suspend fun deleteTag(tag: String) {
        val affectedEntryIds = entryIdsForTag(tag)
        if (affectedEntryIds.isNotEmpty()) markEntriesPending(affectedEntryIds)
        tagByName(tag)?.remoteId?.let { remoteId ->
            upsertPendingTagDeletion(PendingTagDeletionEntity(remoteId, tag))
        }
        deleteTagEntity(tag)
    }

    @Transaction
    open suspend fun deleteSyncedTag(remoteId: String) {
        val tag = tagByRemoteId(remoteId) ?: return
        val affectedEntryIds = entryIdsForTag(tag.name)
        if (affectedEntryIds.isNotEmpty()) markEntriesPending(affectedEntryIds)
        deleteSyncedTagEntity(remoteId)
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

    private suspend fun insertMissingRemoteTags(tags: List<String>) {
        var sortOrder = nextTagSortOrder()
        tags.forEach { tag ->
            val insertedRowId = insertTagEntity(
                DayTagEntity(
                    name = tag,
                    sortOrder = sortOrder,
                    syncPending = false
                )
            )
            if (insertedRowId != -1L) sortOrder++
        }
    }

    private companion object {
        const val DEFAULT_TAGS_INITIALIZED_KEY = "default_tags_initialized"
        const val DEMO_DATA_INITIALIZED_KEY = "debug_demo_data_initialized_v1"
        const val MEDIUM_DEMO_DATA_INITIALIZED_KEY = "debug_medium_demo_data_initialized_v1"
    }
}
