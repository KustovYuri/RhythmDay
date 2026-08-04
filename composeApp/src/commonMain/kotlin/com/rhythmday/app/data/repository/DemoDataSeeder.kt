package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.DayEntryTagCrossRef
import com.rhythmday.app.data.local.entity.DayTagEntity
import com.rhythmday.app.data.local.toEntity
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DemoDataSeeder(
    private val dao: RhythmDayDao,
    private val scoreCalculator: DayScoreCalculatorUseCase,
    private val currentDate: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    },
    private val currentInstant: () -> Instant = Clock.System::now
) {
    suspend fun seedIfEmpty() {
        val entries = createDemoEntries(
            today = currentDate(),
            now = currentInstant(),
            scoreCalculator = scoreCalculator
        )
        val tagNames = (DefaultDayTags + entries.flatMap(DayEntry::tags)).distinct()

        dao.initializeDemoData(
            entries = entries.map(DayEntry::toEntity),
            tags = tagNames.mapIndexed { index, tag ->
                DayTagEntity(name = tag, sortOrder = index.toLong())
            },
            entryTags = entries.flatMap { entry ->
                entry.tags.map { tag -> DayEntryTagCrossRef(entry.id, tag) }
            }
        )
    }
}

internal fun createDemoEntries(
    today: LocalDate,
    now: Instant,
    scoreCalculator: DayScoreCalculatorUseCase
): List<DayEntry> = (1..DEMO_HISTORY_DAYS)
    .filter { dayOffset -> dayOffset <= 14 || dayOffset % 5 != 0 }
    .mapIndexed { index, dayOffset ->
        val template = DemoDayTemplates[index % DemoDayTemplates.size]
        val metrics = createDemoMetrics(index)
        DayEntry(
            id = (index + 1).toLong(),
            date = today.minus(dayOffset, DateTimeUnit.DAY),
            metrics = metrics,
            overallScore = scoreCalculator(metrics),
            tags = template.tags,
            title = template.title,
            note = template.note,
            savedAt = now - dayOffset.days - (index % 6).hours
        )
    }

private fun createDemoMetrics(index: Int): DayMetrics {
    val profile = DemoMetricProfiles[index % DemoMetricProfiles.size]
    return DayMetrics(
        energy = variedMetric(profile.energy, index, phase = 0.3),
        mood = variedMetric(profile.mood, index, phase = 1.5),
        wellbeing = variedMetric(profile.wellbeing, index, phase = 2.7),
        mentalClarity = variedMetric(profile.mentalClarity, index, phase = 3.9),
        sociality = variedMetric(profile.sociality, index, phase = 5.1),
        sleepQuality = variedMetric(profile.sleepQuality, index, phase = 6.3),
        stress = variedMetric(profile.stress, index, phase = 7.5)
    )
}

private fun variedMetric(
    baseValue: Double,
    index: Int,
    phase: Double
): Double {
    val variation = sin(index * 0.61 + phase) * 0.8 +
        cos(index * 0.17 + phase * 0.7) * 0.4
    return round((baseValue + variation).coerceIn(0.1, 10.0) * 10.0) / 10.0
}

private data class DemoMetricProfile(
    val energy: Double,
    val mood: Double,
    val wellbeing: Double,
    val mentalClarity: Double,
    val sociality: Double,
    val sleepQuality: Double,
    val stress: Double
)

private data class DemoDayTemplate(
    val title: String,
    val tags: List<String>,
    val note: String
)

private const val DEMO_HISTORY_DAYS = 180

private val DemoMetricProfiles = listOf(
    DemoMetricProfile(8.8, 9.1, 8.7, 9.2, 7.3, 8.6, 1.8),
    DemoMetricProfile(8.1, 9.5, 9.0, 7.8, 9.7, 8.0, 1.5),
    DemoMetricProfile(6.3, 7.8, 8.2, 7.2, 4.8, 9.1, 1.4),
    DemoMetricProfile(9.6, 8.8, 9.0, 8.5, 7.0, 8.1, 2.2),
    DemoMetricProfile(4.8, 4.1, 4.5, 3.7, 3.4, 3.6, 9.2),
    DemoMetricProfile(7.2, 8.4, 8.7, 8.0, 5.2, 9.3, 1.1),
    DemoMetricProfile(7.7, 9.0, 8.8, 7.6, 8.9, 8.2, 1.8),
    DemoMetricProfile(3.3, 3.6, 4.1, 2.9, 3.2, 2.4, 9.5),
    DemoMetricProfile(8.7, 9.2, 8.9, 9.4, 6.1, 8.5, 1.3),
    DemoMetricProfile(7.5, 8.0, 7.8, 9.0, 5.5, 7.6, 3.0),
    DemoMetricProfile(9.0, 9.4, 9.1, 8.3, 8.8, 8.4, 1.4),
    DemoMetricProfile(6.9, 8.8, 9.0, 8.7, 4.5, 9.5, 0.8),
    DemoMetricProfile(7.2, 7.9, 8.0, 7.5, 6.5, 8.0, 2.7),
    DemoMetricProfile(6.8, 8.7, 8.3, 7.4, 9.1, 7.2, 2.1),
    DemoMetricProfile(6.2, 8.0, 8.1, 7.0, 5.0, 8.8, 1.9),
    DemoMetricProfile(2.6, 3.8, 2.5, 3.1, 2.2, 4.0, 7.8)
)

