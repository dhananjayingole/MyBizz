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
                    reloadBillDetails() // Reload to get updated history
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
                title = { Text("Bill Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (bill?.status == Bill.STATUS_UNPAID) {
                        IconButton(onClick = {
                            navController.navigate("edit_bill/${billId}")
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            if (bill?.status == Bill.STATUS_UNPAID) {
                ExtendedFloatingActionButton(
                    onClick = { showMarkPaidDialog = true },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Mark Paid") },
                    text = { Text("Mark as Paid") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
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
                            Text("Retry")
                        }
                    }
                }
            } else if (bill == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bill not found")
                }
            } else {
                BillDetailsContent(bill = bill!!)

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
            title = { Text("Mark as Paid") },
            text = { Text("Are you sure you want to mark this bill as paid?") },
            confirmButton = {
                TextButton(onClick = { markBillAsPaid() }) {
                    Text("Mark Paid")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Bill") },
            text = { Text("Are you sure you want to delete this bill? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { deleteBill() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

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

                        // Bill Number and Version
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

                // Show paid by information if bill is paid
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

        // Bill Information
        Text(
            text = "Bill Information",
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
            InfoRow(label = "Paid Date", value = bill.paidDate)
            if (bill.paidBy.isNotEmpty()) {
                InfoRow(label = "Paid By", value = bill.paidBy)
            }
        }

        // Edit restriction notice for paid bills
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
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This bill has been paid and cannot be edited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// NEW: Bill History Section Component - UPDATED VERSION
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
                        contentDescription = "No History",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No History Available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bill modifications will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // History Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bill ID",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Modified By",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Table Rows - Show in chronological order (oldest first)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(billHistory.sortedBy { it.version }) { historyEntry ->
                            HistoryTableRow(historyEntry = historyEntry, billHistory = billHistory)
                        }
                    }
                }
            }

            // History Summary
            Spacer(modifier = Modifier.height(16.dp))
            HistorySummarySection(billHistory = billHistory)
        }
    }
}

// NEW: History Table Row Component - UPDATED VERSION
@Composable
fun HistoryTableRow(historyEntry: BillHistoryEntry, billHistory: List<BillHistoryEntry>) {
    val isCreated = historyEntry.changeType == "CREATED"
    val previousVersion = billHistory.find { it.version == historyEntry.version - 1 }
    val amountChange = if (previousVersion != null) {
        historyEntry.amount - previousVersion.amount
    } else {
        0.0 // First version has no change
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
        // Bill ID Column
        Text(
            text = historyEntry.billNumber,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )

        // Version Column
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
                    text = "Created",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Updated",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Modified By Column
        Text(
            text = historyEntry.modifiedBy.split("@").firstOrNull() ?: historyEntry.modifiedBy,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Date Column
        Text(
            text = historyEntry.modifiedDate.split(" ").getOrNull(0) ?: historyEntry.modifiedDate,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center
        )

        // Amount Column
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
                text = "Total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Change Column
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
                    text = if (amountChange > 0) "Increase" else "Decrease",
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
                    text = if (historyEntry.version == 1) "Initial" else "No Change",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// NEW: History Summary Component
@Composable
fun HistorySummarySection(billHistory: List<BillHistoryEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Version History Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val latestVersion = billHistory.maxByOrNull { it.version }
            val createdEntry = billHistory.find { it.changeType == "CREATED" }
            val modifications = billHistory.count { it.changeType == "MODIFIED" }

            // Calculate amount changes
            val initialAmount = billHistory.firstOrNull()?.amount ?: 0.0
            val currentAmount = latestVersion?.amount ?: 0.0
            val totalChange = currentAmount - initialAmount

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Version",
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
                        text = "Modifications",
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
                        text = "Amount Change",
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

            // Timeline info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Created",
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
                        text = "Last Updated",
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
                        text = "Current Amount",
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