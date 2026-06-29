package com.example.titama

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.titama.data.ActiveTask
import com.example.titama.data.QuickTask
import com.example.titama.data.TaskEntry
import com.example.titama.ui.theme.TITAMATheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settingsFlow.collectAsState()
            
            val darkTheme = when (settings.theme) {
                com.example.titama.data.AppTheme.LIGHT -> false
                com.example.titama.data.AppTheme.DARK -> true
                com.example.titama.data.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            TITAMATheme(darkTheme = darkTheme) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val navItems = remember {
        listOf(
            NavItem("Today", Icons.Filled.Dashboard),
            NavItem("Log", Icons.Filled.CheckCircle),
            NavItem("Totals", Icons.Filled.Timer),
            NavItem("Tasks", Icons.Filled.WatchLater),
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEntrySheet by remember { mutableStateOf(false) }
    val quickTasks by viewModel.quickTasksFlow.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TodayScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    onEntriesClick = { selectedTab = 1 },
                    onLoggedTodayClick = { selectedTab = 2 },
                    onStartClick = { showEntrySheet = true }
                )
                1 -> LogScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
                2 -> TotalsScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
                3 -> TasksScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }
        }
    }

    if (showEntrySheet) {
        TaskEntrySheet(
            onDismiss = { showEntrySheet = false },
            onConfirm = { name, location, note, _, _ ->
                // Check if a task is already running, if so stop it first
                var lastEndTime: Instant? = null
                if (viewModel.activeTask.value != null) {
                    lastEndTime = viewModel.stopTracking()
                }
                viewModel.startTracking(name, location, note)
                showEntrySheet = false
            },
            quickTasks = viewModel.getQuickTasksHierarchy(),
            onAddNewTask = { name -> viewModel.addQuickTask(name) }
        )
    }
}

