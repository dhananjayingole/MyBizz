// EditBillScreen.kt
package eu.tutorials.mybizz.UIScreens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.R
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_bill), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Accent)
            }
        } else if (bill == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.bill_not_found))
            }
        } else if (bill?.status == Bill.STATUS_PAID) {
            // Show message for paid bills - cannot edit
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(AppColors.DangerBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.cannot_edit_paid),
                            modifier = Modifier.size(36.dp),
                            tint = AppColors.Danger
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.cannot_edit_paid),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cannot_edit_paid_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        shape = AppShapes.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text(stringResource(R.string.go_back))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimens.ScreenPadding)
            ) {
                // Bill Info banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.CardSmall,
                    color = AppColors.InfoBg
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Bill #${bill!!.billNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Current Version: ${bill!!.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "Editing will create version ${bill!!.version + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FormSectionCard(title = "Bill details") {
                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.bill_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors(),
                        isError = title.isBlank() && error != null,
                        supportingText = {
                            if (title.isBlank() && error != null) {
                                Text(stringResource(R.string.title_required))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amount = it
                            }
                        },
                        label = { Text(stringResource(R.string.bill_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors(),
                        prefix = { Text("₹") },
                        isError = (amount.isBlank() || amount.toDoubleOrNull() == null) && error != null,
                        supportingText = {
                            if (amount.isBlank() && error != null) {
                                Text(stringResource(R.string.amount_required))
                            } else if (amount.isNotBlank() && amount.toDoubleOrNull() == null) {
                                Text(stringResource(R.string.enter_valid_number))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                FormSectionCard(title = stringResource(R.string.due_date)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.select_due_date)) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = AppShapes.CardSmall,
                        colors = mybizzFieldColors(),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_valid_date), tint = AppColors.Primary)
                            }
                        },
                        placeholder = { Text(stringResource(R.string.tap_to_select_date)) },
                        isError = dueDate.isBlank() && error != null,
                        supportingText = {
                            if (dueDate.isBlank() && error != null) {
                                Text(stringResource(R.string.due_date_required))
                            } else if (dueDate.isNotBlank() && !isValidDate(dueDate)) {
                                Text(stringResource(R.string.select_valid_date))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick date selection buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = AppColors.SurfaceMuted
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FormSectionCard(title = stringResource(R.string.category)) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            shape = AppShapes.CardSmall,
                            colors = mybizzFieldColors(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            placeholder = { Text(stringResource(R.string.select_category)) }
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
                }

                Spacer(modifier = Modifier.height(22.dp))

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
                                        Toast.makeText(context, context.getString(R.string.bill_updated_success), Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        error = context.getString(R.string.failed_update_bill)
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    error = "Error: ${e.message}"
                                    isLoading = false
                                }
                            }
                        } else {
                            error = context.getString(R.string.please_fill_all_fields)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading,
                    shape = AppShapes.Button,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.updating_bill))
                    } else {
                        Text("Update Bill (Version ${bill!!.version + 1})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.CardSmall,
                        color = AppColors.DangerBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = stringResource(R.string.error),
                                tint = AppColors.Danger
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = AppColors.Danger,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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