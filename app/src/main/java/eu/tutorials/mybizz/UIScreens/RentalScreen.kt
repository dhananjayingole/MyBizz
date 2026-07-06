package eu.tutorials.mybizz.UIScreens

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.DatePicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import eu.tutorials.mybizz.Logic.Rental.RentalSharedViewModel
import eu.tutorials.mybizz.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalListScreen(
    sheetsRepo: RentalSheetsRepository,
    // Key change: instead of (Rental) we now pass (tenantName, tenantRentals)
    onTenantSelected: (tenantName: String, rentals: List<Rental>) -> Unit,
    onAddRental: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var allRentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            allRentals = sheetsRepo.getAllRentals()
            isLoading  = false
        }
    }

    // ── Group by tenant name (unique key) ────────────────────────────────────
    val grouped: List<Map.Entry<String, List<Rental>>> = remember(allRentals, searchQuery.text) {
        allRentals
            .filter { rental ->
                rental.tenantName.contains(searchQuery.text, ignoreCase = true) ||
                        rental.property.contains(searchQuery.text, ignoreCase = true)
            }
            .groupBy { it.tenantName.trim() }
            .entries
            .toList()
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.rental_management), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRental,
                containerColor = AppColors.Accent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_rental))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search_tenant_property)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = AppColors.TextMuted)
                },
                shape = AppShapes.CardSmall,
                colors = mybizzFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = AppColors.Accent) }

                grouped.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(stringResource(R.string.no_rentals_found), color = AppColors.TextSecondary) }

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(grouped, key = { it.key }) { (tenantName, rentals) ->
                        TenantCard(
                            tenantName = tenantName,
                            rentals    = rentals,
                            onClick    = { onTenantSelected(tenantName, rentals) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tenant summary card  —  ONE per unique tenant name
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TenantCard(
    tenantName: String,
    rentals: List<Rental>,
    onClick: () -> Unit
) {
    val paidCount   = rentals.count { it.status == Rental.STATUS_PAID }
    val unpaidCount = rentals.size - paidCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.CardSmall,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.InfoBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = tenantName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = tenantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = "${rentals.size} ${if (rentals.size == 1) "property" else "properties"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Paid / Unpaid pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (paidCount > 0)   RentalStatusPill("$paidCount Paid",   isPaid = true)
                    if (unpaidCount > 0) RentalStatusPill("$unpaidCount Unpaid", isPaid = false)
                }
            }

            Icon(
                imageVector        = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint               = AppColors.TextMuted
            )
        }
    }
}

