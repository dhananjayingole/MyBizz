// UserDashboardScreen.kt - User Dashboard
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.content.Intent
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Model.MenuItem
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R
import eu.tutorials.mybizz.pdfgen.PdfGenerator
import eu.tutorials.mybizz.Reporting.MonthlyReportViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    navController: NavController,
    authRepo: AuthRepository,
    reportViewModel: MonthlyReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for menu and dialogs
    var showMenu by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Observe report data
    val monthlyReport = reportViewModel.monthlyReport.observeAsState().value
    val selectedMonth = reportViewModel.selectedMonth.observeAsState("").value

    val menuItems = listOf(
        MenuItem("bills", "View Bills", Routes.BillsListScreen, R.drawable.img_13),
        MenuItem("settings", "Settings", Routes.SettingScreen, R.drawable.img_16),
        MenuItem("RentalManagement", "Rental Management", Routes.RentalListScreen, R.drawable.img_12),
        MenuItem("Task", "Task", Routes.TaskListScreen, R.drawable.img_15),
        MenuItem("Construction", "Plot & Constructions", Routes.PlotAndConstructionEntry, R.drawable.img_10),
        MenuItem("Profile", "Profile", Routes.ProfileScreen, R.drawable.img_11)// Add a report icon
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Dashboard") },
                actions = {
                    // Three-dot menu button
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More Options"
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Monthly Summary option
                        DropdownMenuItem(
                            text = { Text("Monthly Summary") },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.MonthlyReportScreen)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Monthly Summary"
                                )
                            }
                        )

                        Divider()

                        // Generate PDF option
                        DropdownMenuItem(
                            text = { Text("Generate PDF Report") },
                            onClick = {
                                showMenu = false
                                showPdfDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = "Generate PDF"
                                )
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Welcome Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome, User!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Available Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menuItems) { item ->
                    MenuItemCard(
                        menuItem = item,
                        onClick = {
                            navController.navigate(item.route)
                        }
                    )
                }
            }
        }
    }

    // PDF Generation Dialog
    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isGeneratingPdf) {
                    showPdfDialog = false
                }
            },
            icon = {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Generate Monthly Report PDF",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    if (isGeneratingPdf) {
                        // Loading state
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Generating PDF report...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This may take a few seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Information state
                        Column {
                            if (monthlyReport != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Report Details",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("📅 Month: $selectedMonth")
                                        Text("💰 Bills: ${monthlyReport.billsSummary.totalCount}")
                                        Text("🏠 Rentals: ${monthlyReport.rentalsSummary.totalCount}")
                                        Text("✓ Tasks: ${monthlyReport.tasksSummary.totalCount}")
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text(
                                "This will generate a comprehensive PDF report including:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("• Your Bills Summary", style = MaterialTheme.typography.bodySmall)
                                Text("• Tasks Overview", style = MaterialTheme.typography.bodySmall)
                                Text("• Rental Information", style = MaterialTheme.typography.bodySmall)
                                Text("• Financial Overview", style = MaterialTheme.typography.bodySmall)
                            }

                            if (monthlyReport == null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Note: Loading report data...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isGeneratingPdf = true
                            monthlyReport?.let { report ->
                                val pdfGenerator = PdfGenerator(context)
                                val pdfFile = pdfGenerator.generateMonthlyReport(report, selectedMonth)

                                pdfFile?.let { file ->
                                    // Open PDF with system PDF viewer
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open PDF Report"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            isGeneratingPdf = false
                            showPdfDialog = false
                        }
                    },
                    enabled = !isGeneratingPdf && monthlyReport != null
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate PDF")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPdfDialog = false },
                    enabled = !isGeneratingPdf
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}