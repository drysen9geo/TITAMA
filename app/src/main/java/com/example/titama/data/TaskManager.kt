package com.example.titama.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor

class TaskManager {
    private val _activeTask = mutableStateOf<ActiveTask?>(null)
    val activeTask: State<ActiveTask?> = _activeTask

    private val _recentTasks = mutableStateListOf<TaskEntry>()
    val recentTasks: List<TaskEntry> = _recentTasks

    var settings = UserSettings()

    fun roundToNearestInterval(instant: Instant): Instant {
        if (!settings.useRounding || !settings.autoRoundTimes) return instant
        val intervalMillis = settings.roundingInterval.minutes * 60 * 1000L
        val millis = instant.toEpochMilli()
        val rounded = (millis + intervalMillis / 2) / intervalMillis * intervalMillis
        return Instant.ofEpochMilli(rounded)
    }

    fun startTask(name: String = "", location: String = "", note: String = "", lastEndTime: Instant? = null) {
        var startTime = Instant.now()
        
        if (settings.useRounding && settings.autoRoundTimes) {
            startTime = roundToNearestInterval(startTime)
        }
        
        if (settings.preventOverlap && lastEndTime != null && startTime.isBefore(lastEndTime)) {
            startTime = lastEndTime
        }
        
        _activeTask.value = ActiveTask(name = name, location = location, startTime = startTime, note = note)
    }

    fun stopTask(providedEndTime: Instant? = null): TaskEntry? {
        val currentTask = _activeTask.value ?: return null
        var endTime = providedEndTime ?: Instant.now()
        
        if (settings.useRounding && settings.autoRoundTimes) {
            // If we are rounding times, we round the end time to the nearest interval too
            // But we ensure it's at least one interval after start if they were very close
            endTime = roundToNearestInterval(endTime)
            val intervalMillis = settings.roundingInterval.minutes * 60 * 1000L
            if (Duration.between(currentTask.startTime, endTime).toMillis() < intervalMillis / 2) {
                endTime = currentTask.startTime.plusMillis(intervalMillis)
            }
        }

        val duration = Duration.between(currentTask.startTime, endTime)
        val minutes = duration.toMinutes()
        
        val roundedMinutes = if (settings.autoRoundTimes) minutes else roundDuration(minutes)
        
        val entry = TaskEntry(
            name = currentTask.name,
            location = currentTask.location,
            note = currentTask.note,
            startTime = currentTask.startTime,
            endTime = endTime,
            durationMinutes = minutes,
            roundedDurationMinutes = roundedMinutes,
            roundingIntervalUsed = if (settings.useRounding) settings.roundingInterval.minutes else 1
        )
        
        _recentTasks.add(0, entry)
        _activeTask.value = null
        return entry
    }

    fun roundDuration(minutes: Long): Long {
        if (!settings.useRounding) return minutes
        val interval = settings.roundingInterval.minutes.toDouble()
        if (interval <= 1.0) return minutes
        
        val units = minutes / interval
        return if (settings.roundUp) {
            (ceil(units) * interval).toLong()
        } else {
            (floor(units) * interval).toLong()
        }
    }
    
    fun getDailyTotalMinutes(): Long {
        return _recentTasks.sumOf { it.roundedDurationMinutes }
    }

    fun updateActiveTask(name: String, location: String, note: String) {
        val current = _activeTask.value ?: return
        _activeTask.value = current.copy(name = name, location = location, note = note)
    }

    fun clearActiveTask() {
        _activeTask.value = null
    }
}
