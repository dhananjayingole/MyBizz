package eu.tutorials.mybizz.UIScreens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.BillHistoryEntry
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import eu.tutorials.mybizz.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsScreen(
    navController: NavController,
    billId: String,
    authRepo: AuthRepository
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val billRepository = remember { BillRepository() }
    val sheetsRepository = remember { BillSheetsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var bill by remember { mutableStateOf<Bill?>(null) }
    var billHistory by remember { mutableStateOf<List<BillHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }

    val currentUserEmail = authRepo.currentUser.value?.email ?: ""

    // Load bill details
    LaunchedEffect(billId) {
        loadBillDetails(billId, sheetsRepository, { bill = it }, { error = it }, { isLoading = it })
    }

    // Load history when bill is loaded
    LaunchedEffect(bill) {
        bill?.let {
            if (it.billNumber.isNotEmpty()) {
                isLoadingHistory = true
                try {
                    billHistory = billRepository.getBillHistory(it.billNumber, sheetsRepository)
                } catch (e: Exception) {
                    Log.e("BillDetailsScreen", "Error loading history: ${e.message}")
                } finally {
                    isLoadingHistory = false
                }
            }
        }
    }

    fun reloadBillDetails() {
        coroutineScope.launch {
            isLoading = true
            error = null
            try {
                bill = sheetsRepository.getBillById(billId)
                if (bill == null) {
                    error = "Bill not found"
                } else {
                    billHistory = billRepository.getBillHistory(bill!!.billNumber, sheetsRepository)
                }
            } catch (e: Exception) {
                error = "Failed to load bill details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun markBillAsPaid() {
        coroutineScope.launch {
            isLoading = true
            try {
                val success = billRepository.markBillAsPaid(billId, sheetsRepository, currentUserEmail)
                if (success) {
                    bill?.let { currentBill ->
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date())
                        bill = currentBill.copy(
                            status = Bill.STATUS_PAID,
                            paidDate = currentDate,
                            paidBy = currentUserEmail
                        )
                    }
                    showMarkPaidDialog = false
                    reloadBillDetails()
                } else {
                    error = "Failed to mark bill as paid"
                    isLoading = false
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    fun deleteBill() {
        coroutineScope.launch {
            isLoading = true
            try {
                val success = billRepository.deleteBill(billId, sheetsRepository)
                if (success) {
                    navController.popBackStack()
                } else {
                    error = "Failed to delete bill"
                    showDeleteDialog = false
                    isLoading = false
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
                showDeleteDialog = false
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bill_details), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (bill?.status == Bill.STATUS_UNPAID) {
                        IconButton(onClick = {
                            navController.navigate("edit_bill/${billId}")
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_bill))
                        }
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_bill))
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading && bill == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Accent)
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            color = AppColors.Danger,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { reloadBillDetails() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                            shape = AppShapes.Button
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            } else if (bill == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.bill_not_found))
                }
            } else {
                BillDetailsContent(bill = bill!!)

                // Payment Action Section
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(horizontal = AppDimens.ScreenPadding)) {
                    if (bill!!.status == Bill.STATUS_UNPAID) {
                        // Pay Now Button
                        Button(
                            onClick = {
                                navController.navigate("payment_bill/${bill!!.id}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = AppShapes.Button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Success
                            )
                        ) {
                            Icon(
                                Icons.Default.AccountBox,
                                contentDescription = stringResource(R.string.pay_now),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pay Now - ₹${String.format("%.2f", bill!!.amount)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Alternative: Manual Mark as Paid
                        OutlinedButton(
                            onClick = { showMarkPaidDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = AppShapes.Button,
                            border = BorderStroke(1.dp, AppColors.Primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.mark_as_paid), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.mark_paid_manual))
                        }
                    } else {
                        // Show Paid Status Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.Card,
                            color = AppColors.SuccessBg
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(AppColors.Success.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppColors.Success,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.payment_completed),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = AppColors.Success
                                    )
                                    if (bill!!.paidDate.isNotEmpty()) {
                                        Text(
                                            "Paid on: ${bill!!.paidDate}",
                                            fontSize = 13.sp,
                                            color = AppColors.TextSecondary
                                        )
                                    }
                                    if (bill!!.paidBy.isNotEmpty()) {
                                        Text(
                                            "Paid by: ${bill!!.paidBy}",
                                            fontSize = 13.sp,
                                            color = AppColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // History Section
                Spacer(modifier = Modifier.height(24.dp))

                BillHistorySection(
                    billHistory = billHistory,
                    isLoading = isLoadingHistory,
                    currentBillNumber = bill!!.billNumber
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialogs
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text(stringResource(R.string.mark_as_paid), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.mark_paid_confirm)) },
            confirmButton = {
                TextButton(onClick = { markBillAsPaid() }) {
                    Text(stringResource(R.string.mark_as_paid), color = AppColors.Success, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_bill), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_bill_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { deleteBill() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.Danger
                    )
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
fun BillDetailsContent(bill: Bill) {
    val isPaid = bill.status == Bill.STATUS_PAID
    Column(modifier = Modifier.padding(AppDimens.ScreenPadding)) {
        // Status Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.Card,
            color = if (isPaid) AppColors.SuccessBg else AppColors.DangerBg
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = bill.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) AppColors.Success else AppColors.Danger
                        )
                        Text(
                            text = if (isPaid) "Paid on ${bill.paidDate}" else "Due on ${bill.dueDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bill: ${bill.billNumber}  ·  Version: ${bill.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted
                        )
                    }
                    Text(
                        text = "₹${"%.2f".format(bill.amount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }

                if (isPaid && bill.paidBy.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paid by: ${bill.paidBy}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SectionGap))

        Text(
            text = stringResource(R.string.bill_info),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.Card,
            color = AppColors.Surface,
            border = BorderStroke(1.dp, AppColors.Border)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                InfoRow(label = "Title", value = bill.title)
                InfoRow(label = "Category", value = bill.category)
                InfoRow(label = "Description", value = bill.description.ifEmpty { "No description" })
                InfoRow(label = "Due Date", value = bill.dueDate)
                InfoRow(label = "Created Date", value = bill.createdDate)
                InfoRow(label = "Created By", value = bill.createdBy)
                InfoRow(label = "Last Modified", value = bill.modifiedDate)
                InfoRow(label = "Last Modified By", value = bill.modifiedBy, showDivider = isPaid)

                if (isPaid) {
                    InfoRow(label = stringResource(R.string.paid_on), value = bill.paidDate)
                    if (bill.paidBy.isNotEmpty()) {
                        InfoRow(label = stringResource(R.string.paid_by), value = bill.paidBy, showDivider = false)
                    }
                }
            }
        }

        if (isPaid) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.CardSmall,
                color = AppColors.SurfaceMuted
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.bill_info),
                        tint = AppColors.Success
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bill_paid_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun BillHistorySection(
    billHistory: List<BillHistoryEntry>,
    isLoading: Boolean,
    currentBillNumber: String
) {
    Column(modifier = Modifier.padding(horizontal = AppDimens.ScreenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bill History - #$currentBillNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Text(
                text = "${billHistory.size} versions",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Accent)
            }
        } else if (billHistory.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.Card,
                color = AppColors.SurfaceMuted
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.no_history),
                        modifier = Modifier.size(36.dp),
                        tint = AppColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.bill_modifications_appear),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.Card,
                color = AppColors.Surface,
                border = BorderStroke(1.dp, AppColors.Border)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Primary, RoundedCornerShape(topStart = AppDimens.CardRadius, topEnd = AppDimens.CardRadius))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.bill_id_col),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.version_col),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.modified_by_col),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.date_col),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.bill_amount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.change_col),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(billHistory.sortedBy { it.version }) { historyEntry ->
                            HistoryTableRow(historyEntry = historyEntry, billHistory = billHistory)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HistorySummarySection(billHistory = billHistory)
        }
    }
}

