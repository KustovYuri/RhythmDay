package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.toDomain
import com.rhythmday.app.data.local.toEntity
import com.rhythmday.app.data.sync.DiarySyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RoomDayEntryRepository(
    private val dao: RhythmDayDao,
    private val demoDataSeeder: DemoDataSeeder? = null,
    private val syncCoordinator: DiarySyncCoordinator? = null,
    private val applicationScope: CoroutineScope? = null
) : DayEntryRepository {
    override fun observeEntries(): Flow<List<DayEntry>> = flow {
        demoDataSeeder?.seedIfEmpty()
        requestSync()
        emitAll(
            dao.observeEntries().map { entries ->
                entries.map { entry -> entry.toDomain() }
            }
        )
    }

    override suspend fun upsertEntry(entry: DayEntry) {
        dao.upsertEntry(entry.toEntity(), entry.tags)
        requestSync()
    }

    private fun requestSync() {
        val coordinator = syncCoordinator ?: return
        applicationScope?.launch { coordinator.refresh() }
    }
}
