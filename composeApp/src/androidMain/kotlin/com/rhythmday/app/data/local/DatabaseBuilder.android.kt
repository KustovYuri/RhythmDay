package com.rhythmday.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun createDatabaseBuilder(context: Context): RoomDatabase.Builder<RhythmDayDatabase> {
    val appContext = context.applicationContext
    val databaseFile = appContext.getDatabasePath(RHYTHM_DAY_DATABASE_NAME)
    return Room.databaseBuilder<RhythmDayDatabase>(
        context = appContext,
        name = databaseFile.absolutePath
    )
}
