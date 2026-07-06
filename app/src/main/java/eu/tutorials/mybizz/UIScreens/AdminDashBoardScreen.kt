package eu.tutorials.mybizz.UIScreens

import androidx.compose.runtime.livedata.observeAsState
import eu.tutorials.mybizz.pdfgen.PdfGenerator
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Model.MenuItem
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R
import eu.tutorials.mybizz.Reporting.MonthlyReportViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
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
        MenuItem("bills", context.getString(R.string.bills_management), Routes.BillsListScreen, R.drawable.img_13),
        MenuItem("users", context.getString(R.string.user_management), Routes.UserManagementScreen, R.drawable.img_14),
        MenuItem("RentalManagement", context.getString(R.string.rental_management), Routes.RentalListScreen, R.drawable.img_12),
        MenuItem("Task", context.getString(R.string.task), Routes.TaskListScreen, R.drawable.img_15),
        MenuItem("Construction", context.getString(R.string.plot_constructions), Routes.PlotAndConstructionEntry, R.drawable.img_10),
        MenuItem("Profile", context.getString(R.string.profile), Routes.ProfileScreen, R.drawable.img_11),
        MenuItem("Setting", context.getString(R.string.setting), Routes.SettingScreen, R.drawable.img_16)
    )

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.PrimaryDark,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
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
                            text = { Text(stringResource(R.string.monthly_summary)) },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.MonthlyReportScreen)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = stringResource(R.string.monthly_summary),
                                    tint = AppColors.Primary
                                )
                            }
                        )

                        Divider()

                        // Generate PDF option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.generate_pdf)) },
                            onClick = {
                                showMenu = false
                                showPdfDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Generate PDF",
                                    tint = AppColors.Primary
                                )
                            }
                        )

                        Divider()
                        // Moves to the BankSMS Screen
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.banking_sms)) },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.BankSMSScreen)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Banking SMS",
                                    tint = AppColors.Primary
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
                .padding(AppDimens.ScreenPadding)
        ) {
            // Welcome Section — deep gradient card marks this clearly as the
            // admin surface (darker than the user dashboard's header).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.Card)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.PrimaryDark, AppColors.Primary)
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.welcome_admin),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.manage_business),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SectionGap))

            SectionHeading(
                title = stringResource(R.string.quick_actions),
                subtitle = "Manage every part of the business"
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { item ->
                    MenuItemCard(
                        menuItem = item,
                        onClick = {
                            when {
                                item.route.isNotEmpty() -> navController.navigate(item.route)
                                else -> {
                                    // Handle empty or invalid routes
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // PDF Generation Dialog (shared component, defined in UserDashboardScreen.kt)
    if (showPdfDialog) {
        MonthlyPdfDialog(
            isGeneratingPdf = isGeneratingPdf,
            monthlyReport = monthlyReport,
            selectedMonth = selectedMonth,
            includesList = listOf(
                "Financial Overview",
                "Bills Summary & Details",
                "Rentals Summary & Details",
                "Tasks Summary & Details"
            ),
            onDismiss = { if (!isGeneratingPdf) showPdfDialog = false },
            onGenerate = {
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
            }
        )
    }
}

/**
 * Menu tile used by both the user and admin dashboards.
 * Redesigned: icon sits in a soft tinted circle, title left-aligned,
 * chevron signals it's tappable — reads as a settings/navigation row
 * rather than a plain icon button.
 */
@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        shape = AppShapes.CardSmall,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.InfoBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = menuItem.icon),
                    contentDescription = menuItem.title,
                    modifier = Modifier.size(22.dp),
                    tint = androidx.compose.ui.graphics.Color.Unspecified
                )
            }
            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1
            )
        }
    }
}