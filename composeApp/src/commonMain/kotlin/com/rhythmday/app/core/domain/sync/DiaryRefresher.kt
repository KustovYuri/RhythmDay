package com.rhythmday.app.core.domain.sync

fun interface DiaryRefresher {
    suspend fun refresh(): Result<Unit>
}
