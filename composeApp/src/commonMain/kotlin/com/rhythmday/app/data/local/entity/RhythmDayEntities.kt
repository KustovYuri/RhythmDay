package com.rhythmday.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "day_entries",
    indices = [Index(value = ["date_epoch_day"], unique = true)]
)
data class DayEntryEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    val energy: Double,
    val mood: Double,
    val wellbeing: Double,
    @ColumnInfo(name = "mental_clarity") val mentalClarity: Double,
    val sociality: Double,
    @ColumnInfo(name = "sleep_quality") val sleepQuality: Double,
    val stress: Double,
    @ColumnInfo(name = "overall_score") val overallScore: Double,
    val title: String?,
    val note: String?,
    @ColumnInfo(name = "saved_at_epoch_milliseconds") val savedAtEpochMilliseconds: Long?
)

@Entity(tableName = "day_tags")
data class DayTagEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Long
)

@Entity(
    tableName = "day_entry_tags",
    primaryKeys = ["entry_id", "tag_name"],
    foreignKeys = [
        ForeignKey(
            entity = DayEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DayTagEntity::class,
            parentColumns = ["name"],
            childColumns = ["tag_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tag_name"])]
)
data class DayEntryTagCrossRef(
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "tag_name") val tagName: String
)

@Entity(tableName = "database_metadata")
data class DatabaseMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)

data class DayEntryWithTags(
    @Embedded val entry: DayEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            value = DayEntryTagCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "tag_name"
        )
    )
    val tags: List<DayTagEntity>
)