@Composable
private fun RentalStatusPill(label: String, isPaid: Boolean) {
    val bg = if (isPaid) AppColors.SuccessBg else AppColors.DangerBg
    val fg = if (isPaid) AppColors.Success else AppColors.Danger
    Surface(
        shape = AppShapes.Chip,
        color = bg
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color    = fg
        )
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
        containerColor = AppColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_rental), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
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
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize()
        ) {
            FormSectionCard(title = "Tenant & property") {
                OutlinedTextField(
                    value = tenantName,
                    onValueChange = { tenantName = it },
                    label = { Text(stringResource(R.string.tenant_name)) },
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = property,
                    onValueChange = { property = it },
                    label = { Text(stringResource(R.string.property_shop)) },
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = contactNo,
                    onValueChange = { contactNo = it },
                    label = { Text(stringResource(R.string.contact_number)) },
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FormSectionCard(title = "Rent details") {
                OutlinedTextField(
                    value = rentAmount,
                    onValueChange = { rentAmount = it },
                    label = { Text(stringResource(R.string.rent_amount)) },
                    prefix = { Text("₹") },
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = month,
                    onValueChange = { },
                    label = { Text("Month (YYYY-MM)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Pick Month",
                            tint = AppColors.Primary,
                            modifier = Modifier.clickable { openMonthYearPicker() }
                        )
                    },
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openMonthYearPicker() },
                    readOnly = true
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppShapes.Button,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text(stringResource(R.string.save_rental), fontWeight = FontWeight.SemiBold)
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
    val isPaid = rental.status == Rental.STATUS_PAID

    var showPaymentOptions by remember { mutableStateOf(false) }

    // Function to handle call
    fun makePhoneCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context.startActivity(intent)
            } else {
                showCallDialog = true
            }
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(dialIntent)
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.rental_details), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
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
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rental Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.Card,
                color = if (isPaid) AppColors.SuccessBg else AppColors.DangerBg
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            rental.tenantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(rental.property, color = AppColors.TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "₹${rental.rentAmount}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Month: ${rental.month}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }

                    // Contact info with clickable call button
                    if (rental.contactNo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.CardSmall,
                            color = AppColors.Surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { makePhoneCall(rental.contactNo) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rental.contactNo, fontSize = 16.dp.value.sp, color = AppColors.TextPrimary)
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = AppColors.Accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (rental.paymentDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Payment Date: ${rental.paymentDate}", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Status: ${rental.status.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) AppColors.Success else AppColors.Danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Payment Action Section
            if (!isPaid) {
                Button(
                    onClick = { showPaymentOptions = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = AppShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Success
                    )
                ) {
                    Icon(Icons.Default.AccountBox, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pay Now - ₹${String.format("%.2f", rental.rentAmount)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showMarkPaidDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = AppShapes.Button,
                    border = BorderStroke(1.dp, AppColors.Primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.mark_paid_button), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_paid_manual))
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Card,
                    color = AppColors.InfoBg
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
                            tint = AppColors.Primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                stringResource(R.string.rent_paid),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AppColors.Primary
                            )
                            if (rental.paymentDate.isNotEmpty()) {
                                Text(
                                    "Paid on: ${rental.paymentDate}",
                                    fontSize = 14.sp,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isPaid) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Button,
                        border = BorderStroke(1.dp, AppColors.Border)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.edit), color = AppColors.Primary)
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = AppShapes.Button,
                    border = BorderStroke(1.dp, AppColors.Danger.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Danger
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
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
                    tint = AppColors.Danger,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text(stringResource(R.string.delete_rental), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_rental_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Danger
                    ),
                    shape = AppShapes.Button
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
                }
            }
        )
    }

    // Manual Mark as Paid Dialog
    if (showMarkPaidDialog) {
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = false },
            title = { Text(stringResource(R.string.mark_as_paid), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.mark_paid_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showMarkPaidDialog = false
                    onMarkPaid()
                }) {
                    Text(stringResource(R.string.mark_paid_title), color = AppColors.Success, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
                }
            }
        )
    }

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
            title = { Text(stringResource(R.string.call_permission_required), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.call_permission_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showCallDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings", color = AppColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCallDialog = false
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${rental.contactNo}")
                    }
                    context.startActivity(dialIntent)
                }) {
                    Text(stringResource(R.string.use_dialer), color = AppColors.TextSecondary)
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
        containerColor = AppColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_rental), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
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
                .padding(AppDimens.ScreenPadding)
                .fillMaxSize()
        ) {
            FormSectionCard(title = "Rental details") {
                OutlinedTextField(value = tenantName, onValueChange = { tenantName = it }, label = { Text(stringResource(R.string.tenant_name)) }, shape = AppShapes.CardSmall, colors = mybizzFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = property, onValueChange = { property = it }, label = { Text(stringResource(R.string.property_shop)) }, shape = AppShapes.CardSmall, colors = mybizzFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = rentAmount, onValueChange = { rentAmount = it }, label = { Text(stringResource(R.string.rent_amount)) }, prefix = { Text("₹") }, shape = AppShapes.CardSmall, colors = mybizzFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = { Text("Month (YYYY-MM)") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.date), tint = AppColors.Primary) },
                    shape = AppShapes.CardSmall,
                    colors = mybizzFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = contactNo, onValueChange = { contactNo = it }, label = { Text(stringResource(R.string.contact_number)) }, shape = AppShapes.CardSmall, colors = mybizzFieldColors(), modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(22.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppShapes.Button,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text(stringResource(R.string.update_rental), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantPropertiesScreen(
    viewModel: RentalSharedViewModel,
    onPropertySelected: (Rental) -> Unit,
    onBack: () -> Unit
) {
    val tenantName = viewModel.selectedTenantName
    val rentals    = viewModel.selectedTenantRentals

    val totalRent   = rentals.sumOf { it.rentAmount }
    val paidCount   = rentals.count { it.status == Rental.STATUS_PAID }
    val unpaidCount = rentals.size - paidCount

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tenantName, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (rentals.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No properties found for $tenantName", color = AppColors.TextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier
                .padding(padding)
                .padding(horizontal = AppDimens.ScreenPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding      = PaddingValues(vertical = 16.dp)
        ) {

            // ── Summary header ──────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Card)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = tenantName.take(1).uppercase(),
                                    color      = Color.White,
                                    fontSize   = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text       = tenantName,
                                    style      = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                                Text(
                                    text  = "Total rent: ₹$totalRent / month",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(label = "Properties", value = "${rentals.size}", valueColor = Color.White)
                            StatBox(label = "Paid",       value = "$paidCount",   valueColor = Color.White)
                            StatBox(label = "Unpaid",     value = "$unpaidCount", valueColor = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text  = "Rented Properties",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── One card per property ───────────────────────────────────────
            items(rentals, key = { it.id }) { rental ->
                PropertyDetailCard(
                    rental  = rental,
                    onClick = { onPropertySelected(rental) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Individual property card  (child node)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PropertyDetailCard(rental: Rental, onClick: () -> Unit) {
    val isPaid = rental.status == Rental.STATUS_PAID

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.CardSmall,
        colors    = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppColors.Border)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: property icon + details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(if (isPaid) AppColors.SuccessBg else AppColors.DangerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = if (isPaid) AppColors.Success else AppColors.Danger,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text       = rental.property,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text  = "₹${rental.rentAmount}  •  ${rental.month}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    if (rental.paymentDate.isNotEmpty()) {
                        Text(
                            text  = "Paid on: ${rental.paymentDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextMuted
                        )
                    }
                }
            }

            // Right: status pill + chevron
            Column(horizontalAlignment = Alignment.End) {
                RentalStatusPill(if (isPaid) "PAID" else "UNPAID", isPaid = isPaid)
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector        = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint               = AppColors.TextMuted,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Small stat box used in the summary card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatBox(
    label: String,
    value: String,
    valueColor: Color = AppColors.TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor.copy(alpha = 0.75f)
        )
    }
}