package eu.tutorials.mybizz.UIScreens

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import kotlinx.coroutines.launch
import java.util.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.ui.res.stringResource
import eu.tutorials.mybizz.R

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
                title = { Text(stringResource(R.string.rental_management)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRental) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_rental))
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
                label = { Text(stringResource(R.string.search_tenant_property)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
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
                    Text(stringResource(R.string.no_rentals_found))
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

@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
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
            val dayPicker = dialog. datePicker.findViewById<DatePicker>(dayPickerId)
            dayPicker?.visibility = android.view.View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dialog.show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_rental)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                label = { Text(stringResource(R.string.tenant_name)) }
            )

            OutlinedTextField(
                value = property,
                onValueChange = { property = it },
                label = { Text(stringResource(R.string.property_shop)) }
            )

            OutlinedTextField(
                value = rentAmount,
                onValueChange = { rentAmount = it },
                label = { Text(stringResource(R.string.rent_amount)) }
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
                label = { Text(stringResource(R.string.contact_number)) }
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
                Text(stringResource(R.string.save_rental))
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
    var showCallDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showPaymentOptions by remember { mutableStateOf(false) }

    // Function to handle call
    fun makePhoneCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            // Check if we have permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context.startActivity(intent)
            } else {
                // Request permission if not granted
                showCallDialog = true
            }
        } catch (e: Exception) {
            // Fallback to dial if call fails
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(dialIntent)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.rental_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
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

                    // Contact info with clickable call button
                    if (rental.contactNo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { makePhoneCall(rental.contactNo) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${rental.contactNo}",
                                        fontSize = 16.sp
                                    )
                                }

                                // Small call button
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (rental.paymentDate.isNotEmpty()) {
                        Text("Payment Date: ${rental.paymentDate}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                    onClick = { showPaymentOptions = true }, // Changed this line
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
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
                    Icon(Icons.Default.Check, stringResource(R.string.mark_paid_button))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_paid_manual))
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
                                stringResource(R.string.rent_paid),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rental.status == Rental.STATUS_UNPAID) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.edit))
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.delete))
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
            title = { Text(stringResource(R.string.delete_rental)) },
            text = { Text(stringResource(R.string.delete_rental_confirm)) },
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

    // Manual Mark as Paid Dialog
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text(stringResource(R.string.mark_as_paid)) },
            text = { Text(stringResource(R.string.mark_paid_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showMarkPaidDialog = false
                    onMarkPaid()
                }) {
                    Text(stringResource(R.string.mark_paid_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
// Add this dialog call at the bottom of RentalDetailScreen (alongside other dialogs):
    if (showPaymentOptions) {
        PaymentOptionsDialog(
            rental = rental,
            onDismiss = { showPaymentOptions = false },
            onPayHere = {
                navController.navigate("payment_rental/${rental.id}")
            }
        )
    }


    // Call Permission Dialog
    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = { Text(stringResource(R.string.call_permission_required)) },
            text = { Text(stringResource(R.string.call_permission_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showCallDialog = false
                    // Open app settings for permission
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCallDialog = false
                    // Fallback to dial
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${rental.contactNo}")
                    }
                    context.startActivity(dialIntent)
                }) {
                    Text(stringResource(R.string.use_dialer))
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
                title = { Text(stringResource(R.string.edit_rental)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            OutlinedTextField(value = tenantName, onValueChange = { tenantName = it }, label = { Text(stringResource(R.string.tenant_name)) })
            OutlinedTextField(value = property, onValueChange = { property = it }, label = { Text(stringResource(R.string.property_shop)) })
            OutlinedTextField(value = rentAmount, onValueChange = { rentAmount = it }, label = { Text(stringResource(R.string.rent_amount)) })
            OutlinedTextField(
                value = month,
                onValueChange = { month = it },
                label = { Text("Month (YYYY-MM)") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.date)) }
            )
            OutlinedTextField(value = contactNo, onValueChange = { contactNo = it }, label = { Text(stringResource(R.string.contact_number)) })

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
                Text(stringResource(R.string.update_rental))
            }
        }
    }
}