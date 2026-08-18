package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.repository.DayTagRepository
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.DayTagEntity
import com.rhythmday.app.data.sync.DiarySyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RoomDayTagRepository(
    private val dao: RhythmDayDao,
    private val syncCoordinator: DiarySyncCoordinator? = null,
    private val applicationScope: CoroutineScope? = null
) : DayTagRepository {
    override fun observeTags(): Flow<List<String>> = flow {
        dao.initializeDefaultTags(
            DefaultDayTags.mapIndexed { index, tag ->
                DayTagEntity(name = tag, sortOrder = index.toLong())
            }
        )
        requestSync()
        emitAll(dao.observeTags().map { tags -> tags.map(DayTagEntity::name) })
    }

    override suspend fun addTag(tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isNotEmpty()) {
            dao.addTag(cleanTag)
            requestSync()
        }
    }

    override suspend fun deleteTag(tag: String) {
        dao.deleteTag(tag)
        requestSync()
    }

    private fun requestSync() {
        val coordinator = syncCoordinator ?: return
        applicationScope?.launch { coordinator.refresh() }
    }
}

internal val DefaultDayTags = listOf(
    "Работа",
    "Прогулка",
    "Спорт",
    "Плохой сон",
    "Друзья",
    "Болезнь",
    "Конфликт",
    "Отдых",
    "Хороший сон",
    "Дом",
    "Учёба",
    "Семья",
    "Тревога",
    "Спокойствие",
    "Продуктивность",
    "Перегруз",
    "Восстановление",
    "Творчество",
    "Путешествие",
    "Люди"
)
