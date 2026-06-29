package com.example.titama.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.Instant

/**
 * Represents a completed task saved to the log.
 */
@Entity(tableName = "task_entries")
data class TaskEntry(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val note: String = "",
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long,
    val roundedDurationMinutes: Long,
    val roundingIntervalUsed: Int = 5 // Default to 5m
)

/**
 * Represents the current state of a running task.
 */
data class ActiveTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val startTime: Instant,
    val note: String = ""
)

/**
 * Represents a predefined task for quick selection.
 * Can contain nested tasks for hierarchical categorization.
 */
@Entity(tableName = "quick_tasks")
data class QuickTask(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null // For hierarchical structure
)

/**
 * Rounding intervals for task durations.
 */
enum class RoundingInterval(val minutes: Int, val label: String) {
    NONE(1, "None"),
    FIVE(5, "5m"),
    FIFTEEN(15, "15m"),
    THIRTY(30, "30m"),
    HOUR(60, "1h")
}

/**
 * Theme options for the application.
 */
enum class AppTheme(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Settings for the application, including rounding preferences.
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 0, // Single row for settings
    val roundingInterval: RoundingInterval = RoundingInterval.FIVE,
    val roundUp: Boolean = true,
    val enableNotifications: Boolean = true,
    val useRounding: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val autoRoundTimes: Boolean = false, // Round start/end to nearest interval
    val preventOverlap: Boolean = false  // Ensure next task starts after previous ends
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }
}
