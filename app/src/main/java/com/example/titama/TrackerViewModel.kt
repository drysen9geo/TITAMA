package com.example.titama

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class TrackerViewModel : ViewModel() {
    private val _entries = mutableStateListOf<TimeEntry>()

    val todayEntries: List<TimeEntry>
        get() = _entries
            .filter { it.date == LocalDate.now() }
            .sortedByDescending { it.startTime }

    val totalMinutesToday: Long
        get() = todayEntries.sumOf { it.durationMinutes }

    fun addEntry(entry: TimeEntry) {
        _entries.add(entry)
    }

    fun updateEntry(updated: TimeEntry) {
        val i = _entries.indexOfFirst { it.id == updated.id }
        if (i >= 0) _entries[i] = updated
    }
}
