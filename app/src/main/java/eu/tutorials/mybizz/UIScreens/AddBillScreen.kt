package eu.tutorials.mybizz.UIScreens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
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
import eu.tutorials.mybizz.R

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

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_new_bill), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isLoading) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .padding(AppDimens.ScreenPadding)
        ) {

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
                    label = { Text(stringResource(R.string.amount)) },
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
                            Icon(Icons.Default.DateRange, contentDescription = "Pick date", tint = AppColors.Primary)
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
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_MONTH, days)
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                dueDate = sdf.format(cal.time)
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
                                    Toast.makeText(context, context.getString(R.string.bill_added_success), Toast.LENGTH_SHORT).show()
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
                    Text(stringResource(R.string.adding_bill))
                } else {
                    Text(stringResource(R.string.add_bill), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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

            // Performance notice
            if (isLoading) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.CardSmall,
                    color = AppColors.SurfaceMuted
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.adding_to_sheets),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.Accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.may_take_seconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            // Form Guidelines
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.CardSmall,
                color = AppColors.InfoBg
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.form_guidelines),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GuideLineItem(stringResource(R.string.bill_description))
                    GuideLineItem(stringResource(R.string.bill_amount))
                    GuideLineItem(stringResource(R.string.select_due_date))
                    GuideLineItem(stringResource(R.string.select_category))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.note_sheets_sync),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Card wrapper that groups related form fields under a small heading. */
@Composable
fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun mybizzFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Accent,
    unfocusedBorderColor = AppColors.Border,
    focusedLabelColor = AppColors.Accent,
    cursorColor = AppColors.Accent
)

@Composable
fun GuideLineItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp),
            color = AppColors.Primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary
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