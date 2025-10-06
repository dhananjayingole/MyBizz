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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import java.util.UUID

@Composable
fun TaskListScreen(
    navController: NavHostController,
    sheetsRepo: TaskSheetsRepository
) {
    val scope = rememberCoroutineScope()
    val repository = TaskRepository()

    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            tasks = repository.getAllTasks(sheetsRepo)
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Routes.AddTaskScreen)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Tasks", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks found", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // CORRECTED: Use proper route format
                                    navController.navigate("task_detail/${task.id}")
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(task.title, style = MaterialTheme.typography.titleMedium)
                                Text("Assigned to: ${task.assignedTo}", style = MaterialTheme.typography.bodySmall)
                                Text("Due: ${task.dueDate}", style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Add New Task", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title *") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = assignedTo,
            onValueChange = { assignedTo = it },
            label = { Text("Assigned To *") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status Dropdown - FIXED
        var statusExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                label = { Text("Status") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Status")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { statusExpanded = true }
            )
            DropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                statusOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            status = option
                            statusExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = { Text("Due Date (YYYY-MM-DD)") },
            placeholder = { Text("2024-12-31") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Cancel")
            }

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
                enabled = title.isNotEmpty() && assignedTo.isNotEmpty()
            ) {
                Text("Save Task")
            }
        }
    }
}

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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Task not found")
        }
    } else {
        TaskDetailContent(
            task = task!!,
            onEdit = {
                // CORRECTED: Use proper route format
                navController.navigate("edit_task/${task!!.id}")
            },
            onDelete = {
                scope.launch {
                    repository.deleteTask(task!!.id, sheetsRepo)
                    navController.popBackStack()
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun TaskDetailContent(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(task.title, style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Task Details
        DetailItem(label = "Description", value = task.description)
        DetailItem(label = "Assigned To", value = task.assignedTo)
        DetailItem(label = "Due Date", value = task.dueDate)
        DetailItem(label = "Notes", value = task.notes)

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Back")
        }
    }
}

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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Task not found")
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Edit Task", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = assignedTo,
                onValueChange = { assignedTo = it },
                label = { Text("Assigned To *") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status Dropdown - FIXED
            var statusExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Status")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { statusExpanded = true }
                )
                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                status = option
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Due Date (YYYY-MM-DD)") },
                placeholder = { Text("2024-12-31") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel")
                }

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
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}