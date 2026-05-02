// UserDashboardScreen.kt - User Dashboard
package eu.tutorials.mybizz.UIScreens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.plot.PlotSheetsRepository
import eu.tutorials.mybizz.Model.MenuItem
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R
import eu.tutorials.mybizz.Reporting.MonthlyReportViewModel
import eu.tutorials.mybizz.pdfgen.PdfGenerator
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    navController: NavController,
    authRepo: AuthRepository,
    plotSheetsRepo: PlotSheetsRepository,                    // ← NEW: pass from NavGraph
    reportViewModel: MonthlyReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val monthlyReport = reportViewModel.monthlyReport.observeAsState().value
    val selectedMonth = reportViewModel.selectedMonth.observeAsState("").value

    val menuItems = listOf(
        MenuItem("bills", context.getString(R.string.view_bills), Routes.BillsListScreen, R.drawable.img_13),
        MenuItem("settings", context.getString(R.string.setting), Routes.SettingScreen, R.drawable.img_16),
        MenuItem("RentalManagement", context.getString(R.string.rental_management), Routes.RentalListScreen, R.drawable.img_12),
        MenuItem("Task", context.getString(R.string.task), Routes.TaskListScreen, R.drawable.img_15),
        MenuItem("Construction", context.getString(R.string.plot_constructions), Routes.PlotAndConstructionEntry, R.drawable.img_10),
        MenuItem("Profile", context.getString(R.string.profile), Routes.ProfileScreen, R.drawable.img_11)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_dashboard)) },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.monthly_summary)) },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.MonthlyReportScreen)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.generate_pdf)) },
                            onClick = {
                                showMenu = false
                                showPdfDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Build, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.banking_sms)) },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.BankSMSScreen)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ChatScreen) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    Icons.Default.AccountBox,
                    contentDescription = stringResource(R.string.chatbot),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->

        // LazyColumn lets the poster + grid scroll as one page
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)   // clear the FAB
        ) {

            // ── 1. Welcome card ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.welcome_user),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── 2. Plot Ad Poster (most recent plot from Sheet) ──────────────
            item {
                PlotAdBannerSection(
                    sheetsRepo = plotSheetsRepo,
                    onViewAllPlots = { navController.navigate(Routes.PlotListScreen) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── 3. "Available Options" heading ───────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.available_options),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 4. Menu items 2-column grid ──────────────────────────────────
            // LazyVerticalGrid inside LazyColumn must have a fixed height.
            // 6 items / 2 cols = 3 rows × ~146dp ≈ 438dp
            item {
                val rowCount = (menuItems.size + 1) / 2
                val gridHeight = (rowCount * 146).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false,          // outer LazyColumn handles scroll
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    items(menuItems) { item ->
                        MenuItemCard(
                            menuItem = item,
                            onClick = { navController.navigate(item.route) }
                        )
                    }
                }
            }
        }
    }

    // ── PDF Generation Dialog (100% unchanged from original) ─────────────────
    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isGeneratingPdf) showPdfDialog = false
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
                    stringResource(R.string.generate_monthly_pdf),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    if (isGeneratingPdf) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.generating_pdf),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.may_take_seconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                                            stringResource(R.string.report_details),
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
                                stringResource(R.string.pdf_includes),
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
                                    stringResource(R.string.loading_report_data),
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
                                        context.startActivity(
                                            Intent.createChooser(intent, "Open PDF Report")
                                        )
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
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.generate_pdf))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPdfDialog = false },
                    enabled = !isGeneratingPdf
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}