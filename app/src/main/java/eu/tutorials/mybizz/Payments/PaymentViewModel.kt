package eu.tutorials.mybizz.Payments

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Model.Payment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PaymentViewModel"
    }

    private val paymentRepo = PaymentSheetsRepository(application)
    private val billRepo = BillSheetsRepository(application)
    private val rentalRepo = RentalSheetsRepository(application)

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments

    // Process successful payment
    fun processPaymentSuccess(
        paymentId: String,
        transactionId: String,
        billId: String?,
        rentalId: String?,
        amount: Double,
        paymentMethod: String,
        payerName: String,
        payerEmail: String,
        payerPhone: String,
        razorpayPaymentId: String = "",
        upiTransactionId: String = ""
    ) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Processing

                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                // Create payment record
                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    billId = billId ?: "",
                    rentalId = rentalId ?: "",
                    paymentType = if (billId != null) Payment.TYPE_BILL else Payment.TYPE_RENTAL,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    status = Payment.STATUS_SUCCESS,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    payerPhone = payerPhone,
                    paymentDate = currentDate,
                    razorpayPaymentId = razorpayPaymentId,
                    upiTransactionId = upiTransactionId,
                    timestamp = System.currentTimeMillis()
                )

                // Save payment record
                val paymentSaved = paymentRepo.addPayment(payment)

                if (paymentSaved) {
                    // Mark bill or rental as paid
                    if (billId != null) {
                        billRepo.markBillAsPaid(billId, payerEmail)
                        Log.d(TAG, "Bill $billId marked as paid")
                    } else if (rentalId != null) {
                        rentalRepo.markRentalAsPaid(rentalId)
                        Log.d(TAG, "Rental $rentalId marked as paid")
                    }

                    _paymentState.value = PaymentState.Success(payment)
                    Log.d(TAG, "Payment processed successfully: $paymentId")
                } else {
                    _paymentState.value = PaymentState.Error("Failed to save payment record")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment: ${e.message}", e)
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Process failed payment
    fun processPaymentFailure(
        billId: String?,
        rentalId: String?,
        amount: Double,
        paymentMethod: String,
        payerName: String,
        payerEmail: String,
        payerPhone: String,
        failureReason: String
    ) {
        viewModelScope.launch {
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                // Create failed payment record
                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    transactionId = "FAILED_${System.currentTimeMillis()}",
                    billId = billId ?: "",
                    rentalId = rentalId ?: "",
                    paymentType = if (billId != null) Payment.TYPE_BILL else Payment.TYPE_RENTAL,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    status = Payment.STATUS_FAILED,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    payerPhone = payerPhone,
                    paymentDate = currentDate,
                    failureReason = failureReason,
                    timestamp = System.currentTimeMillis()
                )

                // Save failed payment record
                paymentRepo.addPayment(payment)
                _paymentState.value = PaymentState.Failed(failureReason)

                Log.d(TAG, "Payment failure recorded: $failureReason")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording payment failure: ${e.message}", e)
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Load all payments
    fun loadAllPayments() {
        viewModelScope.launch {
            try {
                val allPayments = paymentRepo.getAllPayments()
                _payments.value = allPayments
                Log.d(TAG, "Loaded ${allPayments.size} payments")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading payments: ${e.message}", e)
            }
        }
    }

    // Load payments for a specific bill
    fun loadPaymentsForBill(billId: String) {
        viewModelScope.launch {
            try {
                val billPayments = paymentRepo.getPaymentsByBillId(billId)
                _payments.value = billPayments
                Log.d(TAG, "Loaded ${billPayments.size} payments for bill $billId")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bill payments: ${e.message}", e)
            }
        }
    }

    // Load payments for a specific rental
    fun loadPaymentsForRental(rentalId: String) {
        viewModelScope.launch {
            try {
                val rentalPayments = paymentRepo.getPaymentsByRentalId(rentalId)
                _payments.value = rentalPayments
                Log.d(TAG, "Loaded ${rentalPayments.size} payments for rental $rentalId")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading rental payments: ${e.message}", e)
            }
        }
    }

    // Reset payment state
    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }
}

// Payment states
sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val payment: Payment) : PaymentState()
    data class Failed(val reason: String) : PaymentState()
    data class Error(val message: String) : PaymentState()
}