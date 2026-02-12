package eu.tutorials.mybizz.Payments

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.Payment
import eu.tutorials.mybizz.Model.Rental

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bill: Bill? = null,
    rental: Rental? = null,
    onPaymentSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: PaymentViewModel = viewModel()

    var payerName by remember { mutableStateOf("") }
    var payerEmail by remember { mutableStateOf("") }
    var payerPhone by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    val paymentState by viewModel.paymentState.collectAsState()

    // Calculate amount and description
    val amount = bill?.amount ?: rental?.rentAmount ?: 0.0
    val description = bill?.title ?: "Rent for ${rental?.month} - ${rental?.property}"
    val itemId = bill?.id ?: rental?.id ?: ""
    val itemType = if (bill != null) "Bill" else "Rental"

    // UPI Payment Handler
    val upiHandler = remember { UpiPaymentHandler(activity) }

    // Handle UPI response
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
            val upiResult = upiHandler.handleUpiResponse(result.data)

            if (upiResult.success) {
                viewModel.processPaymentSuccess(
                    paymentId = upiResult.transactionId ?: "",
                    transactionId = upiResult.transactionId ?: "",
                    billId = bill?.id,
                    rentalId = rental?.id,
                    amount = amount,
                    paymentMethod = Payment.METHOD_UPI,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    payerPhone = payerPhone,
                    upiTransactionId = upiResult.transactionId ?: ""
                )
            } else {
                viewModel.processPaymentFailure(
                    billId = bill?.id,
                    rentalId = rental?.id,
                    amount = amount,
                    paymentMethod = Payment.METHOD_UPI,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    payerPhone = payerPhone,
                    failureReason = upiResult.message
                )
            }
        }
    }

    // Razorpay Payment Handler
    val razorpayHandler = remember {
        RazorpayPaymentHandler(
            activity = activity,
            onPaymentSuccess = { paymentId, orderId, signature ->
                viewModel.processPaymentSuccess(
                    paymentId = paymentId,
                    transactionId = paymentId,
                    billId = bill?.id,
                    rentalId = rental?.id,
                    amount = amount,
                    paymentMethod = selectedPaymentMethod,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    payerPhone = payerPhone,
                    razorpayPaymentId = paymentId
                )
            },
            onPaymentFailure = { error ->
                viewModel.processPaymentFailure(
                    billId = bill?.id,
                    rentalId = rental?.id,
                    amount = amount,
                    paymentMethod = selectedPaymentMethod,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    payerPhone = payerPhone,
                    failureReason = error
                )
            }
        )
    }

    // Handle payment state changes
    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentState.Success -> {
                Toast.makeText(context, "Payment successful!", Toast.LENGTH_LONG).show()
                showDialog = true
            }
            is PaymentState.Failed -> {
                val reason = (paymentState as PaymentState.Failed).reason
                Toast.makeText(context, "Payment failed: $reason", Toast.LENGTH_LONG).show()
            }
            is PaymentState.Error -> {
                val error = (paymentState as PaymentState.Error).message
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pay $itemType") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Payment Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = description,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Amount to Pay",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "₹ ${String.format("%.2f", amount)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payer Details
            Text(
                text = "Payer Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = payerName,
                onValueChange = { payerName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, "Name") }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = payerEmail,
                onValueChange = { payerEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, "Email") }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = payerPhone,
                onValueChange = { payerPhone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Method Selection
            Text(
                text = "Select Payment Method",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // UPI Payment Button
            PaymentMethodButton(
                icon = Icons.Default.AccountBox,
                title = "UPI Payment",
                subtitle = "PhonePe, Google Pay, Paytm",
                isSelected = selectedPaymentMethod == Payment.METHOD_UPI,
                onClick = { selectedPaymentMethod = Payment.METHOD_UPI }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Razorpay (Card/Net Banking/Wallet)
            PaymentMethodButton(
                icon = Icons.Default.Info,
                title = "Card / Net Banking",
                subtitle = "Debit Card, Credit Card, Net Banking",
                isSelected = selectedPaymentMethod == Payment.METHOD_CARD,
                onClick = { selectedPaymentMethod = Payment.METHOD_CARD }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pay Button
            Button(
                onClick = {
                    if (payerName.isBlank() || payerEmail.isBlank() || payerPhone.isBlank()) {
                        Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedPaymentMethod.isEmpty()) {
                        Toast.makeText(context, "Please select payment method", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    when (selectedPaymentMethod) {
                        Payment.METHOD_UPI -> {
                            val transactionId = "TXN${System.currentTimeMillis()}"
                            upiHandler.initiateUpiPayment(
                                amount = amount,
                                transactionNote = description,
                                transactionId = transactionId
                            )
                        }
                        Payment.METHOD_CARD -> {
                            razorpayHandler.initiatePayment(
                                amount = amount,
                                itemDescription = description,
                                payerName = payerName,
                                payerEmail = payerEmail,
                                payerPhone = payerPhone
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = paymentState !is PaymentState.Processing
            ) {
                if (paymentState is PaymentState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Place, "Pay")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay ₹${String.format("%.2f", amount)}")
                }
            }
        }
    }

    // Success Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(64.dp)
                )
            },
            title = { Text("Payment Successful!") },
            text = { Text("Your payment of ₹${String.format("%.2f", amount)} was successful.") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    viewModel.resetPaymentState()
                    onPaymentSuccess()
                }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun PaymentMethodButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}