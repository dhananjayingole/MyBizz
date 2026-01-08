package eu.tutorials.mybizz.Report

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BillsMonthlyReportScreen(viewModel: BillsMonthlyReportViewModel) {
    val report by viewModel.report.collectAsState()

    LaunchedEffect(Unit) {
        val now = java.time.LocalDate.now()
        viewModel.load(now.monthValue, now.year)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            Column {
                // Header
                Text(
                    text = "Monthly Financial Report",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${getMonthName(report?.month ?: 1)} ${report?.year ?: 2026}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (report == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading report...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Column
                }

                report?.let { reportData ->
                    // Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryCard(
                            title = "Total Bills",
                            value = reportData.totalBills.toString(),
                            icon = "📄",
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryCard(
                            title = "Paid",
                            value = reportData.paidCount.toString(),
                            icon = "✅",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )

                        SummaryCard(
                            title = "Pending",
                            value = reportData.unpaidCount.toString(),
                            icon = "⏳",
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Amount Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AmountCard(
                            title = "Paid Amount",
                            amount = reportData.totalPaidAmount,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )

                        AmountCard(
                            title = "Pending Amount",
                            amount = reportData.totalUnpaidAmount,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Charts Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Payment Status Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Paid vs Unpaid Pie Chart
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                PaymentStatusPieChart(
                                    paidAmount = reportData.totalPaidAmount,
                                    unpaidAmount = reportData.totalUnpaidAmount,
                                    modifier = Modifier.size(220.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                LegendItem(
                                    color = Color(0xFF4CAF50),
                                    label = "Paid: ₹${formatAmount(reportData.totalPaidAmount)}",
                                    percentage = if (reportData.totalPaidAmount + reportData.totalUnpaidAmount > 0) {
                                        (reportData.totalPaidAmount / (reportData.totalPaidAmount + reportData.totalUnpaidAmount) * 100).toInt()
                                    } else 0
                                )

                                LegendItem(
                                    color = Color(0xFFFF9800),
                                    label = "Pending: ₹${formatAmount(reportData.totalUnpaidAmount)}",
                                    percentage = if (reportData.totalPaidAmount + reportData.totalUnpaidAmount > 0) {
                                        (reportData.totalUnpaidAmount / (reportData.totalPaidAmount + reportData.totalUnpaidAmount) * 100).toInt()
                                    } else 0
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Category Distribution
                    if (reportData.categoryTotals.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Spending by Category",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Category Bars
                                reportData.categoryTotals.forEach { (category, amount) ->
                                    CategoryBar(
                                        category = category,
                                        amount = amount,
                                        total = reportData.totalPaidAmount,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AmountCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "₹${formatAmount(amount)}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@Composable
fun PaymentStatusPieChart(
    paidAmount: Double,
    unpaidAmount: Double,
    modifier: Modifier = Modifier
) {
    val total = paidAmount + unpaidAmount
    val paidPercentage = if (total > 0) paidAmount / total else 0.5
    val unpaidPercentage = if (total > 0) unpaidAmount / total else 0.5

    val paidColor = Color(0xFF4CAF50)
    val unpaidColor = Color(0xFFFF9800)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2f
            val centerOffset = Offset(canvasSize / 2, canvasSize / 2)

            var startAngle = -90f

            // Draw paid section
            val paidSweep = 360f * paidPercentage.toFloat()
            drawArc(
                color = paidColor,
                startAngle = startAngle,
                sweepAngle = paidSweep,
                useCenter = true,
                size = Size(canvasSize, canvasSize)
            )
            startAngle += paidSweep

            // Draw unpaid section
            val unpaidSweep = 360f * unpaidPercentage.toFloat()
            drawArc(
                color = unpaidColor,
                startAngle = startAngle,
                sweepAngle = unpaidSweep,
                useCenter = true,
                size = Size(canvasSize, canvasSize)
            )

            // Draw center circle for donut effect
            drawCircle(
                color = Color.Black,
                radius = radius * 0.5f,
                center = centerOffset
            )

            // Draw percentage text in center
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${(paidPercentage * 100).toInt()}%",
                    centerOffset.x - 20,
                    centerOffset.y + 10,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#4CAF50")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
            }
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Paid",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "${(paidPercentage * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    percentage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryBar(
    category: String,
    amount: Double,
    total: Double,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) amount / total else 0.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "₹${formatAmount(amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = percentage.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${(amount / total * 100).toInt()}% of total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(amount)
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month $month"
    }
}