// BillsListScreen.kt
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsListScreen(
    navController: NavController,
    authRepo: AuthRepository
) {
    val context = LocalContext.current
    val billRepository = remember { BillRepository() }
    val sheetsRepository = remember { BillSheetsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var bills by remember { mutableStateOf<List<Bill>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val currentUserEmail = authRepo.currentUser.value?.email ?: ""

    // Filter bills based on search query
    val filteredBills = bills.filter { bill ->
        searchQuery.isEmpty() || bill.title.contains(searchQuery, ignoreCase = true) ||
                bill.category.contains(searchQuery, ignoreCase = true) ||
                bill.description.contains(searchQuery, ignoreCase = true) ||
                bill.billNumber.contains(searchQuery, ignoreCase = true) // NEW: Search by bill number
    }

    // Load bills when screen is created
    LaunchedEffect(Unit) {
        loadBills(sheetsRepository, bills, { bills = it }, { error = it }, { isLoading = it })
    }

    // Function to reload bills
    fun reloadBills() {
        coroutineScope.launch {
            refreshing = true
            error = null
            try {
                bills = sheetsRepository.getAllBills()
            } catch (e: Exception) {
                error = "Failed to load bills: ${e.message}"
            } finally {
                refreshing = false
            }
        }
    }

    // Function to mark bill as paid
    fun markBillAsPaid(billId: String) {
        coroutineScope.launch {
            isLoading = true
            try {
                val success = billRepository.markBillAsPaid(billId, sheetsRepository, currentUserEmail)
                if (success) {
                    // Optimistic update - update local state immediately
                    bills = bills.map { bill ->
                        if (bill.id == billId) {
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date())
                            bill.copy(
                                status = Bill.STATUS_PAID,
                                paidDate = currentDate,
                                paidBy = currentUserEmail
                            )
                        } else {
                            bill
                        }
                    }
                    // Then sync with server
                    reloadBills()
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
    fun deleteBill(billId: String) {
        coroutineScope.launch {
            isLoading = true
            try {
                val success = billRepository.deleteBill(billId, sheetsRepository)
                if (success) {
                    // Optimistic update - remove from local state immediately
                    bills = bills.filter { it.id != billId }
                    // Show success message or handle accordingly
                } else {
                    error = "Failed to delete bill"
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bills Management") },
            )
        },
        floatingActionButton = {
            // Both users and admins can add bills
            FloatingActionButton(
                onClick = { navController.navigate("add_bill") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search bills by title, category, bill number...") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search results info
            if (searchQuery.isNotEmpty()) {
                Text(
                    text = "Found ${filteredBills.size} bills matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading && bills.isEmpty()) {
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
                        Button(onClick = { reloadBills() }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                if (filteredBills.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (searchQuery.isNotEmpty()) {
                                Text("No bills found matching \"$searchQuery\"")
                                Text("Try a different search term", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("No bills found.")
                                Text("Add your first bill using the + button!")
                            }
                        }
                    }
                } else {
                    // Statistics (based on filtered results)
                    val totalBills = filteredBills.size
                    val paidBills = filteredBills.count { it.status == Bill.STATUS_PAID }
                    val unpaidBills = filteredBills.count { it.status == Bill.STATUS_UNPAID }
                    val totalAmount = filteredBills.sumOf { it.amount }
                    val unpaidAmount = filteredBills.filter { it.status == Bill.STATUS_UNPAID }.sumOf { it.amount }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Statistics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard(title = "Total", value = totalBills.toString())
                            StatCard(title = "Paid", value = paidBills.toString(), isPaid = true)
                            StatCard(title = "Unpaid", value = unpaidBills.toString(), isPaid = false)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amount Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "₹${"%.2f".format(totalAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Total Amount", style = MaterialTheme.typography.bodySmall)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "₹${"%.2f".format(unpaidAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text("Due Amount", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results (${filteredBills.size})"
                            else "All Bills (${filteredBills.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredBills) { bill ->
                                BillItem(
                                    bill = bill,
                                    onMarkAsPaid = { markBillAsPaid(it) },
                                    onClick = {
                                        navController.navigate("bill_details/${bill.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillItem(
    bill: Bill,
    onMarkAsPaid: (String) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // NEW: Bill Number display
                    Text(
                        text = "Bill #${bill.billNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = bill.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Due: ${bill.dueDate}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // NEW: Version info
                    Text(
                        text = "Version: ${bill.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    if (bill.status == Bill.STATUS_PAID) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (bill.paidBy.isNotEmpty()) {
                                "Paid by: ${bill.paidBy}"
                            } else {
                                "Paid"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (bill.paidDate.isNotEmpty()) {
                            Text(
                                text = "On: ${bill.paidDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${"%.2f".format(bill.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (bill.status == Bill.STATUS_PAID) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = bill.status,
                            tint = if (bill.status == Bill.STATUS_PAID) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = bill.status.replaceFirstChar { it.uppercase() },
                            color = if (bill.status == Bill.STATUS_PAID) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Both users and admins can mark bills as paid, but only unpaid bills
            if (bill.status == Bill.STATUS_UNPAID) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onMarkAsPaid(bill.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Mark as Paid")
                }
            }
        }
    }
}

private suspend fun loadBills(
    sheetsRepository: BillSheetsRepository,
    currentBills: List<Bill>,
    onBillsLoaded: (List<Bill>) -> Unit,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    onLoading(true)
    try {
        val newBills = sheetsRepository.getAllBills()
        onBillsLoaded(newBills)
    } catch (e: Exception) {
        onError("Failed to load bills: ${e.message}")
        if (currentBills.isEmpty()) {
            onBillsLoaded(emptyList())
        }
    } finally {
        onLoading(false)
    }
}

@Composable
fun StatCard(title: String, value: String, isPaid: Boolean? = null) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (isPaid) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}