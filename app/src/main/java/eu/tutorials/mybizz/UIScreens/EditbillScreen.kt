// EditBillScreen.kt
package eu.tutorials.mybizz.UIScreens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillScreen(
    navController: NavController,
    billId: String,
    authRepo: AuthRepository
) {
    val context = LocalContext.current
    val billRepository = remember { BillRepository() }
    val sheetsRepository = remember { BillSheetsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var bill by remember { mutableStateOf<Bill?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Bill.CATEGORIES[0]) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentUserEmail = authRepo.currentUser.value?.email ?: ""

    // Date Picker State - initialize with current date
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        yearRange = Calendar.getInstance().get(Calendar.YEAR)..(Calendar.getInstance().get(Calendar.YEAR) + 5),
        initialDisplayMode = DisplayMode.Picker
    )

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            dueDate = sdf.format(Date(selectedDate))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Load bill details
    LaunchedEffect(billId) {
        try {
            bill = sheetsRepository.getBillById(billId)
            bill?.let { loadedBill ->
                // Check if bill is paid - if so, prevent editing
                if (loadedBill.status == Bill.STATUS_PAID) {
                    error = "Cannot edit paid bills"
                    return@LaunchedEffect
                }

                title = loadedBill.title
                description = loadedBill.description
                amount = loadedBill.amount.toString()
                dueDate = loadedBill.dueDate
                category = loadedBill.category

                // Set initial date for date picker by parsing the due date
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = sdf.parse(dueDate)
                    date?.let {
                        // The date picker will use the current dueDate value
                    }
                } catch (e: Exception) {
                    // Ignore date parsing errors
                }
            }
        } catch (e: Exception) {
            error = "Failed to load bill details: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Bill") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (bill == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Bill not found")
            }
        } else if (bill?.status == Bill.STATUS_PAID) {
            // Show message for paid bills - cannot edit
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Cannot Edit",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Edit Paid Bill",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This bill has been marked as paid and cannot be edited.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Bill Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Bill #${bill!!.billNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current Version: ${bill!!.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Editing will create version ${bill!!.version + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bill Title*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = title.isBlank() && error != null,
                    supportingText = {
                        if (title.isBlank() && error != null) {
                            Text("Title is required")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = it
                        }
                    },
                    label = { Text("Amount*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("₹") },
                    isError = (amount.isBlank() || amount.toDoubleOrNull() == null) && error != null,
                    supportingText = {
                        if (amount.isBlank() && error != null) {
                            Text("Amount is required")
                        } else if (amount.isNotBlank() && amount.toDoubleOrNull() == null) {
                            Text("Please enter a valid number")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due Date Field with Date Picker
                Text(
                    text = "Due Date*",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { },
                    label = { Text("Select due date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                        }
                    },
                    placeholder = { Text("Tap to select date") },
                    isError = dueDate.isBlank() && error != null,
                    supportingText = {
                        if (dueDate.isBlank() && error != null) {
                            Text("Due date is required")
                        } else if (dueDate.isNotBlank() && !isValidDate(dueDate)) {
                            Text("Please select a valid date")
                        }
                    }
                )

                // Quick date selection buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "Today" to 0,
                        "Tomorrow" to 1,
                        "Next Week" to 7,
                    ).forEach { (label, days) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.DAY_OF_MONTH, days)
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                dueDate = sdf.format(calendar.time)
                            },
                            label = { Text(label) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Dropdown
                Text(
                    text = "Category*",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Select a category") }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Bill.CATEGORIES.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Update Button
                Button(
                    onClick = {
                        if (editValidateInputs(title, amount, dueDate)) {
                            isLoading = true
                            error = null

                            coroutineScope.launch {
                                try {
                                    val updatedBill = bill!!.copy(
                                        title = title.trim(),
                                        description = description.trim(),
                                        amount = amount.toDouble(),
                                        dueDate = dueDate.trim(),
                                        category = category
                                    )

                                    val success = billRepository.updateBill(updatedBill, sheetsRepository, currentUserEmail)

                                    if (success) {
                                        Toast.makeText(context, "Bill updated successfully! New version created.", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        error = "Failed to update bill. Please check your internet connection and try again."
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    error = "Error: ${e.message}"
                                    isLoading = false
                                }
                            }
                        } else {
                            error = "Please fill all required fields correctly"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Updating Bill...")
                    } else {
                        Text("Update Bill (Version ${bill!!.version + 1})", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// Validation functions for EditBillScreen
private fun editValidateInputs(title: String, amount: String, dueDate: String): Boolean {
    if (title.trim().isEmpty() || amount.trim().isEmpty() || dueDate.trim().isEmpty()) {
        return false
    }

    if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
        return false
    }

    return isValidDate(dueDate.trim())
}

private fun isValidDate(date: String): Boolean {
    return try {
        if (!date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            return false
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(date)
        true
    } catch (e: Exception) {
        false
    }
}