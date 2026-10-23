package com.rhythmday.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "ai_reports",
    primaryKeys = ["from_epoch_day", "to_epoch_day"]
)
data class AiReportEntity(
    @ColumnInfo(name = "from_epoch_day") val fromEpochDay: Long,
    @ColumnInfo(name = "to_epoch_day") val toEpochDay: Long,
    @ColumnInfo(name = "entries_fingerprint") val entriesFingerprint: String,
    @ColumnInfo(name = "report_json") val reportJson: String,
    @ColumnInfo(name = "generated_at_epoch_milliseconds") val generatedAtEpochMilliseconds: Long
)
