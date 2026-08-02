package com.rhythmday.app.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createDatabaseBuilder(): RoomDatabase.Builder<RhythmDayDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val databasePath = requireNotNull(documentDirectory?.path) + "/$RHYTHM_DAY_DATABASE_NAME"
    return Room.databaseBuilder<RhythmDayDatabase>(name = databasePath)
}