@Composable
fun HistoryTableRow(historyEntry: BillHistoryEntry, billHistory: List<BillHistoryEntry>) {
    val isCreated = historyEntry.changeType == "CREATED"
    val previousVersion = billHistory.find { it.version == historyEntry.version - 1 }
    val amountChange = if (previousVersion != null) {
        historyEntry.amount - previousVersion.amount
    } else {
        0.0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCreated) AppColors.InfoBg else AppColors.Surface
            )
            .padding(12.dp)
            .border(
                width = if (isCreated) 1.dp else 0.dp,
                color = if (isCreated) AppColors.Primary.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = historyEntry.billNumber,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center,
            color = AppColors.TextPrimary
        )

        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "V${historyEntry.version}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isCreated) AppColors.Primary else AppColors.TextPrimary
            )
            if (isCreated) {
                Text(
                    text = stringResource(R.string.created),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Primary
                )
            } else {
                Text(
                    text = stringResource(R.string.updated),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Accent
                )
            }
        }

        Text(
            text = historyEntry.modifiedBy.split("@").firstOrNull() ?: historyEntry.modifiedBy,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = AppColors.TextPrimary
        )

        Text(
            text = historyEntry.modifiedDate.split(" ").getOrNull(0) ?: historyEntry.modifiedDate,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center,
            color = AppColors.TextSecondary
        )

        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "₹${"%.2f".format(historyEntry.amount)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.total),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (historyEntry.version > 1 && amountChange != 0.0) {
                Text(
                    text = if (amountChange > 0) "+₹${"%.2f".format(amountChange)}"
                    else "₹${"%.2f".format(amountChange)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (amountChange > 0) AppColors.Success else AppColors.Danger
                )
                Text(
                    text = if (amountChange > 0) stringResource(R.string.increase) else stringResource(R.string.decrease),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (amountChange > 0) AppColors.Success else AppColors.Danger
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted
                )
                Text(
                    text = if (historyEntry.version == 1) stringResource(R.string.initial) else stringResource(R.string.no_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted
                )
            }
        }
    }
    Divider(color = AppColors.Border, thickness = 1.dp)
}

