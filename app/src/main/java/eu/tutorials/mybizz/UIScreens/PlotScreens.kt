package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import eu.tutorials.mybizz.Logic.plot.PlotRepository
import eu.tutorials.mybizz.Logic.plot.PlotSheetsRepository
import eu.tutorials.mybizz.Model.Plot
import eu.tutorials.mybizz.R
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plot_management), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClicked,
                containerColor = AppColors.Accent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_plot))
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
                label = { Text(stringResource(R.string.search_plots)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = AppColors.TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.CardSmall,
                colors = mybizzFieldColors()
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

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.CardSmall,
        color = AppColors.Primary
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
            color = Color.White,
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
        shape = AppShapes.CardSmall,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Border)
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
                    if (asking >= initial) stringResource(R.string.status_positive) else stringResource(R.string.status_negotiate)
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
            maxLines = 2,
            color = AppColors.TextPrimary
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
            stringResource(R.string.status_positive) -> AppColors.SuccessBg to AppColors.Success
            stringResource(R.string.status_negotiate) -> AppColors.DangerBg to AppColors.Danger
            else -> AppColors.SurfaceMuted to AppColors.TextSecondary
        }

        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier
                .clip(AppShapes.Chip)
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

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_plot), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            visitDate = date
                        }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FormSectionCard(title = stringResource(R.string.plot_info)) {
                    OutlinedTextField(value = plotName, onValueChange = { plotName = it }, label = { Text(stringResource(R.string.plot_name)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = plotId, onValueChange = { plotId = it }, label = { Text(stringResource(R.string.plot_id)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.location)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = plotSize, onValueChange = { plotSize = it }, label = { Text(stringResource(R.string.plot_size)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = initialPrice, onValueChange = { initialPrice = it }, label = { Text(stringResource(R.string.initial_price)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors(), prefix = { Text("₹") })
                }
            }

            item {
                FormSectionCard(title = stringResource(R.string.visitor_info)) {
                    OutlinedTextField(value = visitorName, onValueChange = { visitorName = it }, label = { Text(stringResource(R.string.visitor_name)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = visitorNumber, onValueChange = { visitorNumber = it }, label = { Text(stringResource(R.string.visitor_phone)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = visitorAddress,
                        onValueChange = { visitorAddress = it },
                        label = { Text(stringResource(R.string.visitor_address)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = askingAmount, onValueChange = { askingAmount = it }, label = { Text(stringResource(R.string.asking_amount)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors(), prefix = { Text("₹") })
                }
            }

            item {
                FormSectionCard(title = stringResource(R.string.visit_details)) {
                    OutlinedTextField(value = attendedBy, onValueChange = { attendedBy = it }, label = { Text(stringResource(R.string.attended_by)) }, modifier = Modifier.fillMaxWidth(), shape = AppShapes.CardSmall, colors = mybizzFieldColors())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = visitDate,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.visit_date)) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.visit_date), tint = AppColors.Primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.notes_remarks)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = AppShapes.Button,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    enabled = plotName.isNotEmpty() && plotId.isNotEmpty() && location.isNotEmpty() &&
                            visitorName.isNotEmpty() && visitorNumber.isNotEmpty() &&
                            askingAmount.isNotEmpty() && attendedBy.isNotEmpty() &&
                            initialPrice.isNotEmpty() && plotSize.isNotEmpty()
                ) {
                    Text(stringResource(R.string.save_plot), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(plot.plotName, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Card,
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Plot Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow("Plot Name:", plot.plotName)
                        InfoRow("Plot ID:", plot.plotId)
                        InfoRow("Location:", plot.location)
                        InfoRow("Plot Size:", "${plot.plotSize} sq. ft.")
                        InfoRow("Initial Price:", "₹${plot.initialPrice}", showDivider = false)
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Card,
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Visitor Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow("Visitor Name:", plot.visitorName)
                        InfoRow("Phone Number:", plot.visitorNumber)
                        InfoRow("Address:", plot.visitorAddress)
                        InfoRow("Asking Amount:", "₹${plot.askingAmount}", showDivider = false)
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Card,
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.visit_details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow(stringResource(R.string.attended_by), plot.attendedBy)
                        InfoRow(stringResource(R.string.visit_date), plot.visitDate)
                        InfoRow(stringResource(R.string.construction_notes), plot.notes, showDivider = false)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onDelete(plot) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = AppShapes.Button,
                        border = BorderStroke(1.dp, AppColors.Danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete))
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_plot), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            visitDate = date
                        }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimens.ScreenPadding)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = AppShapes.Button,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    enabled = plotName.isNotEmpty() && plotId.isNotEmpty() && location.isNotEmpty() &&
                            visitorName.isNotEmpty() && visitorNumber.isNotEmpty() &&
                            askingAmount.isNotEmpty() && attendedBy.isNotEmpty() &&
                            initialPrice.isNotEmpty() && plotSize.isNotEmpty()
                ) {
                    Text(stringResource(R.string.update_plot), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}