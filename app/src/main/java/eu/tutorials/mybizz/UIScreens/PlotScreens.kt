package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import eu.tutorials.mybizz.Logic.plot.PlotRepository
import eu.tutorials.mybizz.Logic.plot.PlotSheetsRepository
import eu.tutorials.mybizz.Model.Plot
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlotListScreen(
    navController: NavHostController,
    sheetsRepo: PlotSheetsRepository,
    onAddClicked: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = PlotRepository()

    var plots by remember { mutableStateOf<List<Plot>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            plots = repository.getAllPlots(sheetsRepo)
        }
    }

    val filteredList = plots.filter {
        it.plotName.contains(searchQuery, ignoreCase = true) ||
                it.visitorName.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true)
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Plot Management") },
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
                Icon(Icons.Default.Add, contentDescription = "Add Plot", tint = MaterialTheme.colorScheme.onPrimary)
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
                label = { Text("Search Plots, Visitors, or Locations") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Table Header - Fixed and scrollable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                PlotTableHeader()
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filteredList) { plot ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ){
                        PlotTableRow(
                            plot = plot,
                            onClick = {
                                navController.navigate("plotdetailscreen/${plot.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlotTableHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TableHeaderCell("Plot Name", 120.dp)
            TableHeaderCell("Location", 120.dp)
            TableHeaderCell("Visitor", 100.dp)
            TableHeaderCell("Contact", 100.dp)
            TableHeaderCell("Asking Amt", 90.dp)
            TableHeaderCell("Initial Price", 90.dp)
            TableHeaderCell("Plot Size", 80.dp)
            TableHeaderCell("Attended By", 100.dp)
            TableHeaderCell("Status", 80.dp)
        }
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlotTableRow(plot: Plot, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TableCell(plot.plotName, 120.dp)
            TableCell(plot.location, 120.dp)
            TableCell(plot.visitorName, 100.dp)
            TableCell(plot.visitorNumber, 100.dp)
            TableCell("₹${plot.askingAmount}", 90.dp)
            TableCell("₹${plot.initialPrice}", 90.dp)
            TableCell(plot.plotSize, 80.dp)
            TableCell(plot.attendedBy, 100.dp)
            StatusCell(
                status = if (plot.askingAmount.isNotEmpty() && plot.initialPrice.isNotEmpty()) {
                    val asking = plot.askingAmount.toDoubleOrNull() ?: 0.0
                    val initial = plot.initialPrice.toDoubleOrNull() ?: 0.0
                    if (asking >= initial) "Positive" else "Negotiate"
                } else "New",
                width = 80.dp
            )
        }
    }
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun StatusCell(status: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val (backgroundColor, textColor) = when (status) {
            "Positive" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            "Negotiate" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlotScreen(
    sheetsRepo: PlotSheetsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = PlotRepository()

    var plotName by remember { mutableStateOf("") }
    var plotId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var visitorName by remember { mutableStateOf("") }
    var visitorNumber by remember { mutableStateOf("") }
    var visitorAddress by remember { mutableStateOf("") }
    var askingAmount by remember { mutableStateOf("") }
    var attendedBy by remember { mutableStateOf("") }
    var initialPrice by remember { mutableStateOf("") }
    var plotSize by remember { mutableStateOf("") }
    var visitDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // For Date Picker
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Plot Visit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            visitDate = date
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Plot Information", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = plotName,
                    onValueChange = { plotName = it },
                    label = { Text("Plot Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = plotId,
                    onValueChange = { plotId = it },
                    label = { Text("Plot ID *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = plotSize,
                    onValueChange = { plotSize = it },
                    label = { Text("Plot Size (sq. ft.) *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = initialPrice,
                    onValueChange = { initialPrice = it },
                    label = { Text("Initial Price (₹) *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Visitor Information", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = visitorName,
                    onValueChange = { visitorName = it },
                    label = { Text("Visitor Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = visitorNumber,
                    onValueChange = { visitorNumber = it },
                    label = { Text("Visitor Phone *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = visitorAddress,
                    onValueChange = { visitorAddress = it },
                    label = { Text("Visitor Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
            item {
                OutlinedTextField(
                    value = askingAmount,
                    onValueChange = { askingAmount = it },
                    label = { Text("Asking Amount (₹) *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Visit Details", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = attendedBy,
                    onValueChange = { attendedBy = it },
                    label = { Text("Attended By *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = visitDate,
                    onValueChange = {},
                    label = { Text("Visit Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Visit Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val plot = Plot(
                                id = UUID.randomUUID().toString(),
                                plotName = plotName,
                                plotId = plotId,
                                location = location,
                                visitorName = visitorName,
                                visitorNumber = visitorNumber,
                                visitorAddress = visitorAddress,
                                askingAmount = askingAmount,
                                attendedBy = attendedBy,
                                initialPrice = initialPrice,
                                plotSize = plotSize,
                                visitDate = visitDate,
                                notes = notes
                            )
                            repository.addPlot(plot, sheetsRepo)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = plotName.isNotEmpty() && plotId.isNotEmpty() && location.isNotEmpty() &&
                            visitorName.isNotEmpty() && visitorNumber.isNotEmpty() &&
                            askingAmount.isNotEmpty() && attendedBy.isNotEmpty() &&
                            initialPrice.isNotEmpty() && plotSize.isNotEmpty()
                ) {
                    Text("Save Plot Visit")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotDetailScreen(
    plot: Plot,
    onEdit: (Plot) -> Unit,
    onDelete: (Plot) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(plot.plotName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Plot Information", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Plot Name:", plot.plotName)
                        InfoRow("Plot ID:", plot.plotId)
                        InfoRow("Location:", plot.location)
                        InfoRow("Plot Size:", "${plot.plotSize} sq. ft.")
                        InfoRow("Initial Price:", "₹${plot.initialPrice}")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Visitor Information", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Visitor Name:", plot.visitorName)
                        InfoRow("Phone Number:", plot.visitorNumber)
                        InfoRow("Address:", plot.visitorAddress)
                        InfoRow("Asking Amount:", "₹${plot.askingAmount}")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Visit Details", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Attended By:", plot.attendedBy)
                        InfoRow("Visit Date:", plot.visitDate)
                        InfoRow("Notes:", plot.notes)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                    Button(
//                        onClick = { onEdit(plot) },
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Edit")
//                    }
                    OutlinedButton(
                        onClick = { onDelete(plot) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

// Edit Plot Screen (similar to Add but with existing data)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlotScreen(
    sheetsRepo: PlotSheetsRepository,
    existing: Plot,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = PlotRepository()

    var plotName by remember { mutableStateOf(existing.plotName) }
    var plotId by remember { mutableStateOf(existing.plotId) }
    var location by remember { mutableStateOf(existing.location) }
    var visitorName by remember { mutableStateOf(existing.visitorName) }
    var visitorNumber by remember { mutableStateOf(existing.visitorNumber) }
    var visitorAddress by remember { mutableStateOf(existing.visitorAddress) }
    var askingAmount by remember { mutableStateOf(existing.askingAmount) }
    var attendedBy by remember { mutableStateOf(existing.attendedBy) }
    var initialPrice by remember { mutableStateOf(existing.initialPrice) }
    var plotSize by remember { mutableStateOf(existing.plotSize) }
    var visitDate by remember { mutableStateOf(existing.visitDate) }
    var notes by remember { mutableStateOf(existing.notes) }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Plot Visit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            visitDate = date
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ... (Same form fields as AddPlotScreen but with existing values)
            // Implementation similar to AddPlotScreen but with update logic
            item {
                Button(
                    onClick = {
                        scope.launch {
                            val updatedPlot = existing.copy(
                                plotName = plotName,
                                plotId = plotId,
                                location = location,
                                visitorName = visitorName,
                                visitorNumber = visitorNumber,
                                visitorAddress = visitorAddress,
                                askingAmount = askingAmount,
                                attendedBy = attendedBy,
                                initialPrice = initialPrice,
                                plotSize = plotSize,
                                visitDate = visitDate,
                                notes = notes
                            )
                            repository.updatePlot(updatedPlot, sheetsRepo)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = plotName.isNotEmpty() && plotId.isNotEmpty() && location.isNotEmpty() &&
                            visitorName.isNotEmpty() && visitorNumber.isNotEmpty() &&
                            askingAmount.isNotEmpty() && attendedBy.isNotEmpty() &&
                            initialPrice.isNotEmpty() && plotSize.isNotEmpty()
                ) {
                    Text("Update Plot Visit")
                }
            }
        }
    }
}