package com.rhythmday.app.feature.createentry.presentation

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.model.DAY_TITLE_MAX_LENGTH
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.repository.DayPhotoRepository
import com.rhythmday.app.core.domain.repository.DayTagRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEntryViewModelTest {
    @Test
    fun saveBecomesAvailableOnlyAfterEveryMetricIsSet() {
        val repository = RecordingDayEntryRepository()
        val viewModel = createViewModel(repository)
        val values = listOf(
            CreateEntryMetric.ENERGY to 7.0,
            CreateEntryMetric.MOOD to 6.0,
            CreateEntryMetric.WELLBEING to 7.0,
            CreateEntryMetric.SLEEP_QUALITY to 8.0,
            CreateEntryMetric.MENTAL_CLARITY to 6.0,
            CreateEntryMetric.SOCIALITY to 6.0,
            CreateEntryMetric.STRESS to 4.0
        )

        assertFalse(viewModel.uiState.value.canSave)

        values.forEachIndexed { index, (metric, value) ->
            viewModel.onMetricChanged(metric, value)
            if (index < values.lastIndex) {
                assertFalse(viewModel.uiState.value.canSave)
            }
        }

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun saveIsIgnoredWhileAnyMetricIsEmpty() {
        val repository = RecordingDayEntryRepository()
        val viewModel = createViewModel(repository)

        viewModel.onMetricChanged(CreateEntryMetric.ENERGY, 7.0)
        viewModel.save()

        assertTrue(repository.savedEntries.isEmpty())
    }

    @Test
    fun metricValueIsRoundedToTenths() {
        val viewModel = createViewModel(RecordingDayEntryRepository())

        viewModel.onMetricChanged(CreateEntryMetric.ENERGY, 7.26)

        assertEquals(7.3, viewModel.uiState.value.energy)
    }

    @Test
    fun metricValueCannotBeLowerThanPointOne() {
        val viewModel = createViewModel(RecordingDayEntryRepository())

        viewModel.onMetricChanged(CreateEntryMetric.ENERGY, 0.0)

        assertEquals(0.1, viewModel.uiState.value.energy)
    }

    @Test
    fun preparesNewEntryForRequestedDate() {
        val viewModel = createViewModel(RecordingDayEntryRepository())
        val missedDate = LocalDate(2026, 7, 18)

        viewModel.prepareEntry(entryId = null, entryDate = missedDate)

        assertEquals(missedDate, viewModel.uiState.value.entryDate)
    }

    @Test
    fun titleIsLimitedBeforeSaving() {
        val viewModel = createViewModel(RecordingDayEntryRepository())

        viewModel.onTitleChanged("а".repeat(DAY_TITLE_MAX_LENGTH + 5))

        assertEquals(DAY_TITLE_MAX_LENGTH, viewModel.uiState.value.title.length)
    }

    @Test
    fun pickedPhotosCanBeRemovedBeforeSaving() {
        val viewModel = createViewModel(RecordingDayEntryRepository())
        val firstPhoto = byteArrayOf(1, 2, 3)
        val secondPhoto = byteArrayOf(4, 5, 6)

        viewModel.onPhotosPicked(listOf(firstPhoto, secondPhoto))
        viewModel.onPhotoRemoved(0)

        assertEquals(1, viewModel.uiState.value.photos.size)
        assertContentEquals(secondPhoto, viewModel.uiState.value.photos.single())
    }

    @Test
    fun photoSelectionIsLimitedToTenPhotos() {
        val viewModel = createViewModel(RecordingDayEntryRepository())
        val photos = List(12) { index -> byteArrayOf(index.toByte()) }

        viewModel.onPhotosPicked(photos)

        assertEquals(10, viewModel.uiState.value.photos.size)
        assertEquals("Можно добавить не больше 10 фотографий", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun deletingTagRemovesItFromDatabaseAndSelectedTags() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val tagRepository = RecordingDayTagRepository(listOf("Работа"))
            val viewModel = createViewModel(
                repository = RecordingDayEntryRepository(),
                tagRepository = tagRepository
            )
            viewModel.prepareEntry(null)
            advanceUntilIdle()
            viewModel.onCustomTagAdded("Медитация")
            advanceUntilIdle()

            viewModel.onTagDeleted("Медитация")
            advanceUntilIdle()

            assertFalse("Медитация" in viewModel.uiState.value.selectedTags)
            assertFalse("Медитация" in viewModel.uiState.value.availableTags)
            assertFalse("Медитация" in tagRepository.observeTags().first())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadsAndUpdatesRequestedEntry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val originalEntry = DayEntry(
                id = 9,
                date = LocalDate(2026, 7, 19),
                metrics = DayMetrics(4, 5, 6, 7, 8, 9, 3),
                overallScore = 6.4,
                tags = listOf("Спорт", "Медитация"),
                title = "Спокойное воскресенье",
                note = "Исходный комментарий"
            )
            val repository = RecordingDayEntryRepository(listOf(originalEntry))
            val originalPhotos = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
            val photoRepository = RecordingDayPhotoRepository(originalEntry.id, originalPhotos)
            val viewModel = createViewModel(repository, photoRepository = photoRepository)

            viewModel.prepareEntry(originalEntry.id)
            advanceUntilIdle()

            assertEquals(originalEntry.date, viewModel.uiState.value.entryDate)
            assertEquals(originalEntry.title, viewModel.uiState.value.title)
            assertEquals(originalEntry.metrics.energy, viewModel.uiState.value.energy)
            assertEquals(originalEntry.metrics.sleepQuality, viewModel.uiState.value.sleepQuality)
            assertEquals(originalEntry.tags.toSet(), viewModel.uiState.value.selectedTags)
            assertEquals(originalEntry.note, viewModel.uiState.value.note)
            assertContentEquals(originalPhotos[0], viewModel.uiState.value.photos[0])
            assertContentEquals(originalPhotos[1], viewModel.uiState.value.photos[1])

            viewModel.onMetricChanged(CreateEntryMetric.ENERGY, 9.4)
            viewModel.onTitleChanged("Новый ритм")
            viewModel.onNoteChanged("Обновленный комментарий")
            viewModel.save()
            advanceUntilIdle()

            val updatedEntry = repository.savedEntries.single()
            assertEquals(originalEntry.id, updatedEntry.id)
            assertEquals(originalEntry.date, updatedEntry.date)
            assertEquals("Новый ритм", updatedEntry.title)
            assertEquals(9.4, updatedEntry.metrics.energy)
            assertEquals("Обновленный комментарий", updatedEntry.note)
            assertContentEquals(originalPhotos[0], photoRepository.photos.value[0])
            assertContentEquals(originalPhotos[1], photoRepository.photos.value[1])
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(
        repository: DayEntryRepository,
        tagRepository: DayTagRepository = RecordingDayTagRepository(),
        photoRepository: DayPhotoRepository = RecordingDayPhotoRepository()
    ): CreateEntryViewModel =
        CreateEntryViewModel(
            saveDayEntryUseCase = SaveDayEntryUseCase(
                repository = repository,
                scoreCalculator = DayScoreCalculatorUseCase(),
                currentDate = { LocalDate(2026, 7, 20) }
            ),
            observeDayEntryUseCase = ObserveDayEntryUseCase(repository),
            dayTagRepository = tagRepository,
            dayPhotoRepository = photoRepository
        )
}

private class RecordingDayPhotoRepository(
    initialEntryId: Long? = null,
    initialPhotos: List<ByteArray> = emptyList()
) : DayPhotoRepository {
    private var entryId = initialEntryId
    val photos = MutableStateFlow(initialPhotos)

    override fun observePhotos(entryId: Long): Flow<List<ByteArray>> =
        if (this.entryId == null || this.entryId == entryId) photos else MutableStateFlow(emptyList())

    override suspend fun replacePhotos(entryId: Long, photos: List<ByteArray>) {
        this.entryId = entryId
        this.photos.value = photos
    }
}

private class RecordingDayTagRepository(
    tags: List<String> = emptyList()
) : DayTagRepository {
    private val tagsFlow = MutableStateFlow(tags)

    override fun observeTags(): Flow<List<String>> = tagsFlow

    override suspend fun addTag(tag: String) {
        tagsFlow.update { tags -> (tags + tag).distinct() }
    }

    override suspend fun deleteTag(tag: String) {
        tagsFlow.update { tags -> tags - tag }
    }
}

private class RecordingDayEntryRepository(
    entries: List<DayEntry> = emptyList()
) : DayEntryRepository {
    private val entriesFlow = MutableStateFlow(entries)
    val savedEntries = mutableListOf<DayEntry>()

    override fun observeEntries(): Flow<List<DayEntry>> = entriesFlow

    override suspend fun upsertEntry(entry: DayEntry) {
        savedEntries += entry
        entriesFlow.update { entries ->
            entries.filterNot { currentEntry -> currentEntry.date == entry.date } + entry
        }
    }
}
