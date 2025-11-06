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
fun AddBillScreen(
    navController: NavController,
    authRepo: AuthRepository
) {
    val context = LocalContext.current
    val billRepository = remember { BillRepository() }
    val sheetsRepository = remember { BillSheetsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Bill.CATEGORIES[0]) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Track successful bill addition
    var billAddedSuccessfully by remember { mutableStateOf(false) }

    // Navigate back when bill is successfully added
    LaunchedEffect(billAddedSuccessfully) {
        if (billAddedSuccessfully) {
            navController.popBackStack()
        }
    }

    // Date Picker State
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // Set initial date to today + 7 days
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val defaultDueDate = calendar.time

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = defaultDueDate.time,
        yearRange = currentYear..(currentYear + 5),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Bill") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isLoading) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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

            // Add Bill Button
            Button(
                onClick = {
                    if (validateInputs(title, amount, dueDate)) {
                        isLoading = true
                        error = null

                        coroutineScope.launch {
                            try {
                                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                val currentUserEmail = authRepo.currentUser.value?.email ?: "unknown"

                                val newBill = Bill(
                                    id = UUID.randomUUID().toString(),
                                    billNumber = "", // This will be generated by repository
                                    version = 1,
                                    title = title.trim(),
                                    description = description.trim(),
                                    amount = amount.toDouble(),
                                    dueDate = dueDate.trim(),
                                    status = Bill.STATUS_UNPAID,
                                    category = category,
                                    paidDate = "",
                                    paidBy = "",
                                    createdDate = currentDate,
                                    createdBy = currentUserEmail,
                                    modifiedDate = currentDate,
                                    modifiedBy = currentUserEmail
                                )

                                val success = billRepository.addBill(newBill, sheetsRepository)

                                if (success) {
                                    Toast.makeText(context, "Bill added successfully!", Toast.LENGTH_SHORT).show()
                                    billAddedSuccessfully = true // Trigger navigation
                                } else {
                                    error = "Failed to add bill. Please check your internet connection and try again."
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
                    Text("Adding Bill...")
                } else {
                    Text("Add Bill", style = MaterialTheme.typography.bodyLarge)
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

            // Performance notice
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Adding bill to Google Sheets...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This may take a few seconds.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Form Guidelines
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Form Guidelines",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GuideLineItem("Title: Brief description of the bill")
                    GuideLineItem("Amount: Enter numeric value only (e.g., 1500.50)")
                    GuideLineItem("Due Date: Select using date picker or quick buttons")
                    GuideLineItem("Category: Select appropriate category from dropdown")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: Bills are saved to Google Sheets and may take 2-3 seconds to process.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GuideLineItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Validation functions
private fun validateInputs(title: String, amount: String, dueDate: String): Boolean {
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