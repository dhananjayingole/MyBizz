package eu.tutorials.mybizz

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Keep DateUtils with yyyy-MM-dd in a string.
object DateUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseDate(date: String): LocalDate? {
        return try {
            LocalDate.parse(date.substring(0, 10), dateOnlyFormatter)
        } catch (e: Exception) {
            null
        }
    }
}
