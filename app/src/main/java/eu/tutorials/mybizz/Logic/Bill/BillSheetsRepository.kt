package eu.tutorials.mybizz.Repository

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.BillHistoryEntry
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BillSheetsRepository(private val context: Context) {
    companion object {
        private const val TAG = "BillSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val BILLS_SHEET_NAME = "Sheet1"
        private const val HISTORY_SHEET_NAME = "BillHistory"
        private const val BILLS_RANGE = "A:O" // We'll use this range but map columns properly
        private const val HISTORY_RANGE = "A:F"
        private const val API_TIMEOUT_SECONDS = 20L

        // ACTUAL COLUMN MAPPING BASED ON YOUR SHEET STRUCTURE
        // Your sheet has: | A | B | C | D | E | F | G | H | I |
        // Which corresponds to: | ID? | Title | Description | Amount | Due Date | Status | Category | Paid Date | Created Date |

        // Let's map the new structure to your existing columns
        private const val COL_ID = 0                    // A - ID
        private const val COL_TITLE = 1                // B - Title (will be used for bill number temporarily)
        private const val COL_DESCRIPTION = 2          // C - Description
        private const val COL_AMOUNT = 3               // D - Amount
        private const val COL_DUE_DATE = 4             // E - Due Date
        private const val COL_STATUS = 5               // F - Status
        private const val COL_CATEGORY = 6             // G - Category
        private const val COL_PAID_DATE = 7            // H - Paid Date
        private const val COL_CREATED_DATE = 8         // I - Created Date

        // New columns we'll add for versioning (these will be empty initially)
        private const val COL_BILL_NUMBER = 9          // J - Bill Number (new)
        private const val COL_VERSION = 10             // K - Version (new)
        private const val COL_CREATED_BY = 11          // L - Created By (new)
        private const val COL_MODIFIED_DATE = 12       // M - Modified Date (new)
        private const val COL_MODIFIED_BY = 13         // N - Modified By (new)
        private const val COL_PAID_BY = 14             // O - Paid By (new)
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

    // Generate bill number based on year and sequence
    private suspend fun generateBillNumber(): String = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService() ?: return@withContext ""

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, "$BILLS_SHEET_NAME!A:O")
                .execute()

            val values = response.getValues()
            if (values.isNullOrEmpty() || values.size <= 1) {
                return@withContext "${currentYear}001"
            }

            // Find the highest number for current year
            var maxNumber = 0
            for (i in 1 until values.size) {
                try {
                    val row = values[i]
                    // Check if we have a bill number in column J (index 9)
                    if (row.size > COL_BILL_NUMBER) {
                        val billNumber = row[COL_BILL_NUMBER]?.toString() ?: ""
                        if (billNumber.isNotEmpty() && billNumber.startsWith(currentYear.toString())) {
                            val number = billNumber.substring(4).toIntOrNull() ?: 0
                            if (number > maxNumber) {
                                maxNumber = number
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping row $i in bill number generation: ${e.message}")
                }
            }

            val nextNumber = maxNumber + 1
            return@withContext "$currentYear${nextNumber.toString().padStart(3, '0')}"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating bill number: ${e.message}", e)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            "${currentYear}001"
        }
    }

    suspend fun getAllBills(): List<Bill> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                Log.d(TAG, "Fetching all bills from Google Sheets")
                val service = getSheetsService() ?: throw Exception("Sheets service unavailable")

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$BILLS_SHEET_NAME!$BILLS_RANGE")
                    .execute()

                val values = response.getValues()
                if (values.isNullOrEmpty() || values.size <= 1) {
                    Log.d(TAG, "No bills data found")
                    return@withTimeout emptyList()
                }

                val bills = mutableListOf<Bill>()
                for (i in 1 until values.size) {
                    try {
                        val row = values[i]
                        // Check if we have at least the basic required data (title)
                        if (row.size > COL_TITLE && row[COL_TITLE].toString().isNotBlank()) {

                            // Generate a unique ID if not present
                            val id = if (row.size > COL_ID && row[COL_ID].toString().isNotBlank()) {
                                row[COL_ID].toString()
                            } else {
                                UUID.randomUUID().toString()
                            }

                            // Generate bill number if not present
                            val billNumber = if (row.size > COL_BILL_NUMBER && row[COL_BILL_NUMBER].toString().isNotBlank()) {
                                row[COL_BILL_NUMBER].toString()
                            } else {
                                // Use title as temporary bill number or generate one
                                "TEMP-${i}"
                            }

                            val bill = Bill(
                                id = id,
                                billNumber = billNumber,
                                version = if (row.size > COL_VERSION) row[COL_VERSION].toString().toIntOrNull() ?: 1 else 1,
                                title = row[COL_TITLE].toString(),
                                description = if (row.size > COL_DESCRIPTION) row[COL_DESCRIPTION].toString() else "",
                                amount = if (row.size > COL_AMOUNT) row[COL_AMOUNT].toString().toDoubleOrNull() ?: 0.0 else 0.0,
                                dueDate = if (row.size > COL_DUE_DATE) row[COL_DUE_DATE].toString() else "",
                                status = if (row.size > COL_STATUS) {
                                    val status = row[COL_STATUS].toString().toLowerCase(Locale.ROOT)
                                    if (status == "paid") Bill.STATUS_PAID else Bill.STATUS_UNPAID
                                } else {
                                    Bill.STATUS_UNPAID
                                },
                                category = if (row.size > COL_CATEGORY) row[COL_CATEGORY].toString() else "",
                                paidDate = if (row.size > COL_PAID_DATE) row[COL_PAID_DATE].toString() else "",
                                paidBy = if (row.size > COL_PAID_BY) row[COL_PAID_BY].toString() else "",
                                createdDate = if (row.size > COL_CREATED_DATE) row[COL_CREATED_DATE].toString() else "",
                                createdBy = if (row.size > COL_CREATED_BY) row[COL_CREATED_BY].toString() else "",
                                modifiedDate = if (row.size > COL_MODIFIED_DATE) row[COL_MODIFIED_DATE].toString() else "",
                                modifiedBy = if (row.size > COL_MODIFIED_BY) row[COL_MODIFIED_BY].toString() else ""
                            )
                            bills.add(bill)
                            Log.d(TAG, "Loaded bill: ${bill.title} with ID: ${bill.id}")
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

    // Get bill history - FIXED VERSION
    suspend fun getBillHistory(billNumber: String): List<BillHistoryEntry> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                Log.d(TAG, "Fetching history for bill: $billNumber")
                val service = getSheetsService() ?: return@withTimeout emptyList()

                // Ensure history sheet exists
                ensureHistorySheetExists(service)

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$HISTORY_SHEET_NAME!$HISTORY_RANGE")
                    .execute()

                val values = response.getValues()
                if (values.isNullOrEmpty() || values.size <= 1) {
                    return@withTimeout emptyList()
                }

                val history = mutableListOf<BillHistoryEntry>()

                // Get all versions for this bill and sort by version
                val billEntries = mutableListOf<BillHistoryEntry>()
                for (i in 1 until values.size) {
                    try {
                        val row = values[i]
                        if (row.size > 0 && row[0].toString() == billNumber) {
                            val entry = BillHistoryEntry(
                                billNumber = row[0].toString(),
                                version = row.getOrNull(1)?.toString()?.toIntOrNull() ?: 1,
                                modifiedBy = row.getOrNull(2)?.toString() ?: "",
                                modifiedDate = row.getOrNull(3)?.toString() ?: "",
                                amount = row.getOrNull(4)?.toString()?.toDoubleOrNull() ?: 0.0,
                                cumulativeAmount = 0.0, // We'll calculate this later
                                changeType = row.getOrNull(5)?.toString() ?: "MODIFIED"
                            )
                            billEntries.add(entry)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping invalid history row $i: ${e.message}")
                    }
                }

                // Sort by version and calculate cumulative amounts
                val sortedEntries = billEntries.sortedBy { it.version }
                var runningTotal = 0.0

                for (entry in sortedEntries) {
                    runningTotal = entry.amount // Each entry shows the current total amount at that version
                    history.add(entry.copy(cumulativeAmount = runningTotal))
                }

                Log.d(TAG, "Found ${history.size} history entries for bill $billNumber")
                history
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bill history: ${e.message}", e)
            emptyList()
        }
    }

    // Ensure history sheet exists
    private suspend fun ensureHistorySheetExists(service: Sheets) {
        try {
            val spreadsheet = service.spreadsheets().get(SPREADSHEET_ID).execute()
            val sheetExists = spreadsheet.sheets.any { it.properties.title == HISTORY_SHEET_NAME }

            if (!sheetExists) {
                // Create history sheet
                val addSheetRequest = Request().setAddSheet(
                    AddSheetRequest().setProperties(
                        SheetProperties().setTitle(HISTORY_SHEET_NAME)
                    )
                )

                val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(addSheetRequest))

                service.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute()

                // Add headers
                val headers = listOf(
                    listOf("Bill Number", "Version", "Modified By", "Date", "Amount", "Change Type")
                )
                val headerRequest = ValueRange().setValues(headers)
                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, "$HISTORY_SHEET_NAME!A1:F1", headerRequest)
                    .setValueInputOption("RAW")
                    .execute()

                Log.d(TAG, "Created history sheet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring history sheet exists: ${e.message}", e)
        }
    }

    // Add entry to history
    private suspend fun addHistoryEntry(
        billNumber: String,
        version: Int,
        modifiedBy: String,
        amount: Double,
        changeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService() ?: return@withContext false
            ensureHistorySheetExists(service)

            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            val values = listOf(
                billNumber,
                version.toString(),
                modifiedBy,
                currentDate,
                amount.toString(),
                changeType
            )

            val request = ValueRange().setValues(listOf(values))
            service.spreadsheets().values()
                .append(SPREADSHEET_ID, "$HISTORY_SHEET_NAME!A:F", request)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute()

            Log.d(TAG, "Added history entry for bill $billNumber version $version")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding history entry: ${e.message}", e)
            false
        }
    }

    suspend fun addBill(bill: Bill): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // Generate bill number
                val billNumber = generateBillNumber()
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val billWithNumber = bill.copy(
                    id = UUID.randomUUID().toString(),
                    billNumber = billNumber,
                    version = 1,
                    createdDate = currentDate,
                    modifiedDate = currentDate,
                    modifiedBy = bill.createdBy
                )

                // Prepare all 15 columns (A-O)
                val values = listOf(
                    billWithNumber.id,                    // A - ID
                    billWithNumber.title,                 // B - Title
                    billWithNumber.description,           // C - Description
                    billWithNumber.amount.toString(),     // D - Amount
                    billWithNumber.dueDate,               // E - Due Date
                    billWithNumber.status,                // F - Status
                    billWithNumber.category,              // G - Category
                    billWithNumber.paidDate,              // H - Paid Date
                    billWithNumber.createdDate,           // I - Created Date
                    billWithNumber.billNumber,            // J - Bill Number (new)
                    billWithNumber.version.toString(),    // K - Version (new)
                    billWithNumber.createdBy,             // L - Created By (new)
                    billWithNumber.modifiedDate,          // M - Modified Date (new)
                    billWithNumber.modifiedBy,            // N - Modified By (new)
                    billWithNumber.paidBy                 // O - Paid By (new)
                )

                val request = ValueRange().setValues(listOf(values))
                service.spreadsheets().values()
                    .append(SPREADSHEET_ID, "$BILLS_SHEET_NAME!$BILLS_RANGE", request)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                // Add initial history entry
                addHistoryEntry(
                    billNumber,
                    1,
                    billWithNumber.createdBy,
                    billWithNumber.amount,
                    "CREATED"
                )

                Log.d(TAG, "Bill added successfully with number: $billNumber")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bill: ${e.message}", e)
            false
        }
    }

    suspend fun updateBill(bill: Bill, updatedBy: String): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // Get current bill to check for changes
                val currentBill = getBillById(bill.id) ?: return@withTimeout false

                // Increment version
                val newVersion = currentBill.version + 1
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val updatedBill = bill.copy(
                    version = newVersion,
                    modifiedDate = currentDate,
                    modifiedBy = updatedBy
                )

                // Find the row by ID
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$BILLS_SHEET_NAME!A:A")
                    .execute()

                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == bill.id) {
                        rowIndex = i + 1
                        break
                    }
                }

                if (rowIndex == -1) {
                    Log.e(TAG, "Bill not found for update: ${bill.id}")
                    return@withTimeout false
                }

                // Update all 15 columns
                val valuesToUpdate = listOf(
                    updatedBill.id,                    // A - ID
                    updatedBill.title,                 // B - Title
                    updatedBill.description,           // C - Description
                    updatedBill.amount.toString(),     // D - Amount
                    updatedBill.dueDate,               // E - Due Date
                    updatedBill.status,                // F - Status
                    updatedBill.category,              // G - Category
                    updatedBill.paidDate,              // H - Paid Date
                    updatedBill.createdDate,           // I - Created Date
                    updatedBill.billNumber,            // J - Bill Number
                    updatedBill.version.toString(),    // K - Version
                    updatedBill.createdBy,             // L - Created By
                    updatedBill.modifiedDate,          // M - Modified Date
                    updatedBill.modifiedBy,            // N - Modified By
                    updatedBill.paidBy                 // O - Paid By
                )

                val request = ValueRange().setValues(listOf(valuesToUpdate))
                val range = "$BILLS_SHEET_NAME!A$rowIndex:O$rowIndex"

                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, range, request)
                    .setValueInputOption("RAW")
                    .execute()

                // Add history entry for modification
                addHistoryEntry(
                    updatedBill.billNumber,
                    newVersion,
                    updatedBy,
                    updatedBill.amount,
                    "MODIFIED"
                )

                Log.d(TAG, "Bill updated successfully to version $newVersion")
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

                val clearRequest = com.google.api.services.sheets.v4.model.ClearValuesRequest()
                val range = "$BILLS_SHEET_NAME!A$rowIndex:O$rowIndex"

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

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())

        val updatedBill = bill.copy(
            status = Bill.STATUS_PAID,
            paidDate = currentDate,
            paidBy = paidByEmail
        )

        return updateBill(updatedBill, paidByEmail)
    }
}