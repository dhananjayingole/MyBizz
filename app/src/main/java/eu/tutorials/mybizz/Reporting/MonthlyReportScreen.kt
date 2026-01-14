package eu.tutorials.mybizz.Reporting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.Rental
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: MonthlyReportViewModel = viewModel(),
    navController: NavController
) {
    val monthlyReport = viewModel.monthlyReport.observeAsState().value
    val isLoading = viewModel.isLoading.observeAsState(false).value
    val errorMessage = viewModel.errorMessage.observeAsState().value
    val selectedMonth = viewModel.selectedMonth.observeAsState("").value

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Monthly Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                    }
                }
                errorMessage != null -> {
                    ErrorView(errorMessage = errorMessage)
                }
                monthlyReport != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Month Selector with elevated card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            MonthSelector(
                                selectedMonth = selectedMonth,
                                onPreviousMonth = { viewModel.loadPreviousMonth() },
                                onNextMonth = { viewModel.loadNextMonth() }
                            )
                        }

                        // Financial Overview Card
                        FinancialOverviewCard(monthlyReport)

                        // Tab Row
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = Color.White,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                        height = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == 0,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                    text = {
                                        Text(
                                            "Bills",
                                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 1,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                    text = {
                                        Text(
                                            "Rentals",
                                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 2,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                                    text = {
                                        Text(
                                            "Tasks",
                                            fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> BillsReportPage(monthlyReport.billsSummary)
                                1 -> RentalsReportPage(monthlyReport.rentalsSummary)
                                2 -> TasksReportPage(monthlyReport.tasksSummary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialOverviewCard(report: MonthlyReport) {
    val netAmount = report.getNetAmount()
    val isProfit = netAmount >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Income and Expenses Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinancialMetricCard(
                    label = "Income",
                    amount = report.getTotalIncome(),
                    color = Color(0xFF4CAF50),
                    icon = Icons.Default.KeyboardArrowUp
                )

                Spacer(modifier = Modifier.width(12.dp))

                FinancialMetricCard(
                    label = "Expenses",
                    amount = report.getTotalExpenses(),
                    color = Color(0xFFF44336),
                    icon = Icons.Default.KeyboardArrowDown
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // Net Profit/Loss Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isProfit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isProfit) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = if (isProfit) "Net Profit" else "Net Loss",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${String.format("%,.2f", kotlin.math.abs(netAmount))}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.FinancialMetricCard(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "₹${String.format("%,.2f", amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                "Previous Month",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = formatMonthDisplay(selectedMonth),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                "Next Month",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BillsReportPage(summary: MonthlyReportSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryCard(
                title = "Bills Summary",
                totalAmount = summary.totalAmount,
                paidAmount = summary.paidAmount,
                unpaidAmount = summary.unpaidAmount,
                totalCount = summary.totalCount,
                paidCount = summary.paidCount,
                unpaidCount = summary.unpaidCount,
                isExpense = true
            )
        }

        item {
            if (summary.totalAmount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Payment Status Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        PieChart(
                            data = listOf(
                                PieChartData("Paid", summary.paidAmount, Color(0xFFF44336)),
                                PieChartData("Unpaid", summary.unpaidAmount, Color(0xFFFF9800))
                            )
                        )
                    }
                }
            }
        }

        item {
            val billItems = summary.items.filterIsInstance<BillReportItem>()
            if (billItems.isNotEmpty()) {
                val categoryTotals = billItems
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                if (categoryTotals.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Expenses by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            SimpleBarChart(
                                data = categoryTotals,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }

        item {
            if (summary.items.isNotEmpty()) {
                BillsTable(summary.items.filterIsInstance<BillReportItem>())
            }
        }
    }
}

@Composable
fun RentalsReportPage(summary: MonthlyReportSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryCard(
                title = "Rentals Summary",
                totalAmount = summary.totalAmount,
                paidAmount = summary.paidAmount,
                unpaidAmount = summary.unpaidAmount,
                totalCount = summary.totalCount,
                paidCount = summary.paidCount,
                unpaidCount = summary.unpaidCount,
                isExpense = false
            )
        }

        item {
            if (summary.totalAmount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Collection Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        PieChart(
                            data = listOf(
                                PieChartData("Collected", summary.paidAmount, Color(0xFF4CAF50)),
                                PieChartData("Pending", summary.unpaidAmount, Color(0xFFFF9800))
                            )
                        )
                    }
                }
            }
        }

        item {
            val rentalItems = summary.items.filterIsInstance<RentalReportItem>()
            if (rentalItems.isNotEmpty()) {
                val propertyTotals = rentalItems
                    .groupBy { it.property }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                if (propertyTotals.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Income by Property",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            SimpleBarChart(
                                data = propertyTotals,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }

        item {
            if (summary.items.isNotEmpty()) {
                RentalsTable(summary.items.filterIsInstance<RentalReportItem>())
            }
        }
    }
}

@Composable
fun TasksReportPage(summary: MonthlyReportSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TaskSummaryCard(summary)
        }

        item {
            if (summary.totalCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Task Completion Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        val completionRate = (summary.paidCount.toFloat() / summary.totalCount.toFloat()) * 100

                        DonutChart(
                            percentage = completionRate,
                            completed = summary.paidCount,
                            total = summary.totalCount
                        )
                    }
                }
            }
        }

        item {
            if (summary.items.isNotEmpty()) {
                TasksTable(summary.items.filterIsInstance<TaskReportItem>())
            }
        }
    }
}

