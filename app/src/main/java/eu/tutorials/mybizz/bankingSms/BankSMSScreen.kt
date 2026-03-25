package eu.tutorials.mybizz.bankingSms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSMSScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { BankSMSRepository(context) }

    var smsList by remember { mutableStateOf<List<BankSMS>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var filterType by remember { mutableStateOf("ALL") } // ALL, CREDIT, DEBIT, INFO
    var searchQuery by remember { mutableStateOf("") }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            isLoading = true
            smsList = repo.getBankMessages()
            isLoading = false
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            smsList = repo.getBankMessages()
            isLoading = false
        }
    }

    // Filter list
    val filteredList = smsList.filter { sms ->
        val matchesType = filterType == "ALL" || sms.transactionType == filterType
        val matchesSearch = searchQuery.isEmpty() ||
                sms.sender.contains(searchQuery, ignoreCase = true) ||
                sms.message.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    // Summary stats
    val totalCredit = smsList.filter { it.transactionType == "CREDIT" }
        .mapNotNull { it.amount }.sum()
    val totalDebit = smsList.filter { it.transactionType == "DEBIT" }
        .mapNotNull { it.amount }.sum()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bank Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        smsList = repo.getBankMessages()
                        isLoading = false
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            if (!hasPermission) {
                // Permission Request UI
                PermissionRequestCard {
                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                }
            } else {
                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Credit",
                        amount = totalCredit,
                        color = Color(0xFF4CAF50)
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Debit",
                        amount = totalDebit,
                        color = Color(0xFFF44336)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search messages...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "CREDIT", "DEBIT", "INFO").forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (type) {
                                    "CREDIT" -> Color(0xFF4CAF50)
                                    "DEBIT" -> Color(0xFFF44336)
                                    "INFO" -> Color(0xFF2196F3)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Email,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text("No bank messages found", color = Color.Gray)
                        }
                    }
                } else {
                    Text(
                        "${filteredList.size} messages",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredList) { sms ->
                            BankSMSCard(sms)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BankSMSCard(sms: BankSMS) {
    val cardColor = when (sms.transactionType) {
        "CREDIT" -> Color(0xFFE8F5E9)
        "DEBIT"  -> Color(0xFFFFEBEE)
        else     -> Color(0xFFE3F2FD)
    }

    val typeColor = when (sms.transactionType) {
        "CREDIT" -> Color(0xFF2E7D32)
        "DEBIT"  -> Color(0xFFC62828)
        else     -> Color(0xFF1565C0)
    }

    val typeIcon = when (sms.transactionType) {
        "CREDIT" -> "↑ CREDIT"
        "DEBIT"  -> "↓ DEBIT"
        else     -> "ℹ INFO"
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender badge
                Box(
                    modifier = Modifier
                        .background(typeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        sms.sender,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Transaction Type
                Text(
                    typeIcon,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount (if extracted)
            if (sms.amount != null) {
                Text(
                    "₹${String.format("%,.2f", sms.amount)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Message body
            Text(
                sms.message,
                fontSize = 13.sp,
                color = Color.DarkGray,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Timestamp
            Text(
                dateFormat.format(Date(sms.timestamp)),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier,
    label: String,
    amount: Double,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Text(
                "₹${String.format("%,.0f", amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun PermissionRequestCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Email,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "SMS Permission Required",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "To show your bank transaction messages, please allow SMS read permission.",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
        }
    }
}