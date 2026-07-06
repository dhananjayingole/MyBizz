// UserDashboardScreen.kt - User Dashboard
package eu.tutorials.mybizz.UIScreens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    navController: NavController,
    authRepo: AuthRepository,
    plotSheetsRepo: PlotSheetsRepository,
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.user_dashboard),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
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
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.Primary)
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
                                Icon(Icons.Default.Info, contentDescription = null, tint = AppColors.Primary)
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
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = AppColors.Primary)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ChatScreen) },
                containerColor = AppColors.Accent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    Icons.Default.MailOutline,
                    contentDescription = stringResource(R.string.chatbot),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = AppDimens.ScreenPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {

            // ── 1. Welcome header ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                WelcomeHeaderCard()
                Spacer(modifier = Modifier.height(AppDimens.SectionGap))
            }

            // ── 2. Plot Ad Poster (most recent plot from Sheet) ──────────────
            item {
                PlotAdBannerSection(
                    sheetsRepo = plotSheetsRepo,
                    onViewAllPlots = { navController.navigate(Routes.PlotListScreen) }
                )
                Spacer(modifier = Modifier.height(AppDimens.SectionGap + 4.dp))
            }

            // ── 3. "Available Options" heading ───────────────────────────────
            item {
                SectionHeading(
                    title = stringResource(R.string.available_options),
                    subtitle = "Everything you need, in one place"
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── 4. Menu items 2-column grid ──────────────────────────────────
            item {
                val rowCount = (menuItems.size + 1) / 2
                val gridHeight = (rowCount * 118).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false,
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

    // ── PDF Generation Dialog ─────────────────────────────────────────────
    if (showPdfDialog) {
        MonthlyPdfDialog(
            isGeneratingPdf = isGeneratingPdf,
            monthlyReport = monthlyReport,
            selectedMonth = selectedMonth,
            includesList = listOf(
                "Your Bills Summary",
                "Tasks Overview",
                "Rental Information",
                "Financial Overview"
            ),
            onDismiss = { if (!isGeneratingPdf) showPdfDialog = false },
            onGenerate = {
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
            }
        )
    }
}

/**
 * Header card with a subtle brand gradient — replaces the flat surfaceVariant
 * welcome card with something that reads as "home base" for the app.
 */
@Composable
fun WelcomeHeaderCard() {
    val today = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Card)
            .background(
                Brush.linearGradient(
                    colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = today,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.welcome_user),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Here's what's happening with your business today.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Reusable section heading used across dashboards. */
@Composable
fun SectionHeading(title: String, subtitle: String? = null) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Shared "generate monthly PDF" dialog used by both the user and admin
 * dashboards, so the two stay visually identical.
 */
@Composable
fun MonthlyPdfDialog(
    isGeneratingPdf: Boolean,
    monthlyReport: eu.tutorials.mybizz.Reporting.MonthlyReport?,
    selectedMonth: String,
    includesList: List<String>,
    onDismiss: () -> Unit,
    onGenerate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.InfoBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = AppColors.Primary
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.generate_monthly_pdf),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (isGeneratingPdf) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = AppColors.Accent
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.generating_pdf),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.may_take_seconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                } else {
                    Column {
                        if (monthlyReport != null) {
                            Surface(
                                shape = AppShapes.CardSmall,
                                color = AppColors.SurfaceMuted,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        stringResource(R.string.report_details),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ReportStatLine("Month", selectedMonth)
                                    ReportStatLine("Bills", monthlyReport.billsSummary.totalCount.toString())
                                    ReportStatLine("Rentals", monthlyReport.rentalsSummary.totalCount.toString())
                                    ReportStatLine("Tasks", monthlyReport.tasksSummary.totalCount.toString())
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Text(
                            stringResource(R.string.pdf_includes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column {
                            includesList.forEach { line ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppColors.Success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(line, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        if (monthlyReport == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.loading_report_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Danger
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGenerate,
                enabled = !isGeneratingPdf && monthlyReport != null,
                shape = AppShapes.Button,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.generate_pdf))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGeneratingPdf
            ) {
                Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun ReportStatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}