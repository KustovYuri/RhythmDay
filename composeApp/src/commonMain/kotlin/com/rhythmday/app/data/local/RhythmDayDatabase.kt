package com.rhythmday.app.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.DatabaseMetadataEntity
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryTagCrossRef
import com.rhythmday.app.data.local.entity.DayTagEntity
import kotlinx.coroutines.Dispatchers

const val RHYTHM_DAY_DATABASE_NAME = "rhythmday.db"

@Database(
    entities = [
        DayEntryEntity::class,
        DayTagEntity::class,
        DayEntryTagCrossRef::class,
        DatabaseMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(RhythmDayDatabaseConstructor::class)
abstract class RhythmDayDatabase : RoomDatabase() {
    abstract fun rhythmDayDao(): RhythmDayDao
}

@Suppress("KotlinNoActualForExpect")
expect object RhythmDayDatabaseConstructor : RoomDatabaseConstructor<RhythmDayDatabase> {
    override fun initialize(): RhythmDayDatabase
}

fun buildRhythmDayDatabase(
    builder: RoomDatabase.Builder<RhythmDayDatabase>
): RhythmDayDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
