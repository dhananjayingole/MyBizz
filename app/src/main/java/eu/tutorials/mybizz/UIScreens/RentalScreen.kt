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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(rentals) {
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

    // 📅 Function to open month-year picker
    val openMonthYearPicker = {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val monthIndex = calendar.get(Calendar.MONTH)

        val dialog = DatePickerDialog(
            context,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, _: Int ->
                // Format: YYYY-MM
                val formattedMonth = String.format("%04d-%02d", selectedYear, selectedMonth + 1)
                month = formattedMonth
            },
            year,
            monthIndex,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // 👇 Hide the day picker (only show month & year)
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

            // 📅 Month-Year Picker Field
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
    onEdit: (Rental) -> Unit,
    onDelete: (Rental) -> Unit,
    onMarkPaid: (Rental) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rental Details") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("👤 Tenant: ${rental.tenantName}", style = MaterialTheme.typography.titleMedium)
            Text("🏠 Property: ${rental.property}")
            Text("💰 Rent: ₹${rental.rentAmount}")
            Text("📅 Month: ${rental.month}")
            Text("📞 Contact: ${rental.contactNo}")
            Text("Status: ${rental.status}")
            Text("Payment Date: ${rental.paymentDate ?: "Not Paid"}")

            Spacer(modifier = Modifier.height(16.dp))

            // Show Edit only when UNPAID
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                if (rental.status == Rental.STATUS_UNPAID) {
                    Button(
                        onClick = { onEdit(rental) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit")
                    }
                }

                OutlinedButton(
                    onClick = { onDelete(rental) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }

            if (rental.status == Rental.STATUS_UNPAID) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onMarkPaid(rental)
                        onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Paid 💸")
                }
            }
        }
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
