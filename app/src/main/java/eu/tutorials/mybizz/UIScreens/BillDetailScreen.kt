// BillDetailsScreen.kt - OPTIMIZED VERSION
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Logic.Bill.BillSheetsRepository
import eu.tutorials.mybizz.Model.Bill
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsScreen(
    navController: NavController,
    billId: String,
    authRepo: AuthRepository
) {
    val context = LocalContext.current
    val billRepository = remember { BillRepository() }
    val sheetsRepository = remember { BillSheetsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var bill by remember { mutableStateOf<Bill?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }

    val isAdmin = authRepo.getCurrentUserRole() == "admin"
    val currentUserEmail = authRepo.currentUser.value?.email ?: ""

    // Load bill details
    LaunchedEffect(billId) {
        loadBillDetails(billId, sheetsRepository, { bill = it }, { error = it }, { isLoading = it })
    }

    // Function to reload bill details
    fun reloadBillDetails() {
        coroutineScope.launch {
            isLoading = true
            error = null
            try {
                bill = sheetsRepository.getBillById(billId)
                if (bill == null) {
                    error = "Bill not found"
                }
            } catch (e: Exception) {
                error = "Failed to load bill details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Function to mark bill as paid
    fun markBillAsPaid() {
        coroutineScope.launch {
            isLoading = true
            try {
                val currentUserEmail = authRepo.currentUser.value?.email ?: "Unknown User"
                val success = billRepository.markBillAsPaid(billId, sheetsRepository, currentUserEmail)
                if (success) {
                    // Update local state optimistically
                    bill?.let { currentBill ->
                        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        bill = currentBill.copy(
                            status = Bill.STATUS_PAID,
                            paidDate = currentDate,
                            paidBy = currentUserEmail
                        )
                    }
                    showMarkPaidDialog = false
                } else {
                    error = "Failed to mark bill as paid"
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Function to delete bill
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
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
                showDeleteDialog = false
            } finally {
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
                    if (isAdmin && bill != null) {
                        IconButton(onClick = {
                            navController.navigate("edit_bill/${billId}")
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin && bill?.status == Bill.STATUS_UNPAID) {
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
                BillDetailsContent(bill = bill!!, isAdmin = isAdmin)
            }
        }
    }

    // Mark as Paid Confirmation Dialog
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text("Mark as Paid") },
            text = { Text("Are you sure you want to mark this bill as paid?") },
            confirmButton = {
                TextButton(
                    onClick = { markBillAsPaid() }
                ) {
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

    // Delete Confirmation Dialog
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

// In BillDetailsScreen.kt - Update the BillDetailsContent composable
@Composable
fun BillDetailsContent(bill: Bill, isAdmin: Boolean) {
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

        if (bill.status == Bill.STATUS_PAID) {
            InfoRow(label = "Paid Date", value = bill.paidDate)
            if (bill.paidBy.isNotEmpty()) {
                InfoRow(label = "Paid By", value = bill.paidBy)
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