package com.rhythmday.app.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun createDatabaseBuilder(): RoomDatabase.Builder<RhythmDayDatabase> {
    val applicationDirectory = File(System.getProperty("user.home"), ".rhythmday")
    applicationDirectory.mkdirs()
    val databaseFile = File(applicationDirectory, RHYTHM_DAY_DATABASE_NAME)
    return Room.databaseBuilder<RhythmDayDatabase>(name = databaseFile.absolutePath)
}
