// UIScreens/TaskScreen.kt
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import eu.tutorials.mybizz.Logic.Task.TaskRepository
import eu.tutorials.mybizz.Logic.Task.TaskSheetsRepository
import eu.tutorials.mybizz.Model.Task
import eu.tutorials.mybizz.Navigation.Routes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavHostController,
    sheetsRepo: TaskSheetsRepository
) {
    val scope = rememberCoroutineScope()
    val repository = TaskRepository()

    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            tasks = repository.getAllTasks(sheetsRepo)
            isLoading = false
        }
    }

    val filteredTasks = tasks.filter { task ->
        task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true) ||
                task.assignedTo.contains(searchQuery, ignoreCase = true) ||
                task.status.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tasks") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Routes.AddTaskScreen)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Tasks") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "No tasks found for '$searchQuery'"
                        else "No tasks found",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("task_detail/${task.id}")
                                },
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(task.title, style = MaterialTheme.typography.titleMedium)
                                Text("👤 ${task.assignedTo}")
                                Text("📅 Due: ${task.dueDate}")
                                Text("🔄 Status: ${task.status}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    navController: NavHostController,
    sheetsRepo: TaskSheetsRepository
) {
    val scope = rememberCoroutineScope()
    val repository = TaskRepository()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Pending") }
    var notes by remember { mutableStateOf("") }

    val statusOptions = listOf("Pending", "In Progress", "Completed")
    var expanded by remember { mutableStateOf(false) }

    // For Date Picker
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add New Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        millis?.let {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                            dueDate = date
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                singleLine = false
            )

            OutlinedTextField(
                value = assignedTo,
                onValueChange = { assignedTo = it },
                label = { Text("Assigned To *") },
                modifier = Modifier.fillMaxWidth()
            )

            // Status Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                status = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Due Date with Date Picker
            OutlinedTextField(
                value = dueDate,
                onValueChange = {},
                label = { Text("Due Date") },
                readOnly = true,
                placeholder = { Text("YYYY-MM-DD") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Due Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotEmpty() && assignedTo.isNotEmpty()) {
                        scope.launch {
                            val task = Task(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                description = description,
                                assignedTo = assignedTo,
                                dueDate = dueDate,
                                status = status,
                                notes = notes
                            )
                            repository.addTask(task, sheetsRepo)
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotEmpty() && assignedTo.isNotEmpty()
            ) {
                Text("Save Task")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavHostController,
    taskId: String,
    sheetsRepo: TaskSheetsRepository
) {
    val scope = rememberCoroutineScope()
    val repository = TaskRepository()

    var task by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(taskId) {
        scope.launch {
            val allTasks = repository.getAllTasks(sheetsRepo)
            task = allTasks.find { it.id == taskId }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(task?.title ?: "Task Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (task == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found")
            }
        } else {
            TaskDetailContent(
                task = task!!,
                onEdit = {
                    navController.navigate("edit_task/${task!!.id}")
                },
                onDelete = {
                    scope.launch {
                        repository.deleteTask(task!!.id, sheetsRepo)
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun TaskDetailContent(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status Badge
        Box(
            modifier = Modifier
                .background(
                    color = when (task.status) {
                        "Completed" -> Color.Green.copy(alpha = 0.2f)
                        "In Progress" -> Color.Blue.copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(task.status, color = when (task.status) {
                "Completed" -> Color.Green
                "In Progress" -> Color.Blue
                else -> Color.Gray
            })
        }

        // Task Details
        DetailItem(label = "Description", value = task.description)
        DetailItem(label = "Assigned To", value = task.assignedTo)
        DetailItem(label = "Due Date", value = task.dueDate)
        DetailItem(label = "Notes", value = task.notes)

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text("Delete")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Back")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavHostController,
    taskId: String,
    sheetsRepo: TaskSheetsRepository
) {
    val scope = rememberCoroutineScope()
    val repository = TaskRepository()

    var task by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Pending") }
    var notes by remember { mutableStateOf("") }

    val statusOptions = listOf("Pending", "In Progress", "Completed")
    var expanded by remember { mutableStateOf(false) }

    // For Date Picker
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        scope.launch {
            val allTasks = repository.getAllTasks(sheetsRepo)
            task = allTasks.find { it.id == taskId }
            task?.let {
                title = it.title
                description = it.description
                assignedTo = it.assignedTo
                dueDate = it.dueDate
                status = it.status
                notes = it.notes
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        millis?.let {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                            dueDate = date
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (task == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false
                )

                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text("Assigned To *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Due Date with Date Picker
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {},
                    label = { Text("Due Date") },
                    readOnly = true,
                    placeholder = { Text("YYYY-MM-DD") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Due Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isNotEmpty() && assignedTo.isNotEmpty()) {
                            scope.launch {
                                val updatedTask = Task(
                                    id = taskId,
                                    title = title,
                                    description = description,
                                    assignedTo = assignedTo,
                                    dueDate = dueDate,
                                    status = status,
                                    notes = notes
                                )
                                repository.updateTask(updatedTask, sheetsRepo)
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotEmpty() && assignedTo.isNotEmpty()
                ) {
                    Text("Update Task")
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    if (value.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}