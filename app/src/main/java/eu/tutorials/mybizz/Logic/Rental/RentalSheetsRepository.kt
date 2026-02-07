package eu.tutorials.mybizz.Logic.Rental

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
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import kotlin.collections.isNotEmpty

class RentalSheetsRepository(private val context: Context) {
    companion object {
        private const val TAG = "RentalSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val RENTALS_SHEET_NAME = "Sheet2" // Use Sheet2 or Rentals tab
        private const val RANGE = "A:H"
        private const val API_TIMEOUT_SECONDS = 20L

        // Column indices
        private const val COL_ID = 0
        private const val COL_TENANT_NAME = 1
        private const val COL_PROPERTY = 2
        private const val COL_RENT_AMOUNT = 3
        private const val COL_MONTH = 4
        private const val COL_STATUS = 5
        private const val COL_PAYMENT_DATE = 6
        private const val COL_CONTACT_NO = 7
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

    suspend fun getAllRentals(): List<Rental> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: throw Exception("Sheets service unavailable")

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$RENTALS_SHEET_NAME!$RANGE")
                    .execute()

                val values = response.getValues()
                if (values.isNullOrEmpty() || values.size <= 1) return@withTimeout emptyList()

                val rentals = mutableListOf<Rental>()
                for (i in 1 until values.size) {
                    val row = values[i]
                    if (row.size > COL_ID && row[COL_ID].toString().isNotBlank()) {
                        val rental = Rental(
                            id = row[COL_ID].toString(),
                            tenantName = row.getOrNull(COL_TENANT_NAME)?.toString() ?: "",
                            property = row.getOrNull(COL_PROPERTY)?.toString() ?: "",
                            rentAmount = row.getOrNull(COL_RENT_AMOUNT)?.toString()?.toDoubleOrNull() ?: 0.0,
                            month = row.getOrNull(COL_MONTH)?.toString() ?: "",
                            status = row.getOrNull(COL_STATUS)?.toString() ?: Rental.STATUS_UNPAID,
                            paymentDate = row.getOrNull(COL_PAYMENT_DATE)?.toString() ?: "",
                            contactNo = row.getOrNull(COL_CONTACT_NO)?.toString() ?: ""
                        )
                        rentals.add(rental)
                    }
                }
                rentals
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching rentals: ${e.message}", e)
            emptyList() // Return empty list instead of throwing
        }
    }
    suspend fun addRental(rental: Rental): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false
                val values = listOf(
                    rental.id,
                    rental.tenantName,
                    rental.property,
                    rental.rentAmount.toString(),
                    rental.month,
                    rental.status,
                    rental.paymentDate,
                    rental.contactNo
                )
                val request = ValueRange().setValues(listOf(values))
                service.spreadsheets().values()
                    .append(SPREADSHEET_ID, "$RENTALS_SHEET_NAME!$RANGE", request)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding rental: ${e.message}", e)
            false
        }
    }

    suspend fun updateRental(rental: Rental): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$RENTALS_SHEET_NAME!A:A")
                    .execute()
                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == rental.id) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                val valuesToUpdate = listOf(
                    rental.id,
                    rental.tenantName,
                    rental.property,
                    rental.rentAmount.toString(),
                    rental.month,
                    rental.status,
                    rental.paymentDate,
                    rental.contactNo
                )
                val request = ValueRange().setValues(listOf(valuesToUpdate))
                val range = "$RENTALS_SHEET_NAME!A$rowIndex:H$rowIndex"
                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, range, request)
                    .setValueInputOption("RAW")
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating rental: ${e.message}", e)
            false
        }
    }

    suspend fun deleteRental(rentalId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$RENTALS_SHEET_NAME!A:A")
                    .execute()
                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == rentalId) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                val clearRequest = com.google.api.services.sheets.v4.model.ClearValuesRequest()
                val range = "$RENTALS_SHEET_NAME!A$rowIndex:H$rowIndex"
                service.spreadsheets().values()
                    .clear(SPREADSHEET_ID, range, clearRequest)
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting rental: ${e.message}", e)
            false
        }
    }

    suspend fun markRentalAsPaid(rentalId: String): Boolean {
        val rental = getAllRentals().find { it.id == rentalId } ?: return false
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val updatedRental = rental.copy(
            status = Rental.STATUS_PAID,
            paymentDate = currentDate
        )
        return updateRental(updatedRental)
    }
}
