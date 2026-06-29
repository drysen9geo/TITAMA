package com.example.titama

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.titama.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TaskDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val taskManager = TaskManager()

    val activeTask: State<ActiveTask?> = taskManager.activeTask
    
    val recentTasksFlow: StateFlow<List<TaskEntry>> = taskDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickTasksFlow: StateFlow<List<QuickTask>> = taskDao.getAllQuickTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsFlow: StateFlow<UserSettings> = taskDao.getSettings()
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _elapsedTimeFormatted = mutableStateOf("00:00:00")
    val elapsedTimeFormatted: State<String> = _elapsedTimeFormatted

    init {
        // Apply settings when they change
        viewModelScope.launch {
            settingsFlow.collect { settings ->
                taskManager.settings = settings
            }
        }

        // Initialize default tasks if empty
        viewModelScope.launch {
            val currentQuickTasks = taskDao.getAllQuickTasks().first()
            if (currentQuickTasks.isEmpty()) {
                taskDao.insertQuickTask(QuickTask(name = "Lunch"))
                taskDao.insertQuickTask(QuickTask(name = "Break"))
                taskDao.insertQuickTask(QuickTask(name = "Work"))
            }
        }

        // Start a timer to update the elapsed time display
        viewModelScope.launch {
            while (true) {
                updateTimer()
                delay(1000)
            }
        }
    }

    private fun updateTimer() {
        val current = activeTask.value
        if (current != null) {
            val duration = Duration.between(current.startTime, Instant.now())
            val s = duration.seconds
            _elapsedTimeFormatted.value = String.format(java.util.Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        } else {
            _elapsedTimeFormatted.value = "00:00:00"
        }
    }

    fun startTracking(name: String = "", location: String = "", note: String = "") {
        val lastTask = recentTasksFlow.value.firstOrNull()
        taskManager.startTask(name, location, note, lastTask?.endTime)
    }

    fun updateActiveTask(name: String, location: String, note: String) {
        val current = activeTask.value ?: return
        taskManager.updateActiveTask(name, location, note)
    }

    fun cancelTracking() {
        taskManager.clearActiveTask()
    }

    fun stopTracking(): Instant? {
        val entry = taskManager.stopTask() ?: return null
        viewModelScope.launch {
            taskDao.insertEntry(entry)
        }
        return entry.endTime
    }

    fun updateEntry(entry: TaskEntry) {
        viewModelScope.launch {
            taskDao.updateEntry(entry)
        }
    }

    fun updateEntryWithTimes(entry: TaskEntry, name: String, location: String, note: String, startTime: Instant, endTime: Instant) {
        val durationMinutes = Duration.between(startTime, endTime).toMinutes()
        val settings = settingsFlow.value
        val updatedEntry = entry.copy(
            name = name,
            location = location,
            note = note,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            roundedDurationMinutes = taskManager.roundDuration(durationMinutes),
            roundingIntervalUsed = if (settings.useRounding) settings.roundingInterval.minutes else 1
        )
        viewModelScope.launch {
            taskDao.updateEntry(updatedEntry)
        }
    }

    fun deleteEntry(entry: TaskEntry) {
        viewModelScope.launch {
            taskDao.deleteEntry(entry)
        }
    }

    fun addManualEntry(name: String, location: String, note: String, startTime: Instant, endTime: Instant) {
        val durationMinutes = Duration.between(startTime, endTime).toMinutes()
        val settings = settingsFlow.value
        val entry = TaskEntry(
            name = name,
            location = location,
            note = note,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            roundedDurationMinutes = taskManager.roundDuration(durationMinutes),
            roundingIntervalUsed = if (settings.useRounding) settings.roundingInterval.minutes else 1
        )
        viewModelScope.launch {
            taskDao.insertEntry(entry)
        }
    }

    fun addQuickTask(name: String, parentId: String? = null) {
        viewModelScope.launch {
            taskDao.insertQuickTask(QuickTask(name = name, parentId = parentId))
        }
    }

    fun updateQuickTask(task: QuickTask) {
        viewModelScope.launch {
            taskDao.insertQuickTask(task) // REPLACE strategy
        }
    }

    fun deleteQuickTask(id: String) {
        viewModelScope.launch {
            taskDao.deleteQuickTaskById(id)
        }
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            taskDao.updateSettings(settings)
        }
    }

    fun getDailyTotalMinutes(): Long {
        val today = LocalDate.now()
        return recentTasksFlow.value.filter {
            it.endTime.atZone(ZoneId.systemDefault()).toLocalDate() == today
        }.sumOf { it.roundedDurationMinutes }
    }

    fun getTasksByDate(): Map<LocalDate, List<TaskEntry>> {
        return recentTasksFlow.value.groupBy {
            it.endTime.atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    fun getQuickTasksHierarchy(): List<QuickTaskWithChildren> {
        val allTasks = quickTasksFlow.value
        val nodes = allTasks.map { QuickTaskWithChildren(it) }
        val nodeMap = nodes.associateBy { it.task.id }
        val roots = mutableListOf<QuickTaskWithChildren>()

        nodes.forEach { node ->
            val parentId = node.task.parentId
            if (parentId == null) {
                roots.add(node)
            } else {
                nodeMap[parentId]?.children?.add(node)
            }
        }
        return roots
    }
}

data class QuickTaskWithChildren(
    val task: QuickTask,
    val children: MutableList<QuickTaskWithChildren> = mutableListOf()
)

