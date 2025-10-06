package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRentalScreen(
    sheetsRepo: RentalSheetsRepository,
    onRentalAdded: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var tenantName by remember { mutableStateOf(TextFieldValue("")) }
    var property by remember { mutableStateOf(TextFieldValue("")) }
    var rentAmount by remember { mutableStateOf(TextFieldValue("")) }
    var month by remember { mutableStateOf(TextFieldValue("")) }
    var contactNo by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add Rental") })
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
            OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month (e.g. 2025-10)") })
            OutlinedTextField(value = contactNo, onValueChange = { contactNo = it }, label = { Text("Contact No") })

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    scope.launch {
                        val repo = RentalRepository()
                        val rental = Rental(
                            id = UUID.randomUUID().toString(),
                            tenantName = tenantName.text,
                            property = property.text,
                            rentAmount = rentAmount.text.toDoubleOrNull() ?: 0.0,
                            month = month.text,
                            status = Rental.STATUS_UNPAID,
                            contactNo = contactNo.text
                        )
                        repo.addRental(rental, sheetsRepo)
                        onRentalAdded()
                    }
                }) {
                    Text("Save")
                }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
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
    onMarkPaid: (Rental) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rental Details") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tenant: ${rental.tenantName}")
            Text("Property: ${rental.property}")
            Text("Rent: ${rental.rentAmount}")
            Text("Month: ${rental.month}")
            Text("Status: ${rental.status}")
            Text("Payment Date: ${rental.paymentDate}")
            Text("Contact No: ${rental.contactNo}")

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onEdit(rental) }) { Text("Edit") }
                OutlinedButton(onClick = { onDelete(rental) }) { Text("Delete") }
                if (rental.status == Rental.STATUS_UNPAID) {
                    Button(onClick = { onMarkPaid(rental) }) { Text("Mark Paid") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalListScreen(
    sheetsRepo: RentalSheetsRepository,
    onRentalSelected: (Rental) -> Unit,
    onAddRental: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            rentals = sheetsRepo.getAllRentals()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rentals") }, actions = {
                IconButton(onClick = onAddRental) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rental")
                }
            })
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rentals) { rental ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRentalSelected(rental) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tenant: ${rental.tenantName}", style = MaterialTheme.typography.titleMedium)
                            Text("Property: ${rental.property}")
                            Text("Month: ${rental.month}")
                            Text("Status: ${rental.status}")
                        }
                    }
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
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var tenantName by remember { mutableStateOf(TextFieldValue(rental.tenantName)) }
    var property by remember { mutableStateOf(TextFieldValue(rental.property)) }
    var rentAmount by remember { mutableStateOf(TextFieldValue(rental.rentAmount.toString())) }
    var month by remember { mutableStateOf(TextFieldValue(rental.month)) }
    var contactNo by remember { mutableStateOf(TextFieldValue(rental.contactNo)) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Rental") }) }
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
            OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month (e.g. 2025-10)") })
            OutlinedTextField(value = contactNo, onValueChange = { contactNo = it }, label = { Text("Contact No") })

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    scope.launch {
                        val repo = RentalRepository()
                        val updated = rental.copy(
                            tenantName = tenantName.text,
                            property = property.text,
                            rentAmount = rentAmount.text.toDoubleOrNull() ?: 0.0,
                            month = month.text,
                            contactNo = contactNo.text
                        )
                        repo.updateRental(updated, sheetsRepo)
                        onRentalUpdated()
                    }
                }) { Text("Update") }

                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}
