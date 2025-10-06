package eu.tutorials.mybizz.Logic.Task

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import eu.tutorials.mybizz.Model.Task
import eu.tutorials.mybizz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class TaskSheetsRepository(private val context: Context) {

    companion object {
        private const val TAG = "TaskSheetsRepository"
        private const val SPREADSHEET_ID = "1fq5rUYjzZB8L2TsNTAKfFHTaBvtjUwziq8F9Jinxk2U"
        private const val SHEET_NAME = "Sheet4"
        private const val RANGE = "A:G"
        private const val API_TIMEOUT_SECONDS = 20L

        // Column indices
        private const val COL_ID = 0
        private const val COL_TITLE = 1
        private const val COL_DESCRIPTION = 2
        private const val COL_ASSIGNED_TO = 3
        private const val COL_DUE_DATE = 4
        private const val COL_STATUS = 5
        private const val COL_NOTES = 6
    }

    private var sheetsService: Sheets? = null

    private fun getSheetsService(): Sheets? {
        return sheetsService ?: try {
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            // Use raw resource like your other repositories
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

    suspend fun getAllTasks(): List<Task> = withContext(Dispatchers.IO) {
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
                        Task(
                            id = row[COL_ID].toString(),
                            title = row.getOrNull(COL_TITLE)?.toString() ?: "",
                            description = row.getOrNull(COL_DESCRIPTION)?.toString() ?: "",
                            assignedTo = row.getOrNull(COL_ASSIGNED_TO)?.toString() ?: "",
                            dueDate = row.getOrNull(COL_DUE_DATE)?.toString() ?: "",
                            status = row.getOrNull(COL_STATUS)?.toString() ?: "Pending",
                            notes = row.getOrNull(COL_NOTES)?.toString() ?: ""
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addTask(task: Task): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(API_TIMEOUT_SECONDS)) {
                val service = getSheetsService() ?: return@withTimeout false

                val values = listOf(
                    task.id,
                    task.title,
                    task.description,
                    task.assignedTo,
                    task.dueDate,
                    task.status,
                    task.notes
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
            Log.e(TAG, "Error adding task: ${e.message}", e)
            false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
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
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == taskId) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                // Clear the row instead of deleting (safer for sheet structure)
                val clearRequest = com.google.api.services.sheets.v4.model.ClearValuesRequest()
                val range = "$SHEET_NAME!A$rowIndex:G$rowIndex"

                service.spreadsheets().values()
                    .clear(SPREADSHEET_ID, range, clearRequest)
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task: ${e.message}", e)
            false
        }
    }

    suspend fun updateTask(task: Task): Boolean = withContext(Dispatchers.IO) {
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
                    if (i > 0 && values[i].isNotEmpty() && values[i][0].toString() == task.id) {
                        rowIndex = i + 1
                        break
                    }
                }
                if (rowIndex == -1) return@withTimeout false

                val updateValues = listOf(
                    task.id,
                    task.title,
                    task.description,
                    task.assignedTo,
                    task.dueDate,
                    task.status,
                    task.notes
                )

                val body = ValueRange().setValues(listOf(updateValues))
                val range = "$SHEET_NAME!A$rowIndex:G$rowIndex"

                service.spreadsheets().values()
                    .update(SPREADSHEET_ID, range, body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task: ${e.message}", e)
            false
        }
    }
}