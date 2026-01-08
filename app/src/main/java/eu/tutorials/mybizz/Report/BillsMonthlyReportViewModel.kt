package eu.tutorials.mybizz.Report

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillsMonthlyReportViewModel(
    private val repository: BillsMonthlyReportRepository
) : ViewModel() {

    private val _report = MutableStateFlow<BillsMonthlyReport?>(null)
    val report = _report.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun load(month: Int, year: Int) {
        viewModelScope.launch {
            _report.value = repository.getMonthlyReport(month, year)
        }
    }
}
