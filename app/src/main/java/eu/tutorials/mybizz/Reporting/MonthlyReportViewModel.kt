package eu.tutorials.mybizz.Reporting

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Logic.Task.TaskSheetsRepository
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MonthlyReportViewModel(application: Application) : AndroidViewModel(application) {
    private val billSheetsRepo = BillSheetsRepository(application)
    private val rentalSheetsRepo = RentalSheetsRepository(application)
    private val taskSheetsRepo = TaskSheetsRepository(application)
    private val reportRepository = MonthlyReportRepository()

    private val _monthlyReport = MutableLiveData<MonthlyReport?>()
    val monthlyReport: LiveData<MonthlyReport?> = _monthlyReport

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedMonth = MutableLiveData<String>()
    val selectedMonth: LiveData<String> = _selectedMonth

    init {
        // Initialize with current month
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            .format(Date())
        _selectedMonth.value = currentMonth
        loadMonthlyReport(currentMonth)
    }

    fun loadMonthlyReport(month: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _selectedMonth.value = month

                val report = reportRepository.getMonthlyReport(
                    month = month,
                    billSheetsRepo = billSheetsRepo,
                    rentalSheetsRepo = rentalSheetsRepo,
                    taskSheetsRepo = taskSheetsRepo
                )

                _monthlyReport.value = report
            } catch (e: Exception) {
                _errorMessage.value = "Error loading report: ${e.message}"
                Log.e("MonthlyReportViewModel", "Error loading report", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPreviousMonth() {
        val current = _selectedMonth.value ?: return
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        calendar.time = sdf.parse(current) ?: return
        calendar.add(Calendar.MONTH, -1)
        val previousMonth = sdf.format(calendar.time)
        loadMonthlyReport(previousMonth)
    }

    fun loadNextMonth() {
        val current = _selectedMonth.value ?: return
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        calendar.time = sdf.parse(current) ?: return
        calendar.add(Calendar.MONTH, 1)
        val nextMonth = sdf.format(calendar.time)
        loadMonthlyReport(nextMonth)
    }
}