@Composable
fun BillsTable(items: List<BillReportItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderText("Bill #", Modifier.weight(0.8f))
                TableHeaderText("Title", Modifier.weight(1.8f))
                TableHeaderText("Category", Modifier.weight(1.3f))
                TableHeaderText("Amount", Modifier.weight(1f), TextAlign.End)
                TableHeaderText("Status", Modifier.weight(0.9f), TextAlign.Center)
            }

            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCellText(item.billNumber, Modifier.weight(0.8f))
                    TableCellText(item.title, Modifier.weight(1.8f), maxLines = 2)
                    TableCellText(item.category, Modifier.weight(1.3f))
                    TableCellText(
                        "₹${String.format("%,.2f", item.amount)}",
                        Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == Bill.STATUS_PAID) Color(0xFFF44336) else Color(0xFFFF9800)
                    )
                    Box(
                        modifier = Modifier.weight(0.9f),
                        contentAlignment = Alignment.Center
                    ) {
                        CompactStatusChip(item.status)
                    }
                }
                if (index < items.size - 1) {
                    Divider(thickness = 0.5.dp, color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

@Composable
fun RentalsTable(items: List<RentalReportItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderText("Tenant Name", Modifier.weight(2f))
                TableHeaderText("Property", Modifier.weight(1.8f))
                TableHeaderText("Amount", Modifier.weight(1.2f), TextAlign.End)
                TableHeaderText("Status", Modifier.weight(0.9f), TextAlign.Center)
            }

            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCellText(item.tenantName, Modifier.weight(2f), maxLines = 2)
                    TableCellText(item.property, Modifier.weight(1.8f), color = Color(0xFF666666), maxLines = 2)
                    TableCellText(
                        "₹${String.format("%,.2f", item.amount)}",
                        Modifier.weight(1.2f),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == Rental.STATUS_PAID) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Box(
                        modifier = Modifier.weight(0.9f),
                        contentAlignment = Alignment.Center
                    ) {
                        CompactStatusChip(item.status)
                    }
                }
                if (index < items.size - 1) {
                    Divider(thickness = 0.5.dp, color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

@Composable
fun TasksTable(items: List<TaskReportItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderText("Title", Modifier.weight(2.5f))
                TableHeaderText("Assigned To", Modifier.weight(1.5f))
                TableHeaderText("Due Date", Modifier.weight(1.3f))
                TableHeaderText("Status", Modifier.weight(1f), TextAlign.Center)
            }

            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCellText(item.title, Modifier.weight(2.5f), maxLines = 2)
                    TableCellText(item.assignedTo, Modifier.weight(1.5f), color = Color(0xFF666666))
                    TableCellText(item.dueDate, Modifier.weight(1.3f))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CompactStatusChip(item.status)
                    }
                }
                if (index < items.size - 1) {
                    Divider(thickness = 0.5.dp, color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

@Composable
fun TableHeaderText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A1A1A),
        modifier = modifier,
        textAlign = textAlign
    )
}

@Composable
fun TableCellText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color(0xFF1A1A1A)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        fontWeight = fontWeight,
        color = color
    )
}
@Composable
fun CompactStatusChip(status: String) {
    val backgroundColor = when (status.lowercase()) {
        "paid", "completed" -> Color(0xFF4CAF50)
        "unpaid", "pending" -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor.copy(alpha = 0.2f),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status.capitalize(Locale.ROOT),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}
data class PieChartData(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.value }
    if (total == 0.0) return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie Chart Canvas
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f

            data.forEach { slice ->
                val sweepAngle = ((slice.value / total) * 360).toFloat()

                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle
            }
        }

        // Legend
        Spacer(modifier = Modifier.height(16.dp))
        data.forEach { slice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${slice.label}: ₹${String.format("%.2f", slice.value)} (${String.format("%.1f", (slice.value / total) * 100)}%)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: Map<String, Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = data.values.maxOrNull() ?: 1.0

    Column(modifier = modifier) {
        data.forEach { (category, amount) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((amount / maxValue).toFloat())
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }

                Text(
                    text = "₹${String.format("%.0f", amount)}",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    percentage: Float,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 40f
            val radius = (size.minDimension - strokeWidth) / 2

            // Background circle
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )

            // Progress arc
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -90f,
                sweepAngle = (percentage / 100) * 360,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${String.format("%.1f", percentage)}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "$completed/$total",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    totalAmount: Double,
    paidAmount: Double,
    unpaidAmount: Double,
    totalCount: Int,
    paidCount: Int,
    unpaidCount: Int,
    isExpense: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountColumn("Total", totalAmount, Color.Gray)
                AmountColumn(
                    "Paid",
                    paidAmount,
                    if (isExpense) Color.Red else Color.Green
                )
                AmountColumn("Unpaid", unpaidAmount, Color(0xFFFF9800))
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CountColumn("Total", totalCount)
                CountColumn("Paid", paidCount)
                CountColumn("Unpaid", unpaidCount)
            }
        }
    }
}

@Composable
fun TaskSummaryCard(summary: MonthlyReportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tasks Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CountColumn("Total", summary.totalCount)
                CountColumn("Completed", summary.paidCount)
                CountColumn("Pending", summary.unpaidCount)
            }
        }
    }
}

@Composable
fun AmountColumn(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = "₹${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CountColumn(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ErrorView(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = Color.Red)
        }
    }
}

fun formatMonthDisplay(month: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(month)
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        displayFormat.format(date ?: Date())
    } catch (e: Exception) {
        month
    }
}
