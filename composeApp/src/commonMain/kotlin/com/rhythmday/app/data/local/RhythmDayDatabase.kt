package com.rhythmday.app.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.DatabaseMetadataEntity
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryTagCrossRef
import com.rhythmday.app.data.local.entity.DayTagEntity
import com.rhythmday.app.data.local.entity.PendingTagDeletionEntity
import kotlinx.coroutines.Dispatchers

const val RHYTHM_DAY_DATABASE_NAME = "rhythmday.db"

@Database(
    entities = [
        DayEntryEntity::class,
        DayTagEntity::class,
        DayEntryTagCrossRef::class,
        DatabaseMetadataEntity::class,
        PendingTagDeletionEntity::class
    ],
    version = 3,
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
    .addMigrations(Migration1To2, Migration2To3)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

private object Migration1To2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE day_entries ADD COLUMN remote_id TEXT")
        connection.execSQL("ALTER TABLE day_entries ADD COLUMN server_version INTEGER")
        connection.execSQL(
            "ALTER TABLE day_entries ADD COLUMN sync_pending INTEGER NOT NULL DEFAULT 1"
        )
        connection.execSQL(
            "ALTER TABLE day_entries ADD COLUMN remote_updated_at_epoch_milliseconds INTEGER"
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_day_entries_remote_id " +
                "ON day_entries(remote_id)"
        )

        connection.execSQL("ALTER TABLE day_tags ADD COLUMN remote_id TEXT")
        connection.execSQL(
            "ALTER TABLE day_tags ADD COLUMN default_tag INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "ALTER TABLE day_tags ADD COLUMN sync_pending INTEGER NOT NULL DEFAULT 1"
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_day_tags_remote_id ON day_tags(remote_id)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS pending_tag_deletions (" +
                "remote_id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)"
        )
    }
}

private object Migration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE day_entries ADD COLUMN local_revision INTEGER NOT NULL DEFAULT 0"
        )
    }
}
