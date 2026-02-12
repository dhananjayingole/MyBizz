package eu.tutorials.mybizz.Payments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder

class UpiPaymentHandler(
    private val activity: Activity
) {
    companion object {
        private const val TAG = "UpiPayment"
        const val UPI_PAYMENT_REQUEST_CODE = 1001

        // Replace with your actual UPI ID
        private const val MERCHANT_UPI_ID = "merchant@paytm" // Your UPI ID
        private const val MERCHANT_NAME = "MyBiz" // Your business name
    }

    fun initiateUpiPayment(
        amount: Double,
        transactionNote: String,
        transactionId: String
    ) {
        try {
            val uri = Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", MERCHANT_UPI_ID)
                .appendQueryParameter("pn", URLEncoder.encode(MERCHANT_NAME, "UTF-8"))
                .appendQueryParameter("tr", transactionId) // Transaction reference ID
                .appendQueryParameter("am", amount.toString())
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", URLEncoder.encode(transactionNote, "UTF-8"))
                .build()

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri

            // Check if UPI apps are available
            val packageManager = activity.packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)

            if (activities.isEmpty()) {
                Toast.makeText(activity, "No UPI app found. Please install a UPI app.", Toast.LENGTH_LONG).show()
                return
            }

            // Choose UPI app
            val chooser = Intent.createChooser(intent, "Pay with")
            activity.startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE)

            Log.d(TAG, "UPI payment initiated for amount: ₹$amount")

        } catch (e: Exception) {
            Log.e(TAG, "Error initiating UPI payment: ${e.message}", e)
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleUpiResponse(data: Intent?): UpiPaymentResult {
        return try {
            if (data == null) {
                UpiPaymentResult(false, "No response received", null)
            } else {
                val response = data.getStringExtra("response") ?: ""
                Log.d(TAG, "UPI Response: $response")

                val responseMap = parseUpiResponse(response)

                val status = responseMap["Status"]?.lowercase()
                val txnId = responseMap["txnId"]
                val txnRef = responseMap["txnRef"]

                when (status) {
                    "success" -> {
                        UpiPaymentResult(
                            success = true,
                            message = "Payment successful",
                            transactionId = txnId ?: txnRef
                        )
                    }
                    "failure" -> {
                        UpiPaymentResult(
                            success = false,
                            message = "Payment failed",
                            transactionId = null
                        )
                    }
                    "submitted" -> {
                        UpiPaymentResult(
                            success = false,
                            message = "Payment pending",
                            transactionId = txnId ?: txnRef
                        )
                    }
                    else -> {
                        UpiPaymentResult(
                            success = false,
                            message = "Payment status unknown",
                            transactionId = null
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling UPI response: ${e.message}", e)
            UpiPaymentResult(false, "Error: ${e.message}", null)
        }
    }

    private fun parseUpiResponse(response: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val pairs = response.split("&")
            for (pair in pairs) {
                val keyValue = pair.split("=")
                if (keyValue.size == 2) {
                    map[keyValue[0]] = keyValue[1]
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UPI response: ${e.message}")
        }
        return map
    }
}

data class UpiPaymentResult(
    val success: Boolean,
    val message: String,
    val transactionId: String?
)