package eu.tutorials.mybizz.Logic.Construction

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import eu.tutorials.mybizz.Model.Construction
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.*
import java.util.concurrent.TimeUnit

class ConstructionSheetsRepository(private val context: Context) {

    companion object {
        private const val TAG = "ConstructionSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val SHEET_NAME = "Sheet3"
        private const val RANGE = "A:H"
        private const val API_TIMEOUT_SECONDS = 20L

        // Column indices
        private const val COL_ID = 0
        private const val COL_PROJECT_NAME = 1
        private const val COL_LOCATION = 2
        private const val COL_START_DATE = 3
        private const val COL_END_DATE = 4
        private const val COL_COST = 5
        private const val COL_STATUS = 6
        private const val COL_NOTES = 7
    }

    private var sheetsService: Sheets? = null

    private fun getSheetsService(): Sheets? {
        return sheetsService ?: try {
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            // Use the same credential loading as your working repositories
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

    suspend fun getAllConstructions(): List<Construction> = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: throw Exception("Sheets service unavailable")

                val response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, "$SHEET_NAME!$RANGE")
                    .execute()

                val values = response.getValues()
                if (values.isNullOrEmpty() || values.size <= 1) return@withTimeout emptyList()

                values.drop(1).mapNotNull { row ->
                    if (row.size > COL_ID && row[COL_ID].toString().isNotBlank()) {
                        Construction(
                            id = row[COL_ID].toString(),
                            projectName = row.getOrNull(COL_PROJECT_NAME)?.toString() ?: "",
                            location = row.getOrNull(COL_LOCATION)?.toString() ?: "",
                            startDate = row.getOrNull(COL_START_DATE)?.toString() ?: "",
                            endDate = row.getOrNull(COL_END_DATE)?.toString() ?: "",
                            cost = row.getOrNull(COL_COST)?.toString() ?: "",
                            status = row.getOrNull(COL_STATUS)?.toString() ?: "",
                            notes = row.getOrNull(COL_NOTES)?.toString() ?: ""
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching constructions: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addConstruction(construction: Construction): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                val values = listOf(
                    construction.id,
                    construction.projectName,
                    construction.location,
                    construction.startDate,
                    construction.endDate,
                    construction.cost,
                    construction.status,
                    construction.notes ?: ""
                )

                val body = ValueRange().setValues(listOf(values))
                service.spreadsheets().values()
                    .append(SPREADSHEET_ID, "$SHEET_NAME!$RANGE", body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding construction: ${e.message}", e)
            false
        }
    }

    suspend fun updateConstruction(construction: Construction): Boolean = withContext(Dispatchers.IO) {
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
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == construction.id) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                val updateValues = listOf(
                    construction.id,
                    construction.projectName,
                    construction.location,
                    construction.startDate,
                    construction.endDate,
                    construction.cost,
                    construction.status,
                    construction.notes ?: ""
                )

                val body = ValueRange().setValues(listOf(updateValues))
                val range = "$SHEET_NAME!A$rowIndex:H$rowIndex"

                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, range, body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating construction: ${e.message}", e)
            false
        }
    }

    suspend fun deleteConstruction(constructionId: String): Boolean = withContext(Dispatchers.IO) {
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
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == constructionId) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                // Clear the row instead of deleting (safer for sheet structure)
                val clearRequest = com.google.api.services.sheets.v4.model.ClearValuesRequest()
                val range = "$SHEET_NAME!A$rowIndex:H$rowIndex"

                service.spreadsheets().values()
                    .clear(SPREADSHEET_ID, range, clearRequest)
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting construction: ${e.message}", e)
            false
        }
    }
}