package eu.tutorials.mybizz.Logic.plot

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import eu.tutorials.mybizz.Model.Plot
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class PlotSheetsRepository(private val context: Context) {
    companion object {
        private const val TAG = "PlotSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val SHEET_NAME = "Sheet5"
        private const val RANGE = "A:M"
        private const val API_TIMEOUT_SECONDS = 20L

        // Column indices
        private const val COL_ID = 0
        private const val COL_PLOT_NAME = 1
        private const val COL_PLOT_ID = 2
        private const val COL_LOCATION = 3
        private const val COL_VISITOR_NAME = 4
        private const val COL_VISITOR_NUMBER = 5
        private const val COL_VISITOR_ADDRESS = 6
        private const val COL_ASKING_AMOUNT = 7
        private const val COL_ATTENDED_BY = 8
        private const val COL_INITIAL_PRICE = 9
        private const val COL_PLOT_SIZE = 10
        private const val COL_VISIT_DATE = 11
        private const val COL_NOTES = 12
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

    suspend fun getAllPlots(): List<Plot> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: throw Exception("Sheets service unavailable")

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$SHEET_NAME!$RANGE")
                    .execute()

                val values = response.getValues()
                Log.d(TAG, "Fetched ${values?.size ?: 0} rows from sheets")

                // FIXED: Skip only 1 row (headers) instead of 2
                if (values.isNullOrEmpty() || values.size <= 1) {
                    Log.d(TAG, "No data found in sheets")
                    return@withTimeout emptyList()
                }

                // FIXED: Drop only 1 row (headers)
                values.drop(1).mapNotNull { row ->
                    Log.d(TAG, "Processing row: $row")
                    if (row.size > COL_ID && row[COL_ID].toString().isNotBlank()) {
                        Plot(
                            id = row[COL_ID].toString(),
                            plotName = row.getOrNull(COL_PLOT_NAME)?.toString() ?: "",
                            plotId = row.getOrNull(COL_PLOT_ID)?.toString() ?: "",
                            location = row.getOrNull(COL_LOCATION)?.toString() ?: "",
                            visitorName = row.getOrNull(COL_VISITOR_NAME)?.toString() ?: "",
                            visitorNumber = row.getOrNull(COL_VISITOR_NUMBER)?.toString() ?: "",
                            visitorAddress = row.getOrNull(COL_VISITOR_ADDRESS)?.toString() ?: "",
                            askingAmount = row.getOrNull(COL_ASKING_AMOUNT)?.toString() ?: "",
                            attendedBy = row.getOrNull(COL_ATTENDED_BY)?.toString() ?: "",
                            initialPrice = row.getOrNull(COL_INITIAL_PRICE)?.toString() ?: "",
                            plotSize = row.getOrNull(COL_PLOT_SIZE)?.toString() ?: "",
                            visitDate = row.getOrNull(COL_VISIT_DATE)?.toString() ?: "",
                            notes = row.getOrNull(COL_NOTES)?.toString() ?: ""
                        )
                    } else {
                        Log.d(TAG, "Skipping empty row")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching plots: ${e.message}", e)
            emptyList()
        }
    }
    suspend fun addPlot(plot: Plot): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                val values = listOf(
                    plot.id,
                    plot.plotName,
                    plot.plotId,
                    plot.location,
                    plot.visitorName,
                    plot.visitorNumber,
                    plot.visitorAddress,
                    plot.askingAmount,
                    plot.attendedBy,
                    plot.initialPrice,
                    plot.plotSize,
                    plot.visitDate,
                    plot.notes
                )

                Log.d(TAG, "Adding plot with values: $values")

                val body = ValueRange().setValues(listOf(values))
                val result = service.spreadsheets().values()
                    .append(SPREADSHEET_ID, "$SHEET_NAME!$RANGE", body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                Log.d(TAG, "Plot added successfully. Result: ${result.updates?.updatedRows} rows updated")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding plot: ${e.message}", e)
            false
        }
    }
    suspend fun updatePlot(plot: Plot): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // Find the row by ID
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$SHEET_NAME!A:A")
                    .execute()

                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == plot.id) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                val updateValues = listOf(
                    plot.id,
                    plot.plotName,
                    plot.plotId,
                    plot.location,
                    plot.visitorName,
                    plot.visitorNumber,
                    plot.visitorAddress,
                    plot.askingAmount,
                    plot.attendedBy,
                    plot.initialPrice,
                    plot.plotSize,
                    plot.visitDate,
                    plot.notes
                )

                val body = ValueRange().setValues(listOf(updateValues))
                val range = "$SHEET_NAME!A$rowIndex:M$rowIndex"

                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, range, body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating plot: ${e.message}", e)
            false
        }
    }
    suspend fun deletePlot(plotId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                // Find the row by ID
                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$SHEET_NAME!A:A")
                    .execute()

                val values = response.getValues() ?: return@withTimeout false

                var rowIndex = -1
                for (i in values.indices) {
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == plotId) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                val clearRequest = com.google.api.services.sheets.v4.model.ClearValuesRequest()
                val range = "$SHEET_NAME!A$rowIndex:M$rowIndex"

                service.spreadsheets().values()
                    .clear(SPREADSHEET_ID, range, clearRequest)
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting plot: ${e.message}", e)
            false
        }
    }
}