@Composable
fun TaskDetailDialog(
    task: TaskEntry,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d").withZone(ZoneId.systemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.name, fontWeight = FontWeight.Bold)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailItem(label = "Date", value = dateFormatter.format(task.startTime))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailItem(
                        label = "Start",
                        value = timeFormatter.format(task.startTime),
                        modifier = Modifier.weight(1f)
                    )
                    DetailItem(
                        label = "End",
                        value = timeFormatter.format(task.endTime),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailItem(
                        label = "Actual Duration",
                        value = "${task.durationMinutes}m",
                        modifier = Modifier.weight(1f)
                    )
                    DetailItem(
                        label = "Rounded (${task.roundingIntervalUsed}m)",
                        value = "${task.roundedDurationMinutes}m",
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }
                if (task.location.isNotEmpty()) {
                    DetailItem(label = "Location", value = task.location)
                }
                if (task.note.isNotEmpty()) {
                    DetailItem(label = "Notes", value = task.note)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    onEntriesClick: () -> Unit = {},
    onLoggedTodayClick: () -> Unit = {},
    onStartClick: () -> Unit = {}
) {
    val today = remember { LocalDate.now() }
    val dateFormatted = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }

    val activeTask by viewModel.activeTask
    val recentTasks by viewModel.recentTasksFlow.collectAsState()
    val dailyTotalMinutes = viewModel.getDailyTotalMinutes()
    val elapsedTime by viewModel.elapsedTimeFormatted

    val todayTasks = remember(recentTasks) {
        recentTasks.filter { it.endTime.atZone(ZoneId.systemDefault()).toLocalDate() == today }
    }

    Column(
        modifier = modifier.padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Today",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateFormatted,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurrentTaskCard(
                activeTask = activeTask,
                elapsedTime = elapsedTime,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTask != null) {
                    ControlCard(
                        icon = Icons.Default.Stop,
                        label = "STOP",
                        color = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.stopTracking() },
                        modifier = Modifier.weight(1f)
                    )
                    ControlCard(
                        icon = Icons.Default.SwapHoriz,
                        label = "SWITCH",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onStartClick() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ControlCard(
                        icon = Icons.Default.PlayArrow,
                        label = "START",
                        color = Color(0xFF4CAF50), // Green
                        onClick = onStartClick,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "ENTRIES",
                value = todayTasks.size.toString(),
                modifier = Modifier.weight(1f).clickable { onEntriesClick() }
            )
            StatCard(
                label = "LOGGED TODAY",
                value = formatMinutes(dailyTotalMinutes),
                modifier = Modifier.weight(1f).clickable { onLoggedTodayClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (todayTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nothing logged today, yet!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                lazyItems(todayTasks.take(10)) { task ->
                    RecentTaskItem(task)
                }
            }
        }
    }
}

@Composable
fun CurrentTaskCard(
    activeTask: ActiveTask?,
    elapsedTime: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (activeTask != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BlinkingDot()
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NOW TRACKING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = elapsedTime,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = activeTask.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        .withZone(java.time.ZoneId.systemDefault())
                    Text(
                        text = activeTask.location,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Started ${timeFormatter.format(activeTask.startTime)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Rounds to 5m interval",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No task running",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Start one and it will show up here",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ControlCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEntrySheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Instant, Instant) -> Unit,
    quickTasks: List<com.example.titama.QuickTaskWithChildren>,
    initialName: String = "",
    initialLocation: String = "",
    initialNote: String = "",
    initialStartTime: Instant = Instant.now(),
    initialEndTime: Instant = Instant.now(),
    isManualEntry: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onAddNewTask: (String) -> Unit = {}
) {
    var taskLabel by remember { mutableStateOf(initialName) }
    var location by remember { mutableStateOf(initialLocation) }
    var note by remember { mutableStateOf(initialNote) }
    
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun showTimePicker(current: Instant, onSelected: (Instant) -> Unit) {
        val dateTime = LocalDateTime.ofInstant(current, ZoneId.systemDefault())
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newDateTime = dateTime.withHour(hourOfDay).withMinute(minute)
                onSelected(newDateTime.atZone(ZoneId.systemDefault()).toInstant())
            },
            dateTime.hour,
            dateTime.minute,
            true
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        onDelete != null -> "Edit Entry"
                        isManualEntry -> "Manual Entry"
                        else -> "Start Task"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onConfirm(taskLabel, location, note, startTime, endTime) }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            Column {
                Text("Task label", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = taskLabel,
                        onValueChange = { taskLabel = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("New task") }
                    )
                    IconButton(onClick = { onAddNewTask(taskLabel) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add as quick task")
                    }
                }
            }

            NestedTaskSelector(
                quickTasks = quickTasks,
                onTaskSelected = { taskLabel = it }
            )

            if (isManualEntry || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedCard(
                            onClick = { showTimePicker(startTime) { startTime = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = timeFormatter.format(startTime.atZone(ZoneId.systemDefault())),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedCard(
                            onClick = { showTimePicker(endTime) { endTime = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = timeFormatter.format(endTime.atZone(ZoneId.systemDefault())),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Column {
                Text("Note", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What was the task?") }
                )
            }

            Column {
                Text("Location / area", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Location") }
                )
            }

            if (onDelete != null) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete entry")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun NestedTaskSelector(
    quickTasks: List<com.example.titama.QuickTaskWithChildren>,
    onTaskSelected: (String) -> Unit
) {
    // Use IDs to track the path so updates to the hierarchy list (e.g. adding a task) 
    // are correctly reflected in the UI.
    var selectedPathIds by remember { mutableStateOf(listOf<String>()) }
    
    // Derived state to find the current nodes based on IDs
    val currentLevel = remember(quickTasks, selectedPathIds) {
        var list = quickTasks
        for (id in selectedPathIds) {
            list = list.find { it.task.id == id }?.children ?: emptyList()
        }
        list
    }

    val pathNodes = remember(quickTasks, selectedPathIds) {
        val nodes = mutableListOf<com.example.titama.QuickTaskWithChildren>()
        var list = quickTasks
        for (id in selectedPathIds) {
            val found = list.find { it.task.id == id }
            if (found != null) {
                nodes.add(found)
                list = found.children
            }
        }
        nodes
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedPathIds.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { selectedPathIds = emptyList() }) {
                    Text("Tasks")
                }
                pathNodes.forEachIndexed { index, node ->
                    Text(">", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { selectedPathIds = selectedPathIds.take(index + 1) }) {
                        Text(node.task.name)
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridItems(currentLevel) { node ->
                Surface(
                    onClick = {
                        if (node.children.isNotEmpty()) {
                            selectedPathIds = selectedPathIds + node.task.id
                        } else {
                            val pathName = (pathNodes + node).joinToString("-") { it.task.name }
                            onTaskSelected(pathName)
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = node.task.name,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(4.dp),
                            fontWeight = FontWeight.Medium
                        )
                        if (node.children.isNotEmpty()) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd).padding(2.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlinkingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecentTaskItem(task: TaskEntry, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = task.name, fontWeight = FontWeight.Medium)
                Text(text = task.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${task.roundedDurationMinutes}m",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatMinutes(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val tasksByDate = viewModel.getTasksByDate()
    var showManualEntry by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<TaskEntry?>(null) }
    var editingEntry by remember { mutableStateOf<TaskEntry?>(null) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Log", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            Surface(
                onClick = { showManualEntry = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Add Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* Show more logic */ },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.ExpandMore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Show More")
            }
            OutlinedButton(
                onClick = { /* Calendar logic */ },
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (tasksByDate.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            ) {
                tasksByDate.forEach { (date, tasks) ->
                    item {
                        Text(
                            text = if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tasks.forEach { task ->
                                RecentTaskItem(task, onClick = { selectedTaskForDetail = task })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualEntry) {
        TaskEntrySheet(
            onDismiss = { showManualEntry = false },
            onConfirm = { name, location, note, startTime, endTime ->
                viewModel.addManualEntry(name, location, note, startTime, endTime)
                showManualEntry = false
            },
            quickTasks = viewModel.getQuickTasksHierarchy(),
            isManualEntry = true
        )
    }

    if (selectedTaskForDetail != null) {
        TaskDetailDialog(
            task = selectedTaskForDetail!!,
            onDismiss = { selectedTaskForDetail = null },
            onEdit = {
                editingEntry = selectedTaskForDetail
                selectedTaskForDetail = null
            }
        )
    }

    if (editingEntry != null) {
        val entry = editingEntry!!
        TaskEntrySheet(
            onDismiss = { editingEntry = null },
            onConfirm = { name, location, note, startTime, endTime ->
                viewModel.updateEntryWithTimes(entry, name, location, note, startTime, endTime)
                editingEntry = null
            },
            quickTasks = viewModel.getQuickTasksHierarchy(),
            initialName = entry.name,
            initialLocation = entry.location,
            initialNote = entry.note,
            initialStartTime = entry.startTime,
            initialEndTime = entry.endTime,
            onDelete = {
                viewModel.deleteEntry(entry)
                editingEntry = null
            }
        )
    }
}

@Composable
fun TotalsScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val recentTasks by viewModel.recentTasksFlow.collectAsState()
    val today = LocalDate.now()
    val todayTasks = remember(recentTasks) {
        recentTasks.filter { it.endTime.atZone(ZoneId.systemDefault()).toLocalDate() == today }
    }
    val totalMinutes = todayTasks.sumOf { it.roundedDurationMinutes }
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Totals", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = {
                    val tsv = todayTasks.joinToString("\n") { "${it.name}\t${it.location}\t${it.roundedDurationMinutes}" }
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Timecard TSV", "Task\tLocation\tMinutes\n$tsv")
                    clipboard.setPrimaryClip(clip)
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy as Spreadsheet (TSV)")
                }
                IconButton(onClick = {
                    val report = todayTasks.joinToString("\n") { "${it.name}, ${it.location}, ${it.roundedDurationMinutes}m" }
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Daily Timecard ($today):\n\n$report\n\nTotal: ${formatMinutes(totalMinutes)}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share Report")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "TOTAL LOGGED TODAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatMinutes(totalMinutes),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Timecard Summary",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        if (todayTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "No tasks logged today", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                lazyItems(todayTasks) { task ->
                    val percentage = if (totalMinutes > 0) task.roundedDurationMinutes.toFloat() / totalMinutes else 0f
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = task.name, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${timeFormatter.format(task.startTime)} - ${timeFormatter.format(task.endTime)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (task.location.isNotEmpty()) {
                                        Text(text = " • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = task.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${task.roundedDurationMinutes}m",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${task.durationMinutes}m)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.fillMaxWidth().clip(CircleShape).height(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val quickTasksHierarchy = viewModel.getQuickTasksHierarchy()
    val settings by viewModel.settingsFlow.collectAsState()
    var newTaskName by remember { mutableStateOf("") }
    var navigationStack by remember { mutableStateOf(listOf<String>()) } // List of IDs
    var showSettings by remember { mutableStateOf(false) }
    
    val currentLevel = remember(quickTasksHierarchy, navigationStack) {
        var list = quickTasksHierarchy
        for (id in navigationStack) {
            list = list.find { it.task.id == id }?.children ?: emptyList()
        }
        list
    }

    val parentName = remember(quickTasksHierarchy, navigationStack) {
        if (navigationStack.isEmpty()) "Tasks"
        else {
            fun findName(nodes: List<com.example.titama.QuickTaskWithChildren>, targetId: String): String? {
                for (node in nodes) {
                    if (node.task.id == targetId) return node.task.name
                    val name = findName(node.children, targetId)
                    if (name != null) return name
                }
                return null
            }
            findName(quickTasksHierarchy, navigationStack.last()) ?: "Tasks"
        }
    }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navigationStack.isNotEmpty()) {
                    IconButton(onClick = { 
                        navigationStack = navigationStack.dropLast(1)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(text = parentName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTaskName,
                onValueChange = { newTaskName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add subtask here...") },
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newTaskName.isNotBlank()) {
                        viewModel.addQuickTask(newTaskName, navigationStack.lastOrNull())
                        newTaskName = ""
                    }
                },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lazyItems(currentLevel) { node ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigationStack = navigationStack + node.task.id
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = node.task.name, fontWeight = FontWeight.Medium)
                            if (node.children.isNotEmpty()) {
                                Text(
                                    text = "${node.children.size} subtasks",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.deleteQuickTask(node.task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            settings = settings,
            onSave = { viewModel.updateSettings(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    settings: com.example.titama.data.UserSettings,
    onSave: (com.example.titama.data.UserSettings) -> Unit
) {
    var interval by remember { mutableStateOf(settings.roundingInterval) }
    var roundUp by remember { mutableStateOf(settings.roundUp) }
    var useRounding by remember { mutableStateOf(settings.useRounding) }
    var theme by remember { mutableStateOf(settings.theme) }
    var autoRoundTimes by remember { mutableStateOf(settings.autoRoundTimes) }
    var preventOverlap by remember { mutableStateOf(settings.preventOverlap) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Theme", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.titama.data.AppTheme.entries.forEach { t ->
                            FilterChip(
                                selected = theme == t,
                                onClick = { theme = t },
                                label = { Text(t.label) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Rounding")
                    Switch(checked = useRounding, onCheckedChange = { useRounding = it })
                }

                if (useRounding) {
                    Column {
                        Text("Rounding Interval", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.example.titama.data.RoundingInterval.entries.forEach { ri ->
                                FilterChip(
                                    selected = interval == ri,
                                    onClick = { interval = ri },
                                    label = { Text(ri.label) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Round Up (Always)")
                        Switch(checked = roundUp, onCheckedChange = { roundUp = it })
                    }

                    HorizontalDivider()

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Time Blocking", fontWeight = FontWeight.Bold)
                            var showTimeBlockingInfo by remember { mutableStateOf(false) }
                            IconButton(onClick = { showTimeBlockingInfo = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(20.dp))
                            }
                            if (showTimeBlockingInfo) {
                                TITAMADialog(
                                    onDismiss = { showTimeBlockingInfo = false },
                                    title = "Time Blocking",
                                    content = {
                                        Text("Auto-Round Times: Round start/end to nearest interval.\nExample (5m): 2:21-2:23 PM becomes 2:20-2:25 PM.\n\nPrevent Overlap: Ensure next task starts exactly when previous task ends.")
                                    }
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Round Times")
                            Switch(checked = autoRoundTimes, onCheckedChange = { autoRoundTimes = it })
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Prevent Overlap")
                            Switch(checked = preventOverlap, onCheckedChange = { preventOverlap = it })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(settings.copy(
                    roundingInterval = interval, 
                    roundUp = roundUp, 
                    useRounding = useRounding, 
                    theme = theme,
                    autoRoundTimes = autoRoundTimes,
                    preventOverlap = preventOverlap
                ))
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TITAMADialog(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = content,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TodayScreenPreview() {
    TITAMATheme {
       // TodayScreen(modifier = Modifier.fillMaxSize(), viewModel = MainViewModel())
    }
}
