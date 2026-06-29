package com.example.titama

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalTime

@Composable
fun TrackerScreen(viewModel: TrackerViewModel) {
    val entries = viewModel.todayEntries
    var showForm by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimeEntry?>(null) }
    var detailEntry by remember { mutableStateOf<TimeEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Tracker", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No history yet",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + to log time",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Tracker", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(entries, key = { it.id }) { entry ->
                    EntryCard(entry = entry, onClick = { detailEntry = entry })
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { editingEntry = null; showForm = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add entry")
        }
    }

    detailEntry?.let { entry ->
        EntryDetailDialog(
            entry = entry,
            onDismiss = { detailEntry = null },
            onEdit = { editingEntry = entry; detailEntry = null; showForm = true }
        )
    }

    if (showForm) {
        EntryFormDialog(
            initial = editingEntry,
            onDismiss = { showForm = false; editingEntry = null },
            onSave = { entry ->
                if (editingEntry == null) viewModel.addEntry(entry)
                else viewModel.updateEntry(entry)
                showForm = false
                editingEntry = null
            }
        )
    }
}

@Composable
fun EntryCard(entry: TimeEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.taskName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${entry.startTime.fmt()} – ${entry.endTime.fmt()}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    entry.durationMinutes.toHm(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (entry.roundedEndTime != entry.endTime) {
                Text(
                    "Rounded to ${entry.roundedEndTime.fmt()} · ${entry.roundedDurationMinutes.toHm()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    entry.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    entry.notes,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EntryDetailDialog(entry: TimeEntry, onDismiss: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.taskName, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Start", entry.startTime.fmt())
                DetailRow("End (Actual)", entry.endTime.fmt())
                DetailRow("End (Rounded)", entry.roundedEndTime.fmt())
                HorizontalDivider()
                DetailRow("Duration", entry.durationMinutes.toHm())
                DetailRow("Duration (Rounded)", entry.roundedDurationMinutes.toHm())
                if (entry.location.isNotBlank()) {
                    HorizontalDivider()
                    DetailRow("Location", entry.location)
                }
                if (entry.notes.isNotBlank()) {
                    HorizontalDivider()
                    DetailRow("Notes", entry.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text("Edit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.3f)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryFormDialog(
    initial: TimeEntry?,
    onDismiss: () -> Unit,
    onSave: (TimeEntry) -> Unit
) {
    var taskName by remember { mutableStateOf(initial?.taskName ?: "") }
    var startTime by remember {
        mutableStateOf(initial?.startTime ?: LocalTime.now().minusHours(1).withSecond(0).withNano(0))
    }
    var endTime by remember {
        mutableStateOf(initial?.endTime ?: LocalTime.now().withSecond(0).withNano(0))
    }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (initial == null) "New Entry" else "Edit Entry",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Start Time",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(startTime.fmt(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "End Time",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(endTime.fmt(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (taskName.isNotBlank()) {
                                val entry = if (initial != null) {
                                    initial.copy(
                                        taskName = taskName,
                                        startTime = startTime,
                                        endTime = endTime,
                                        notes = notes,
                                        location = location
                                    )
                                } else {
                                    TimeEntry(
                                        taskName = taskName,
                                        startTime = startTime,
                                        endTime = endTime,
                                        notes = notes,
                                        location = location
                                    )
                                }
                                onSave(entry)
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showStartPicker) {
        AppTimePickerDialog(
            initial = startTime,
            onDismiss = { showStartPicker = false },
            onConfirm = { startTime = it; showStartPicker = false }
        )
    }

    if (showEndPicker) {
        AppTimePickerDialog(
            initial = endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = { endTime = it; showEndPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false
    )
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
