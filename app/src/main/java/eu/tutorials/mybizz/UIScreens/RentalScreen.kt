package eu.tutorials.mybizz.UIScreens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalListScreen(
    sheetsRepo: RentalSheetsRepository,
    onRentalSelected: (Rental) -> Unit,
    onAddRental: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            rentals = sheetsRepo.getAllRentals()
            isLoading = false
        }
    }

    val filteredList = rentals.filter {
        it.tenantName.contains(searchQuery.text, ignoreCase = true) ||
                it.property.contains(searchQuery.text, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rental Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRental) {
                Icon(Icons.Default.Add, contentDescription = "Add Rental")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Tenant or Property") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No rentals found.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredList) { rental ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRentalSelected(rental) },
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(rental.tenantName, style = MaterialTheme.typography.titleMedium)
                                Text("🏠 ${rental.property}")
                                Text("💰 ₹${rental.rentAmount}")
                                Text("📅 Month: ${rental.month}")
                                Text(
                                    "Status: ${rental.status}",
                                    color = if (rental.status == Rental.STATUS_PAID)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRentalScreen(
    sheetsRepo: RentalSheetsRepository,
    onRentalAdded: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var tenantName by remember { mutableStateOf("") }
    var property by remember { mutableStateOf("") }
    var rentAmount by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var contactNo by remember { mutableStateOf("") }

    val openMonthYearPicker = {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val monthIndex = calendar.get(Calendar.MONTH)

        val dialog = DatePickerDialog(
            context,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, _: Int ->
                val formattedMonth = String.format("%04d-%02d", selectedYear, selectedMonth + 1)
                month = formattedMonth
            },
            year,
            monthIndex,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        try {
            val dayPickerId = context.resources.getIdentifier("day", "id", "android")
            val dayPicker = dialog.datePicker.findViewById<DatePicker>(dayPickerId)
            dayPicker?.visibility = android.view.View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dialog.show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Rental") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = tenantName,
                onValueChange = { tenantName = it },
                label = { Text("Tenant Name") }
            )

            OutlinedTextField(
                value = property,
                onValueChange = { property = it },
                label = { Text("Property/Shop") }
            )

            OutlinedTextField(
                value = rentAmount,
                onValueChange = { rentAmount = it },
                label = { Text("Rent Amount") }
            )

            OutlinedTextField(
                value = month,
                onValueChange = { },
                label = { Text("Month (YYYY-MM)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Pick Month",
                        modifier = Modifier.clickable { openMonthYearPicker() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openMonthYearPicker() },
                readOnly = true
            )

            OutlinedTextField(
                value = contactNo,
                onValueChange = { contactNo = it },
                label = { Text("Contact Number") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        val repo = RentalRepository()
                        val rental = Rental(
                            id = UUID.randomUUID().toString(),
                            tenantName = tenantName,
                            property = property,
                            rentAmount = rentAmount.toDoubleOrNull() ?: 0.0,
                            month = month,
                            status = Rental.STATUS_UNPAID,
                            contactNo = contactNo
                        )
                        repo.addRental(rental, sheetsRepo)
                        onRentalAdded()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Rental")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDetailScreen(
    rental: Rental,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkPaid: () -> Unit,
    onBack: () -> Unit,
    navController: NavController
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rental Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add call icon in top bar
                    if (rental.contactNo.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${rental.contactNo}")
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call Tenant")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rental Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Add call option in contact info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "👤 Tenant: ${rental.tenantName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("🏠 Property: ${rental.property}")
                            Text(
                                "💰 Rent: ₹${rental.rentAmount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("📅 Month: ${rental.month}")

                            // Contact info with call button
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📞 Contact: ${rental.contactNo}")
                                if (rental.contactNo.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                data = android.net.Uri.parse("tel:${rental.contactNo}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = "Call Tenant",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (rental.paymentDate.isNotEmpty()) {
                                Text("Payment Date: ${rental.paymentDate}")
                            }
                        }
                    }

                    // Status
                    Text(
                        "Status: ${rental.status.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        color = if (rental.status == Rental.STATUS_PAID)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Action Section
            if (rental.status == Rental.STATUS_UNPAID) {
                Button(
                    onClick = {
                        navController.navigate("payment_rental/${rental.id}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pay Now - ₹${String.format("%.2f", rental.rentAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showMarkPaidDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, "Mark Paid")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Paid (Manual)")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
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
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Rent Paid ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1565C0)
                            )
                            if (rental.paymentDate.isNotEmpty()) {
                                Text(
                                    "Paid on: ${rental.paymentDate}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rental.status == Rental.STATUS_UNPAID) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, "Edit")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.Delete, "Delete")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Delete Rental?") },
            text = { Text("Are you sure you want to delete this rental record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
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

    // Manual Mark as Paid Dialog
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text("Mark as Paid") },
            text = { Text("Are you sure you want to manually mark this rental as paid? Use 'Pay Now' for payment processing.") },
            confirmButton = {
                TextButton(onClick = {
                    showMarkPaidDialog = false
                    onMarkPaid()
                }) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRentalScreen(
    rental: Rental,
    sheetsRepo: RentalSheetsRepository,
    onRentalUpdated: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var tenantName by remember { mutableStateOf(rental.tenantName) }
    var property by remember { mutableStateOf(rental.property) }
    var rentAmount by remember { mutableStateOf(rental.rentAmount.toString()) }
    var month by remember { mutableStateOf(rental.month) }
    var contactNo by remember { mutableStateOf(rental.contactNo) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Rental") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = tenantName, onValueChange = { tenantName = it }, label = { Text("Tenant Name") })
            OutlinedTextField(value = property, onValueChange = { property = it }, label = { Text("Property/Shop") })
            OutlinedTextField(value = rentAmount, onValueChange = { rentAmount = it }, label = { Text("Rent Amount") })
            OutlinedTextField(
                value = month,
                onValueChange = { month = it },
                label = { Text("Month (YYYY-MM)") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Date") }
            )
            OutlinedTextField(value = contactNo, onValueChange = { contactNo = it }, label = { Text("Contact No") })

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        val repo = RentalRepository()
                        val updated = rental.copy(
                            tenantName = tenantName,
                            property = property,
                            rentAmount = rentAmount.toDoubleOrNull() ?: 0.0,
                            month = month,
                            contactNo = contactNo
                        )
                        repo.updateRental(updated, sheetsRepo)
                        onRentalUpdated()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Rental")
            }
        }
    }
}