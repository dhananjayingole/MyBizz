package eu.tutorials.mybizz.Model

import java.util.*

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String = "", // Payment gateway transaction ID
    val billId: String = "", // Link to bill
    val rentalId: String = "", // Link to rental
    val paymentType: String = "", // "BILL" or "RENTAL"
    val amount: Double = 0.0,
    val paymentMethod: String = "", // "UPI", "CARD", "NET_BANKING", "WALLET"
    val status: String = STATUS_PENDING,
    val payerName: String = "",
    val payerEmail: String = "",
    val payerPhone: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val paymentDate: String = "",
    val razorpayOrderId: String = "", // Razorpay specific
    val razorpayPaymentId: String = "", // Razorpay specific
    val razorpaySignature: String = "", // Razorpay specific
    val upiTransactionId: String = "", // UPI specific
    val failureReason: String = "",
    val notes: String = ""
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAILED = "failed"

        const val TYPE_BILL = "BILL"
        const val TYPE_RENTAL = "RENTAL"

        const val METHOD_UPI = "UPI"
        const val METHOD_CARD = "CARD"
        const val METHOD_NET_BANKING = "NET_BANKING"
        const val METHOD_WALLET = "WALLET"
    }
}

// Payment summary for reports
data class PaymentSummary(
    val totalAmount: Double = 0.0,
    val successfulPayments: Int = 0,
    val failedPayments: Int = 0,
    val pendingPayments: Int = 0,
    val lastPaymentDate: String = "",
    val paymentsByMethod: Map<String, Int> = emptyMap()
)