@Composable
fun HistorySummarySection(billHistory: List<BillHistoryEntry>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        color = AppColors.SurfaceMuted
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.version_history_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val latestVersion = billHistory.maxByOrNull { it.version }
            val createdEntry = billHistory.find { it.changeType == stringResource(R.string.created) }
            val modifications = billHistory.count { it.changeType == stringResource(R.string.modified_by_col) }

            val initialAmount = billHistory.firstOrNull()?.amount ?: 0.0
            val currentAmount = latestVersion?.amount ?: 0.0
            val totalChange = currentAmount - initialAmount

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.current_version_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "V${latestVersion?.version ?: 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.modifications),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "$modifications",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.amount_change),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = if (totalChange >= 0) "+₹${"%.2f".format(totalChange)}"
                        else "₹${"%.2f".format(totalChange)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalChange >= 0) AppColors.Success else AppColors.Danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = AppColors.Border)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.created),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = createdEntry?.modifiedDate?.split(" ")?.get(0) ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.last_updated),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = latestVersion?.modifiedDate?.split(" ")?.get(0) ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.current_amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "₹${"%.2f".format(currentAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, showDivider: Boolean = true) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = AppColors.Border)
        }
    }
}

private suspend fun loadBillDetails(
    billId: String,
    sheetsRepository: BillSheetsRepository,
    onBillLoaded: (Bill?) -> Unit,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    onLoading(true)
    try {
        val bill = sheetsRepository.getBillById(billId)
        onBillLoaded(bill)
        if (bill == null) {
            onError("Bill not found")
        }
    } catch (e: Exception) {
        onError("Failed to load bill details: ${e.message}")
    } finally {
        onLoading(false)
    }
}