private val DemoDayTemplates = listOf(
    DemoDayTemplate(
        title = "Продуктивный день",
        tags = listOf("Работа", "Продуктивность", "Прогулка"),
        note = "Удалось спокойно закончить важные задачи и оставить время на вечернюю прогулку."
    ),
    DemoDayTemplate(
        title = "Встреча с друзьями",
        tags = listOf("Друзья", "Люди", "Кафе"),
        note = "Давно не виделись, много разговаривали и хорошо провели вечер."
    ),
    DemoDayTemplate(
        title = "День восстановления",
        tags = listOf("Отдых", "Восстановление", "Дом"),
        note = "Снизил темп, выспался и посвятил день спокойным домашним делам."
    ),
    DemoDayTemplate(
        title = "Интенсивная тренировка",
        tags = listOf("Спорт", "Прогулка", "Хороший сон"),
        note = "Тренировка была сложной, но после нее появилось приятное чувство бодрости."
    ),
    DemoDayTemplate(
        title = "Сложный дедлайн",
        tags = listOf("Работа", "Перегруз", "Дедлайн"),
        note = "Задач оказалось больше ожидаемого, пришлось дольше сохранять концентрацию."
    ),
    DemoDayTemplate(
        title = "Тихий выходной",
        tags = listOf("Дом", "Отдых", "Чтение"),
        note = "Никуда не торопился, читал и занимался тем, что давно откладывал."
    ),
    DemoDayTemplate(
        title = "Семейный вечер",
        tags = listOf("Семья", "Дом", "Спокойствие"),
        note = "Провели вечер вместе за ужином и обсудили планы на ближайшие дни."
    ),
    DemoDayTemplate(
        title = "Неспокойный день",
        tags = listOf("Тревога", "Плохой сон", "Перегруз"),
        note = "Было трудно сосредоточиться после короткого сна, поэтому сделал только главное."
    ),
    DemoDayTemplate(
        title = "Творческий поток",
        tags = listOf("Творчество", "Музыка", "Спокойствие"),
        note = "Появилось несколько свежих идей, которые хотелось сразу попробовать."
    ),
    DemoDayTemplate(
        title = "Учебный рывок",
        tags = listOf("Учёба", "Продуктивность", "Конспекты"),
        note = "Разобрал сложную тему и закрепил материал небольшим практическим заданием."
    ),
    DemoDayTemplate(
        title = "Новый маршрут",
        tags = listOf("Путешествие", "Прогулка", "Люди"),
        note = "Побывал в новом месте, много ходил пешком и сделал несколько фотографий."
    ),
    DemoDayTemplate(
        title = "Время для себя",
        tags = listOf("Медитация", "Спокойствие", "Восстановление"),
        note = "Отложил телефон, побыл в тишине и немного лучше услышал собственные мысли."
    ),
    DemoDayTemplate(
        title = "Домашние заботы",
        tags = listOf("Дом", "Семья", "Продуктивность"),
        note = "Разобрал накопившиеся бытовые дела и освободил пространство вокруг себя."
    ),
    DemoDayTemplate(
        title = "Долгий разговор",
        tags = listOf("Друзья", "Люди", "Поддержка"),
        note = "Открытый разговор помог посмотреть на ситуацию спокойнее и яснее."
    ),
    DemoDayTemplate(
        title = "Кино и отдых",
        tags = listOf("Кино", "Отдых", "Дом"),
        note = "Вечером посмотрел хороший фильм и наконец переключился после рабочей недели."
    ),
    DemoDayTemplate(
        title = "Неважное самочувствие",
        tags = listOf("Болезнь", "Дом", "Восстановление"),
        note = "Самочувствие подвело, поэтому отменил планы и постарался больше отдыхать."
    )
)
