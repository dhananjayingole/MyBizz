package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import eu.tutorials.mybizz.Logic.Task.TaskRepository
import eu.tutorials.mybizz.Logic.Task.TaskSheetsRepository
import eu.tutorials.mybizz.Model.Task
import eu.tutorials.mybizz.Navigation.Routes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import eu.tutorials.mybizz.R

private fun taskStatusColors(status: String): Pair<Color, Color> = when (status) {
    "Completed" -> AppColors.SuccessBg to AppColors.Success
    "In Progress" -> AppColors.InfoBg to AppColors.Primary
    else -> AppColors.SurfaceMuted to AppColors.TextSecondary
}

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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
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
                containerColor = AppColors.Accent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(AppDimens.ScreenPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search_tasks)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = AppColors.TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.CardSmall,
                colors = mybizzFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Accent)
                }
            } else if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "No tasks found for '$searchQuery'"
                            else stringResource(R.string.no_tasks_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredTasks) { task ->
                        val (bg, fg) = taskStatusColors(task.status)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("task_detail/${task.id}")
                                },
                            shape = AppShapes.CardSmall,
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                            border = BorderStroke(1.dp, AppColors.Border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(task.assignedTo, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Due: ${task.dueDate}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                    }
                                }
                                Surface(shape = AppShapes.Chip, color = bg) {
                                    Text(
                                        task.status,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = fg
                                    )
                                }
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

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_task), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
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
                    }) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize()
        ) {
            FormSectionCard(title = "Task details") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false,
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text(stringResource(R.string.assigned_to)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FormSectionCard(title = "Schedule & status") {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.status)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.due_date)) },
                    readOnly = true,
                    placeholder = { Text("YYYY-MM-DD") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Due Date", tint = AppColors.Primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    singleLine = false,
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppShapes.Button,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                enabled = title.isNotEmpty() && assignedTo.isNotEmpty()
            ) {
                Text(stringResource(R.string.save_task), fontWeight = FontWeight.SemiBold)
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(task?.title ?: stringResource(R.string.task_details), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
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
                CircularProgressIndicator(color = AppColors.Accent)
            }
        } else if (task == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.task_not_found), color = AppColors.TextSecondary)
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
    val (bg, fg) = taskStatusColors(task.status)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status Badge
        Surface(shape = AppShapes.Chip, color = bg) {
            Text(
                task.status,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.Card,
            color = AppColors.Surface,
            border = BorderStroke(1.dp, AppColors.Border)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                TaskDetailItem(label = stringResource(R.string.description), value = task.description)
                TaskDetailItem(label = stringResource(R.string.assigned_to), value = task.assignedTo)
                TaskDetailItem(label = stringResource(R.string.due_date), value = task.dueDate)
                TaskDetailItem(label = stringResource(R.string.notes), value = task.notes, showDivider = false)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = AppShapes.Button,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = AppShapes.Button,
                border = BorderStroke(1.dp, AppColors.Danger),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger)
            ) {
                Text(stringResource(R.string.delete))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = AppShapes.Button,
            border = BorderStroke(1.dp, AppColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
        ) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun TaskDetailItem(label: String, value: String, showDivider: Boolean = true) {
    if (value.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
            if (showDivider) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = AppColors.Border)
            }
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_task), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    }) { Text(stringResource(R.string.ok)) }
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
                CircularProgressIndicator(color = AppColors.Accent)
            }
        } else if (task == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.task_not_found), color = AppColors.TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(AppDimens.ScreenPadding)
                    .fillMaxSize()
            ) {
                FormSectionCard(title = "Task details") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.task_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        singleLine = false,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = assignedTo,
                        onValueChange = { assignedTo = it },
                        label = { Text(stringResource(R.string.assigned_to)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                FormSectionCard(title = "Schedule & status") {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.status)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = AppShapes.CardSmall,
                            colors = mybizzFieldColors()
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

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.due_date)) },
                        readOnly = true,
                        placeholder = { Text("YYYY-MM-DD") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_due_date), tint = AppColors.Primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.notes)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        singleLine = false,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = AppShapes.Button,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    enabled = title.isNotEmpty() && assignedTo.isNotEmpty()
                ) {
                    Text(stringResource(R.string.update_task), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    if (value.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
        }
    }
}