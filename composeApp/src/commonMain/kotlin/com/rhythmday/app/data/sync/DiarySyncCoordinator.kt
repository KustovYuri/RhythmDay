package com.rhythmday.app.data.sync

import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.DatabaseMetadataEntity
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryWithTags
import com.rhythmday.app.data.local.entity.DayTagEntity
import com.rhythmday.app.data.local.entity.PendingTagDeletionEntity
import com.rhythmday.app.data.remote.ApiException
import com.rhythmday.app.data.remote.DayEntryDto
import com.rhythmday.app.data.remote.DayMetricsDto
import com.rhythmday.app.data.remote.DayTagDto
import com.rhythmday.app.data.remote.RhythmDayApi
import com.rhythmday.app.data.remote.UpsertDayEntryRequestDto
import com.rhythmday.app.core.domain.sync.DiaryRefresher
import com.rhythmday.app.feature.auth.data.AuthenticationRequiredException
import com.rhythmday.app.feature.auth.data.AuthorizedSession
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DiarySyncCoordinator(
    private val api: RhythmDayApi,
    private val authorizedSession: AuthorizedSession,
    private val dao: RhythmDayDao,
    private val now: () -> Instant = Clock.System::now
) : DiaryRefresher {
    private val syncMutex = Mutex()

    override suspend fun refresh(): Result<Unit> {
        if (!api.isConfigured) return Result.success(Unit)
        return syncMutex.withLock { runCatching { syncOnce() } }
    }

    private suspend fun syncOnce() {
        val userId = authorizedSession.userId() ?: throw AuthenticationRequiredException()
        ensureDatabaseOwner(userId)
        val syncStartedAt = now()

        pushTagDeletions()
        pullTags()
        pushTags()
        pullEntries()
        pushEntries()
        pullTags()

        dao.upsertMetadata(DatabaseMetadataEntity(LastSyncAtKey, syncStartedAt.toString()))
    }

    private suspend fun ensureDatabaseOwner(userId: String) {
        val existingOwner = dao.metadataValue(SyncUserIdKey)
        check(existingOwner == null || existingOwner == userId) {
            "Local diary belongs to another account"
        }
        if (existingOwner == null) {
            dao.upsertMetadata(DatabaseMetadataEntity(SyncUserIdKey, userId))
        }
    }

    private suspend fun pushTagDeletions() {
        dao.pendingTagDeletions().forEach { deletion ->
            try {
                authorizedSession.request { token -> api.deleteTag(token, deletion.remoteId) }
            } catch (error: ApiException) {
                if (error.status != HttpStatusCode.NotFound) throw error
            }
            dao.removePendingTagDeletion(deletion.remoteId)
        }
    }

    private suspend fun pullTags() {
        val remoteTags = authorizedSession.request(api::listTags)
        val remoteIds = remoteTags.mapTo(mutableSetOf(), DayTagDto::id)

        remoteTags.forEach { tag -> dao.upsertRemoteTag(tag.toEntity()) }
        dao.allTags()
            .filter { tag ->
                !tag.syncPending && tag.remoteId != null && tag.remoteId !in remoteIds
            }
            .forEach { tag -> dao.deleteSyncedTag(requireNotNull(tag.remoteId)) }
    }

    private suspend fun pushTags() {
        dao.pendingTags().forEach { localTag ->
            val remoteTag = try {
                authorizedSession.request { token -> api.createTag(token, localTag.name) }
            } catch (error: ApiException) {
                if (error.code != "TAG_ALREADY_EXISTS") throw error
                authorizedSession.request(api::listTags)
                    .firstOrNull { tag -> tag.name.equals(localTag.name, ignoreCase = true) }
                    ?: throw error
            }
            val updatedRows = dao.markTagSynced(
                name = localTag.name,
                remoteId = remoteTag.id,
                sortOrder = remoteTag.sortOrder.toLong(),
                defaultTag = remoteTag.defaultTag
            )
            if (updatedRows == 0) {
                dao.upsertPendingTagDeletion(
                    PendingTagDeletionEntity(remoteTag.id, localTag.name)
                )
                pushTagDeletions()
            }
        }
    }

    private suspend fun pullEntries() {
        val since = dao.metadataValue(LastSyncAtKey) ?: InitialSyncInstant
        val changes = authorizedSession.request { token -> api.entryChanges(token, since) }
        for (change in changes) {
            applyRemoteEntry(change)
        }
    }

    private suspend fun applyRemoteEntry(remote: DayEntryDto) {
        val dateEpochDay = LocalDate.parse(remote.date).toEpochDays()
        val local = dao.entryByRemoteId(remote.id) ?: dao.entryByDate(dateEpochDay)

        if (remote.deletedAt != null) {
            when {
                local == null -> Unit
                local.entry.syncPending -> dao.linkEntryToRemote(
                    entryId = local.entry.id,
                    remoteId = remote.id,
                    serverVersion = remote.version
                )
                else -> dao.deleteEntryIfUnchanged(
                    entryId = local.entry.id,
                    localRevision = local.entry.localRevision
                )
            }
            return
        }

        if (local?.entry?.syncPending == true) {
            dao.linkEntryToRemote(local.entry.id, remote.id, remote.version)
            return
        }

        val localId = local?.entry?.id ?: dao.nextEntryId()
        dao.upsertRemoteEntry(
            entry = remote.toEntity(localId),
            tagNames = remote.tags,
            expectedLocalRevision = local?.entry?.localRevision
        )
    }

    private suspend fun pushEntries() {
        dao.pendingEntries().forEach { local -> pushEntry(local) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun pushEntry(local: DayEntryWithTags) {
        var remoteId = local.entry.remoteId ?: Uuid.random().toString()
        var serverVersion = local.entry.serverVersion
        dao.linkEntryToRemote(local.entry.id, remoteId, serverVersion)

        repeat(MaxConflictRetries + 1) { attempt ->
            try {
                val saved = authorizedSession.request { token ->
                    api.upsertEntry(
                        accessToken = token,
                        entryId = remoteId,
                        request = local.toRequest(serverVersion)
                    )
                }
                dao.markEntrySynced(
                    entryId = local.entry.id,
                    remoteId = saved.id,
                    serverVersion = saved.version,
                    remoteUpdatedAt = Instant.parse(saved.updatedAt).toEpochMilliseconds(),
                    localRevision = local.entry.localRevision
                )
                return
            } catch (error: ApiException) {
                if (attempt == MaxConflictRetries) throw error
                when (error.code) {
                    "ENTRY_DATE_CONFLICT" -> {
                        val date = LocalDate.fromEpochDays(local.entry.dateEpochDay).toString()
                        val existing = authorizedSession.request { token ->
                            api.listEntries(token, date, date)
                        }.firstOrNull() ?: throw error
                        remoteId = existing.id
                        serverVersion = existing.version
                    }
                    "ENTRY_VERSION_CONFLICT" -> {
                        val existing = authorizedSession.request { token ->
                            api.getEntry(token, remoteId)
                        }
                        serverVersion = existing.version
                    }
                    else -> throw error
                }
                dao.linkEntryToRemote(local.entry.id, remoteId, serverVersion)
            }
        }
    }

    private fun DayEntryWithTags.toRequest(serverVersion: Long?) = UpsertDayEntryRequestDto(
        date = LocalDate.fromEpochDays(entry.dateEpochDay).toString(),
        metrics = DayMetricsDto(
            energy = entry.energy,
            mood = entry.mood,
            wellbeing = entry.wellbeing,
            mentalClarity = entry.mentalClarity,
            sociality = entry.sociality,
            sleepQuality = entry.sleepQuality,
            stress = entry.stress
        ),
        overallScore = entry.overallScore,
        tags = tags.sortedBy(DayTagEntity::sortOrder).map(DayTagEntity::name),
        title = entry.title,
        note = entry.note,
        version = serverVersion
    )

    private fun DayEntryDto.toEntity(localId: Long) = DayEntryEntity(
        id = localId,
        dateEpochDay = LocalDate.parse(date).toEpochDays(),
        energy = metrics.energy,
        mood = metrics.mood,
        wellbeing = metrics.wellbeing,
        mentalClarity = metrics.mentalClarity,
        sociality = metrics.sociality,
        sleepQuality = metrics.sleepQuality,
        stress = metrics.stress,
        overallScore = overallScore,
        title = title,
        note = note,
        savedAtEpochMilliseconds = Instant.parse(updatedAt).toEpochMilliseconds(),
        remoteId = id,
        serverVersion = version,
        syncPending = false,
        remoteUpdatedAtEpochMilliseconds = Instant.parse(updatedAt).toEpochMilliseconds()
    )

    private fun DayTagDto.toEntity() = DayTagEntity(
        name = name,
        sortOrder = sortOrder.toLong(),
        remoteId = id,
        defaultTag = defaultTag,
        syncPending = false
    )

    private companion object {
        const val SyncUserIdKey = "sync_user_id"
        const val LastSyncAtKey = "last_sync_at"
        const val InitialSyncInstant = "1970-01-01T00:00:00Z"
        const val MaxConflictRetries = 2
    }
}
