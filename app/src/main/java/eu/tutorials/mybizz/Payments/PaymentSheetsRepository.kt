package eu.tutorials.mybizz.Payments

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import eu.tutorials.mybizz.Model.Payment
import eu.tutorials.mybizz.Model.PaymentSummary
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class PaymentSheetsRepository(private val context: Context) {
    companion object {
        private const val TAG = "PaymentSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val PAYMENTS_SHEET_NAME = "Payments"
        private const val RANGE = "A:P"
        private const val API_TIMEOUT_SECONDS = 20L

        // Column indices
        private const val COL_ID = 0
        private const val COL_TRANSACTION_ID = 1
        private const val COL_BILL_ID = 2
        private const val COL_RENTAL_ID = 3
        private const val COL_PAYMENT_TYPE = 4
        private const val COL_AMOUNT = 5
        private const val COL_PAYMENT_METHOD = 6
        private const val COL_STATUS = 7
        private const val COL_PAYER_NAME = 8
        private const val COL_PAYER_EMAIL = 9
        private const val COL_PAYER_PHONE = 10
        private const val COL_PAYMENT_DATE = 11
        private const val COL_RAZORPAY_PAYMENT_ID = 12
        private const val COL_UPI_TRANSACTION_ID = 13
        private const val COL_FAILURE_REASON = 14
        private const val COL_NOTES = 15
    }

    private var sheetsService: Sheets? = null

    private fun getSheetsService(): Sheets? {
        return sheetsService ?: try {
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val credentialsStream = context.resources.openRawResource(R.raw.service_account)
            val credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(listOf(SheetsScopes.SPREADSHEETS))

            Sheets.Builder(transport, jsonFactory, HttpCredentialsAdapter(credentials))
                .setApplicationName("MyBiz App")
                .build()
                .also { sheetsService = it }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Sheets service: ${e.message}", e)
            null
        }
    }

    // Ensure Payments sheet exists
    private suspend fun ensurePaymentsSheetExists(service: Sheets) {
        try {
            val spreadsheet = service.spreadsheets().get(SPREADSHEET_ID).execute()
            val sheetExists = spreadsheet.sheets.any { it.properties.title == PAYMENTS_SHEET_NAME }

            if (!sheetExists) {
                // Create payments sheet
                val addSheetRequest = Request().setAddSheet(
                    AddSheetRequest().setProperties(
                        SheetProperties().setTitle(PAYMENTS_SHEET_NAME)
                    )
                )

                val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(addSheetRequest))

                service.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute()

                // Add headers
                val headers = listOf(
                    listOf(
                        "ID", "Transaction ID", "Bill ID", "Rental ID", "Payment Type",
                        "Amount", "Payment Method", "Status", "Payer Name", "Payer Email",
                        "Payer Phone", "Payment Date", "Razorpay Payment ID",
                        "UPI Transaction ID", "Failure Reason", "Notes"
                    )
                )
                val headerRequest = ValueRange().setValues(headers)
                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, "$PAYMENTS_SHEET_NAME!A1:P1", headerRequest)
                    .setValueInputOption("RAW")
                    .execute()

                Log.d(TAG, "Created Payments sheet with headers")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring Payments sheet exists: ${e.message}", e)
        }
    }

    // Add payment record
    suspend fun addPayment(payment: Payment): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false
                ensurePaymentsSheetExists(service)

                val values = listOf(
                    payment.id,
                    payment.transactionId,
                    payment.billId,
                    payment.rentalId,
                    payment.paymentType,
                    payment.amount.toString(),
                    payment.paymentMethod,
                    payment.status,
                    payment.payerName,
                    payment.payerEmail,
                    payment.payerPhone,
                    payment.paymentDate,
                    payment.razorpayPaymentId,
                    payment.upiTransactionId,
                    payment.failureReason,
                    payment.notes
                )

                val request = ValueRange().setValues(listOf(values))
                service.spreadsheets().values()
                    .append(SPREADSHEET_ID, "$PAYMENTS_SHEET_NAME!$RANGE", request)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                Log.d(TAG, "Payment record added successfully: ${payment.id}")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding payment: ${e.message}", e)
            false
        }
    }

    // Get all payments
    suspend fun getAllPayments(): List<Payment> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout emptyList()
                ensurePaymentsSheetExists(service)

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$PAYMENTS_SHEET_NAME!$RANGE")
                    .execute()

                val values = response.getValues()
                if (values.isNullOrEmpty() || values.size <= 1) {
                    return@withTimeout emptyList()
                }

                val payments = mutableListOf<Payment>()
                for (i in 1 until values.size) {
                    try {
                        val row = values[i]
                        if (row.size > COL_ID && row[COL_ID].toString().isNotBlank()) {
                            val payment = Payment(
                                id = row[COL_ID].toString(),
                                transactionId = row.getOrNull(COL_TRANSACTION_ID)?.toString() ?: "",
                                billId = row.getOrNull(COL_BILL_ID)?.toString() ?: "",
                                rentalId = row.getOrNull(COL_RENTAL_ID)?.toString() ?: "",
                                paymentType = row.getOrNull(COL_PAYMENT_TYPE)?.toString() ?: "",
                                amount = row.getOrNull(COL_AMOUNT)?.toString()?.toDoubleOrNull() ?: 0.0,
                                paymentMethod = row.getOrNull(COL_PAYMENT_METHOD)?.toString() ?: "",
                                status = row.getOrNull(COL_STATUS)?.toString() ?: Payment.STATUS_PENDING,
                                payerName = row.getOrNull(COL_PAYER_NAME)?.toString() ?: "",
                                payerEmail = row.getOrNull(COL_PAYER_EMAIL)?.toString() ?: "",
                                payerPhone = row.getOrNull(COL_PAYER_PHONE)?.toString() ?: "",
                                paymentDate = row.getOrNull(COL_PAYMENT_DATE)?.toString() ?: "",
                                razorpayPaymentId = row.getOrNull(COL_RAZORPAY_PAYMENT_ID)?.toString() ?: "",
                                upiTransactionId = row.getOrNull(COL_UPI_TRANSACTION_ID)?.toString() ?: "",
                                failureReason = row.getOrNull(COL_FAILURE_REASON)?.toString() ?: "",
                                notes = row.getOrNull(COL_NOTES)?.toString() ?: ""
                            )
                            payments.add(payment)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping invalid payment row $i: ${e.message}")
                    }
                }

                Log.d(TAG, "Loaded ${payments.size} payment records")
                payments
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payments: ${e.message}", e)
            emptyList()
        }
    }

    // Get payments by bill ID
    suspend fun getPaymentsByBillId(billId: String): List<Payment> {
        val allPayments = getAllPayments()
        return allPayments.filter { it.billId == billId }
    }

    // Get payments by rental ID
    suspend fun getPaymentsByRentalId(rentalId: String): List<Payment> {
        val allPayments = getAllPayments()
        return allPayments.filter { it.rentalId == rentalId }
    }

    // Get payment summary
    suspend fun getPaymentSummary(): PaymentSummary = withContext(Dispatchers.IO) {
        try {
            val allPayments = getAllPayments()

            val totalAmount = allPayments
                .filter { it.status == Payment.STATUS_SUCCESS }
                .sumOf { it.amount }

            val successfulPayments = allPayments.count { it.status == Payment.STATUS_SUCCESS }
            val failedPayments = allPayments.count { it.status == Payment.STATUS_FAILED }
            val pendingPayments = allPayments.count { it.status == Payment.STATUS_PENDING }

            val lastPayment = allPayments
                .filter { it.status == Payment.STATUS_SUCCESS }
                .maxByOrNull { it.timestamp }

            val paymentsByMethod = allPayments
                .filter { it.status == Payment.STATUS_SUCCESS }
                .groupBy { it.paymentMethod }
                .mapValues { it.value.size }

            PaymentSummary(
                totalAmount = totalAmount,
                successfulPayments = successfulPayments,
                failedPayments = failedPayments,
                pendingPayments = pendingPayments,
                lastPaymentDate = lastPayment?.paymentDate ?: "",
                paymentsByMethod = paymentsByMethod
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating payment summary: ${e.message}", e)
            PaymentSummary()
        }
    }
}