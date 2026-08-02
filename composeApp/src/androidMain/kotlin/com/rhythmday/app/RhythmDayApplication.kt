package com.rhythmday.app

import android.app.Application
import com.rhythmday.app.data.local.buildRhythmDayDatabase
import com.rhythmday.app.data.local.createDatabaseBuilder

class RhythmDayApplication : Application() {
    val database by lazy {
        buildRhythmDayDatabase(createDatabaseBuilder(this))
    }
}
