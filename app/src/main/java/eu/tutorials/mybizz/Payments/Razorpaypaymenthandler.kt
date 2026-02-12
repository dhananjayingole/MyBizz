package eu.tutorials.mybizz.Payments

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class RazorpayPaymentHandler(
    private val activity: Activity,
    private val onPaymentSuccess: (String, String, String) -> Unit, // (paymentId, orderId, signature)
    private val onPaymentFailure: (String) -> Unit
) : PaymentResultListener {

    companion object {
        private const val TAG = "RazorpayPayment"
        // IMPORTANT: Replace with your actual Razorpay API Key
        private const val RAZORPAY_KEY = "rzp_test_YOUR_KEY_HERE" // Get from Razorpay Dashboard
    }

    init {
        // Preload Razorpay for faster checkout
        Checkout.preload(activity.applicationContext)
    }

    fun initiatePayment(
        amount: Double, // Amount in rupees
        itemDescription: String,
        payerName: String,
        payerEmail: String,
        payerPhone: String,
        orderId: String? = null
    ) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY)

            val options = JSONObject()

            // Convert amount to paise (1 Rupee = 100 Paise)
            val amountInPaise = (amount * 100).toInt()
            options.put("amount", amountInPaise)
            options.put("currency", "INR")
            options.put("name", "MyBiz Payment")
            options.put("description", itemDescription)

            // Add order ID if provided
            if (!orderId.isNullOrEmpty()) {
                options.put("order_id", orderId)
            }

            // Prefill customer details
            val prefill = JSONObject()
            prefill.put("name", payerName)
            prefill.put("email", payerEmail)
            prefill.put("contact", payerPhone)
            options.put("prefill", prefill)

            // Theme customization
            val theme = JSONObject()
            theme.put("color", "#3399cc")
            options.put("theme", theme)

            // Payment methods
            val method = JSONObject()
            method.put("card", true)
            method.put("netbanking", true)
            method.put("wallet", true)
            method.put("upi", true)
            options.put("method", method)

            // Notes for reference
            val notes = JSONObject()
            notes.put("app", "MyBiz")
            notes.put("description", itemDescription)
            options.put("notes", notes)

            Log.d(TAG, "Opening Razorpay checkout for amount: ₹$amount")
            checkout.open(activity, options)

        } catch (e: Exception) {
            Log.e(TAG, "Error initiating payment: ${e.message}", e)
            Toast.makeText(activity, "Error initiating payment: ${e.message}", Toast.LENGTH_SHORT).show()
            onPaymentFailure("Error: ${e.message}")
        }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        Log.d(TAG, "Payment Success - ID: $paymentId")
        if (paymentId != null) {
            // Payment successful
            onPaymentSuccess(paymentId, "", "")
        } else {
            onPaymentFailure("Payment ID is null")
        }
    }

    override fun onPaymentError(errorCode: Int, errorMessage: String?) {
        Log.e(TAG, "Payment Failed - Code: $errorCode, Message: $errorMessage")
        val message = errorMessage ?: "Payment failed with code: $errorCode"
        onPaymentFailure(message)
    }
}