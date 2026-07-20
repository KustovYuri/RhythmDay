package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveDayEntryUseCase(
    private val repository: DayEntryRepository
) {
    operator fun invoke(entryId: Long): Flow<DayEntry?> =
        repository.observeEntries().map { entries ->
            entries.firstOrNull { entry -> entry.id == entryId }
        }
}
