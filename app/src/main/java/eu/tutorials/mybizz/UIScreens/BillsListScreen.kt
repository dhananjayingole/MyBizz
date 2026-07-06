// BillsListScreen.kt
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R
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
                    error = context.getString(R.string.failed_mark_paid)
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
                    error = context.getString(R.string.failed_delete_bill)
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bills_management), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        floatingActionButton = {
            // Both users and admins can add bills
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add_bill") },
                containerColor = AppColors.Accent,
                contentColor = androidx.compose.ui.graphics.Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_bill)) },
                text = { Text("New Bill") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.ScreenPadding, vertical = 12.dp),
                shape = AppShapes.CardSmall,
                color = AppColors.Surface,
                border = BorderStroke(1.dp, AppColors.Border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = AppColors.TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_bills), color = AppColors.TextMuted) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = AppColors.TextMuted
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
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = AppDimens.ScreenPadding)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading && bills.isEmpty()) {
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
                            onClick = { reloadBills() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                            shape = AppShapes.Button
                        ) {
                            Text(stringResource(R.string.retry))
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
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.SurfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = AppColors.TextMuted,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    "No bills found matching \"$searchQuery\"",
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    stringResource(R.string.try_different_search),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            } else {
                                Text(
                                    stringResource(R.string.no_bills_found),
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    stringResource(R.string.add_first_bill),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
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

                    Column(modifier = Modifier.padding(horizontal = AppDimens.ScreenPadding)) {
                        // Statistics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = stringResource(R.string.total),
                                value = totalBills.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(R.string.paid),
                                value = paidBills.toString(),
                                isPaid = true,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(R.string.unpaid),
                                value = unpaidBills.toString(),
                                isPaid = false,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount Summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.CardSmall,
                            color = AppColors.SurfaceMuted
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = stringResource(R.string.total_amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary
                                    )
                                    Text(
                                        text = "₹${"%.2f".format(totalAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.due_date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary
                                    )
                                    Text(
                                        text = "₹${"%.2f".format(unpaidAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Danger
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results (${filteredBills.size})"
                            else "All Bills (${filteredBills.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(filteredBills.reversed()) { bill ->
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
    val isPaid = bill.status == Bill.STATUS_PAID
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.CardSmall,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bill #${bill.billNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = bill.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Due: ${bill.dueDate}  ·  v${bill.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted
                    )

                    if (isPaid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (bill.paidBy.isNotEmpty()) {
                                "Paid by: ${bill.paidBy}"
                            } else {
                                stringResource(R.string.paid)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Success
                        )
                        if (bill.paidDate.isNotEmpty()) {
                            Text(
                                text = "On: ${bill.paidDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextMuted
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${"%.2f".format(bill.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusChip(isPaid = isPaid)
                }
            }

            // Both users and admins can mark bills as paid, but only unpaid bills
            if (!isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onMarkAsPaid(bill.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Success
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.mark_as_paid))
                }
            }
        }
    }
}

/** Small pill used to communicate paid / unpaid at a glance. */
@Composable
fun StatusChip(isPaid: Boolean) {
    val bg = if (isPaid) AppColors.SuccessBg else AppColors.DangerBg
    val fg = if (isPaid) AppColors.Success else AppColors.Danger
    val label = if (isPaid) "Paid" else "Unpaid"
    Surface(
        shape = AppShapes.Chip,
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPaid) Icons.Default.Check else Icons.Default.Close,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
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
fun StatCard(
    title: String,
    value: String,
    isPaid: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (isPaid) {
        true -> AppColors.SuccessBg to AppColors.Success
        false -> AppColors.DangerBg to AppColors.Danger
        null -> AppColors.SurfaceMuted to AppColors.TextPrimary
    }
    Surface(
        modifier = modifier.height(76.dp),
        shape = AppShapes.CardSmall,
        color = bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = fg.copy(alpha = 0.85f)
            )
        }
    }
}