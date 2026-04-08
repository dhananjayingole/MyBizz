package eu.tutorials.mybizz.UIScreens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bill_details)) },
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
        // REMOVED: Mark as Paid FAB - replaced with Pay Now button
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
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { reloadBillDetails() }) {
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

                // NEW: Payment Action Section
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (bill!!.status == Bill.STATUS_UNPAID) {
                        // Pay Now Button
                        Button(
                            onClick = {
                                navController.navigate("payment_bill/${bill!!.id}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50) // Green
                            )
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.pay_now),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pay Now - ₹${String.format("%.2f", bill!!.amount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Alternative: Manual Mark as Paid
                        OutlinedButton(
                            onClick = { showMarkPaidDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, stringResource(R.string.mark_as_paid))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.mark_paid_manual))
                        }
                    } else {
                        // Show Paid Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9) // Light green
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.payment_completed),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    if (bill!!.paidDate.isNotEmpty()) {
                                        Text(
                                            "Paid on: ${bill!!.paidDate}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    if (bill!!.paidBy.isNotEmpty()) {
                                        Text(
                                            "Paid by: ${bill!!.paidBy}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
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
            }
        }
    }

    // Dialogs
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text(stringResource(R.string.mark_as_paid)) },
            text = { Text(stringResource(R.string.mark_paid_confirm)) },
            confirmButton = {
                TextButton(onClick = { markBillAsPaid() }) {
                    Text(stringResource(R.string.mark_as_paid))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = false }) {
                    Text(stringResource(R.string.mark_as_paid))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_bill)) },
            text = { Text(stringResource(R.string.delete_bill_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { deleteBill() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// Keep all other composables the same (BillDetailsContent, BillHistorySection, etc.)
@Composable
fun BillDetailsContent(bill: Bill) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (bill.status == Bill.STATUS_PAID)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            color = if (bill.status == Bill.STATUS_PAID)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (bill.status == Bill.STATUS_PAID) "Paid on ${bill.paidDate}"
                            else "Due on ${bill.dueDate}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bill: ${bill.billNumber} | Version: ${bill.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "₹${"%.2f".format(bill.amount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (bill.status == Bill.STATUS_PAID && bill.paidBy.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paid by: ${bill.paidBy}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.bill_info),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        InfoRow(label = "Title", value = bill.title)
        InfoRow(label = "Category", value = bill.category)
        InfoRow(label = "Description", value = bill.description.ifEmpty { "No description" })
        InfoRow(label = "Due Date", value = bill.dueDate)
        InfoRow(label = "Created Date", value = bill.createdDate)
        InfoRow(label = "Created By", value = bill.createdBy)
        InfoRow(label = "Last Modified", value = bill.modifiedDate)
        InfoRow(label = "Last Modified By", value = bill.modifiedBy)

        if (bill.status == Bill.STATUS_PAID) {
            InfoRow(label = stringResource(R.string.paid_on), value = bill.paidDate)
            if (bill.paidBy.isNotEmpty()) {
                InfoRow(label = stringResource(R.string.paid_by), value = bill.paidBy)
            }
        }

        if (bill.status == Bill.STATUS_PAID) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.bill_info),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bill_paid_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bill History - #$currentBillNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Total Versions: ${billHistory.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (billHistory.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.no_history),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =  stringResource(R.string.no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.bill_modifications_appear),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
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
                if (isCreated) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(12.dp)
            .border(
                1.dp,
                if (isCreated) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent,
                RoundedCornerShape(4.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = historyEntry.billNumber,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "V${historyEntry.version}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isCreated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (isCreated) {
                Text(
                    text = stringResource(R.string.created),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.updated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Text(
            text = historyEntry.modifiedBy.split("@").firstOrNull() ?: historyEntry.modifiedBy,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = historyEntry.modifiedDate.split(" ").getOrNull(0) ?: historyEntry.modifiedDate,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "₹${"%.2f".format(historyEntry.amount)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                    color = if (amountChange > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (amountChange > 0) stringResource(R.string.increase) else stringResource(R.string.decrease),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (amountChange > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = if (historyEntry.version == 1) stringResource(R.string.initial) else stringResource(R.string.no_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun HistorySummarySection(billHistory: List<BillHistoryEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.version_history_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "V${latestVersion?.version ?: 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.modifications),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$modifications",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.amount_change),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (totalChange >= 0) "+₹${"%.2f".format(totalChange)}"
                        else "₹${"%.2f".format(totalChange)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalChange >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.created),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = createdEntry?.modifiedDate?.split(" ")?.get(0) ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.last_updated),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = latestVersion?.modifiedDate?.split(" ")?.get(0) ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.current_amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${"%.2f".format(currentAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Divider(modifier = Modifier.padding(vertical = 4.dp))
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