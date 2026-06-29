package com.example.titama

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class TimeEntry(
    val id: Long = System.currentTimeMillis(),
    val taskName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val location: String = ""
) {
    val durationMinutes: Long
        get() = ChronoUnit.MINUTES.between(startTime, endTime).coerceAtLeast(0)

    val roundedEndTime: LocalTime
        get() {
            val total = endTime.hour * 60 + endTime.minute
            val rounded = (total / 15.0).roundToInt() * 15
            return LocalTime.of((rounded / 60) % 24, rounded % 60)
        }

    val roundedDurationMinutes: Long
        get() = ChronoUnit.MINUTES.between(startTime, roundedEndTime).coerceAtLeast(0)
}

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

fun LocalTime.fmt(): String = this.format(timeFmt)

fun Long.toHm(): String {
    val h = this / 60
    val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
