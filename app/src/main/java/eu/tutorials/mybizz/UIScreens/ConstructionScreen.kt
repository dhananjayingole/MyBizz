package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.tutorials.mybizz.Model.Construction
import eu.tutorials.mybizz.Logic.Construction.ConstructionSheetsRepository
import eu.tutorials.mybizz.Logic.Construction.ConstructionRepository
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import eu.tutorials.mybizz.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructionListScreen(
    navController: NavHostController,
    sheetsRepo: ConstructionSheetsRepository,
    onAddClicked: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()

    var constructions by remember { mutableStateOf<List<Construction>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            constructions = repository.getAllConstructions(sheetsRepo)
        }
    }

    val filteredList = constructions.filter {
        it.projectName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.construction_projects)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClicked,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_construction), tint = MaterialTheme.colorScheme.onPrimary)
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
                label = { Text(stringResource(R.string.search_project)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_project)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList) { construction ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("construction_detail/${construction.id}")
                            },
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(construction.projectName, style = MaterialTheme.typography.titleMedium)
                            Text("📍 ${construction.location}")
                            Text("🛠 Status: ${construction.status}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConstructionScreen(
    sheetsRepo: ConstructionSheetsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()

    var projectName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("In Progress") }
    var notes by remember { mutableStateOf("") }

    // For dropdown
    val statusOptions = listOf("In Progress", "Started", "Done")
    var expanded by remember { mutableStateOf(false) }

    // For Date Picker
    val datePickerStateStart = rememberDatePickerState()
    val datePickerStateEnd = rememberDatePickerState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_construction)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->

        if (showStartPicker) {
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerStateStart.selectedDateMillis
                        millis?.let {
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                            startDate = date
                        }
                        showStartPicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerStateStart)
            }
        }

        if (showEndPicker) {
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerStateEnd.selectedDateMillis
                        millis?.let {
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                            endDate = date
                        }
                        showEndPicker = false
                    }) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(state = datePickerStateEnd)
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text(stringResource(R.string.project_name)) })
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.location)) })

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                label = { Text(stringResource(R.string.start_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.construction_start))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = {},
                label = { Text(stringResource(R.string.end_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showEndPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.construction_end))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text(stringResource(R.string.construction_cost)) })

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

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) })

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val construction = Construction(
                            id = UUID.randomUUID().toString(),
                            projectName = projectName,
                            location = location,
                            startDate = startDate,
                            endDate = endDate,
                            cost = cost,
                            status = status,
                            notes = notes
                        )
                        repository.addConstruction(construction, sheetsRepo)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_project))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructionDetailScreen(
    construction: Construction,
    onEdit: (Construction) -> Unit,
    onDelete: (Construction) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(construction.projectName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📍 Location: ${construction.location}", style = MaterialTheme.typography.bodyLarge)
            Text("📅 Start Date: ${construction.startDate}")
            Text("📅 End Date: ${construction.endDate}")
            Text("💰 Cost: ${construction.cost}")
            Text("🛠 Status: ${construction.status}")
            Text("📝 Notes: ${construction.notes}")

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onEdit(construction) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.edit))
                }
                OutlinedButton(onClick = { onDelete(construction) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditConstructionScreen(
    sheetsRepo: ConstructionSheetsRepository,
    existing: Construction,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()

    var projectName by remember { mutableStateOf(existing.projectName) }
    var location by remember { mutableStateOf(existing.location) }
    var startDate by remember { mutableStateOf(existing.startDate) }
    var endDate by remember { mutableStateOf(existing.endDate) }
    var cost by remember { mutableStateOf(existing.cost) }
    var status by remember { mutableStateOf(existing.status) }
    var notes by remember { mutableStateOf(existing.notes ?: "") }

    val statusOptions = listOf("In Progress", "Started", "Done")
    var expanded by remember { mutableStateOf(false) }

    val datePickerStateStart = rememberDatePickerState()
    val datePickerStateEnd = rememberDatePickerState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_project)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->

        if (showStartPicker) {
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerStateStart.selectedDateMillis
                        millis?.let {
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                            startDate = date
                        }
                        showStartPicker = false
                    }) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(state = datePickerStateStart)
            }
        }

        if (showEndPicker) {
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerStateEnd.selectedDateMillis
                        millis?.let {
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                            endDate = date
                        }
                        showEndPicker = false
                    }) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(state = datePickerStateEnd)
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text(stringResource(R.string.project_name)) })
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.location)) })

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                label = { Text(stringResource(R.string.start_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription =stringResource(R.string.construction_start))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = {},
                label = { Text(stringResource(R.string.end_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showEndPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.construction_end))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text(stringResource(R.string.construction_cost)) })

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.construction_status)) },
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

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) })

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val updated = existing.copy(
                            projectName = projectName,
                            location = location,
                            startDate = startDate,
                            endDate = endDate,
                            cost = cost,
                            status = status,
                            notes = notes
                        )
                        repository.updateConstruction(updated, sheetsRepo)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.update_project))
            }
        }
    }
}