package com.rhythmday.app.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.DatabaseMetadataEntity
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryPhotoEntity
import com.rhythmday.app.data.local.entity.DayEntryTagCrossRef
import com.rhythmday.app.data.local.entity.DayTagEntity
import kotlinx.coroutines.Dispatchers

const val RHYTHM_DAY_DATABASE_NAME = "rhythmday.db"

@Database(
    entities = [
        DayEntryEntity::class,
        DayEntryPhotoEntity::class,
        DayTagEntity::class,
        DayEntryTagCrossRef::class,
        DatabaseMetadataEntity::class
    ],
    version = 2,
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

private val migration1To2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS `day_entry_photos` (
                `entry_id` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `image_bytes` BLOB NOT NULL,
                PRIMARY KEY(`entry_id`, `position`),
                FOREIGN KEY(`entry_id`) REFERENCES `day_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent()
        )
    }
}

fun buildRhythmDayDatabase(
    builder: RoomDatabase.Builder<RhythmDayDatabase>
): RhythmDayDatabase = builder
    .addMigrations(migration1To2)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
