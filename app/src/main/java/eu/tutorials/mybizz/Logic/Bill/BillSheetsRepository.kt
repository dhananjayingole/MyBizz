package eu.tutorials.mybizz.Logic.Bill

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class BillSheetsRepository(private val context: Context) {
    companion object {
        private const val TAG = "BillSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val BILLS_SHEET_NAME = "Sheet1"
        private const val RANGE = "A:K"
        private const val API_TIMEOUT_SECONDS = 20L

        // Column indices - UPDATED to include paidBy at correct position
        private const val COL_ID = 0
        private const val COL_TITLE = 1
        private const val COL_DESCRIPTION = 2
        private const val COL_AMOUNT = 3
        private const val COL_DUE_DATE = 4
        private const val COL_STATUS = 5
        private const val COL_CATEGORY = 6
        private const val COL_PAID_DATE = 7
        private const val COL_PAID_BY = 8 // This is the correct position
        private const val COL_CREATED_DATE = 9
        private const val COL_CREATED_BY = 10
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

    suspend fun getAllBills(): List<Bill> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                Log.d(TAG, "Fetching all bills from Google Sheets")
                val service = getSheetsService() ?: throw Exception("Sheets service unavailable")

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$BILLS_SHEET_NAME!$RANGE")
                    .execute()

                val values = response.getValues()
                if (values.isNullOrEmpty() || values.size <= 1) {
                    Log.d(TAG, "No bills data found")
                    return@withTimeout emptyList()
                }

                // Skip header row and map to Bill objects
                val bills = mutableListOf<Bill>()
                for (i in 1 until values.size) {
                    try {
                        val row = values[i]
                        if (row.size > COL_ID && row[COL_ID].toString().isNotBlank()) {
                            val bill = Bill(
                                id = row[COL_ID].toString(),
                                title = row.getOrNull(COL_TITLE)?.toString() ?: "",
                                description = row.getOrNull(COL_DESCRIPTION)?.toString() ?: "",
                                amount = row.getOrNull(COL_AMOUNT)?.toString()?.toDoubleOrNull() ?: 0.0,
                                dueDate = row.getOrNull(COL_DUE_DATE)?.toString() ?: "",
                                status = row.getOrNull(COL_STATUS)?.toString() ?: Bill.STATUS_UNPAID,
                                category = row.getOrNull(COL_CATEGORY)?.toString() ?: "",
                                paidDate = row.getOrNull(COL_PAID_DATE)?.toString() ?: "",
                                paidBy = row.getOrNull(COL_PAID_BY)?.toString() ?: "", // Correctly reading paidBy
                                createdDate = row.getOrNull(COL_CREATED_DATE)?.toString() ?: "",
                                createdBy = row.getOrNull(COL_CREATED_BY)?.toString() ?: ""
                            )
                            bills.add(bill)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping invalid row $i: ${e.message}")
                    }
                }

                Log.d(TAG, "Successfully parsed ${bills.size} bills")
                bills
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bills: ${e.message}", e)
            throw e
        }
    }

    suspend fun getBillById(billId: String): Bill? = withContext(Dispatchers.IO) {
        try {
            val allBills = getAllBills()
            allBills.find { it.id == billId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bill by ID: $billId", e)
            null
        }
    }

    suspend fun addBill(bill: Bill): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // CORRECTED: Include paidBy field at position 8
                val values = listOf(
                    bill.id,                    // A - COL_ID (0)
                    bill.title,                 // B - COL_TITLE (1)
                    bill.description,           // C - COL_DESCRIPTION (2)
                    bill.amount.toString(),     // D - COL_AMOUNT (3)
                    bill.dueDate,               // E - COL_DUE_DATE (4)
                    bill.status,                // F - COL_STATUS (5)
                    bill.category,              // G - COL_CATEGORY (6)
                    bill.paidDate,              // H - COL_PAID_DATE (7)
                    bill.paidBy,                // I - COL_PAID_BY (8) - THIS WAS MISSING!
                    bill.createdDate,           // J - COL_CREATED_DATE (9)
                    bill.createdBy              // K - COL_CREATED_BY (10)
                )

                val request = ValueRange().setValues(listOf(values))
                val response = service.spreadsheets().values()
                    .append(SPREADSHEET_ID, "$BILLS_SHEET_NAME!$RANGE", request)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                Log.d(TAG, "Bill added successfully with paidBy field")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bill: ${e.message}", e)
            false
        }
    }

    suspend fun updateBill(bill: Bill): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // First, find the correct row by scanning the ID column
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$BILLS_SHEET_NAME!A:A")
                    .execute()

                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == bill.id) {
                        rowIndex = i + 1 // +1 because Sheets is 1-indexed
                        break
                    }
                }

                if (rowIndex == -1) {
                    Log.e(TAG, "Bill not found for update: ${bill.id}")
                    return@withTimeout false
                }

                // CORRECTED: Include paidBy field at position 8
                val valuesToUpdate = listOf(
                    bill.id,                    // A - COL_ID (0)
                    bill.title,                 // B - COL_TITLE (1)
                    bill.description,           // C - COL_DESCRIPTION (2)
                    bill.amount.toString(),     // D - COL_AMOUNT (3)
                    bill.dueDate,               // E - COL_DUE_DATE (4)
                    bill.status,                // F - COL_STATUS (5)
                    bill.category,              // G - COL_CATEGORY (6)
                    bill.paidDate,              // H - COL_PAID_DATE (7)
                    bill.paidBy,                // I - COL_PAID_BY (8) - THIS WAS MISSING!
                    bill.createdDate,           // J - COL_CREATED_DATE (9)
                    bill.createdBy              // K - COL_CREATED_BY (10)
                )

                val request = ValueRange().setValues(listOf(valuesToUpdate))
                // CORRECTED: Update range to include column K (11 columns total)
                val range = "$BILLS_SHEET_NAME!A$rowIndex:K$rowIndex"

                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, range, request)
                    .setValueInputOption("RAW")
                    .execute()

                Log.d(TAG, "Bill updated successfully at row $rowIndex with paidBy: ${bill.paidBy}")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bill: ${e.message}", e)
            false
        }
    }

    suspend fun deleteBill(billId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // Find the row by scanning the ID column
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$BILLS_SHEET_NAME!A:A")
                    .execute()

                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == billId) {
                        rowIndex = i + 1
                        break
                    }
                }

                if (rowIndex == -1) {
                    Log.e(TAG, "Bill not found for deletion: $billId")
                    return@withTimeout false
                }

                // Clear the entire row (all 11 columns)
                val clearRequest = com.google.api.services.sheets.v4.model.ClearValuesRequest()
                val range = "$BILLS_SHEET_NAME!A$rowIndex:K$rowIndex"

                service.spreadsheets().values()
                    .clear(SPREADSHEET_ID, range, clearRequest)
                    .execute()

                Log.d(TAG, "Bill deleted successfully from row $rowIndex")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting bill: ${e.message}", e)
            false
        }
    }

    suspend fun markBillAsPaid(billId: String, paidByEmail: String): Boolean {
        val bill = getBillById(billId) ?: return false

        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val updatedBill = bill.copy(
            status = Bill.STATUS_PAID,
            paidDate = currentDate,
            paidBy = paidByEmail
        )

        return updateBill(updatedBill)
    }
}