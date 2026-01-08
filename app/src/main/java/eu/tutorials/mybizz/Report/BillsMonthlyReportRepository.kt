package eu.tutorials.mybizz.Report


import android.os.Build
import androidx.annotation.RequiresApi
import eu.tutorials.mybizz.DateUtils
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Repository.BillSheetsRepository

class BillsMonthlyReportRepository(
    private val billRepository: BillRepository,
    private val sheetsRepo: BillSheetsRepository
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getMonthlyReport(month: Int, year: Int): BillsMonthlyReport {

        val allBills = billRepository.getAllBills(sheetsRepo)

        val monthlyBills = allBills.filter { bill ->
            val date = when {
                bill.status == Bill.STATUS_PAID -> DateUtils.parseDate(bill.paidDate)
                else -> DateUtils.parseDate(bill.createdDate)
            }

            date?.monthValue == month && date.year == year
        }

        val paidBills = monthlyBills.filter { it.status == Bill.STATUS_PAID }
        val unpaidBills = monthlyBills.filter { it.status == Bill.STATUS_UNPAID }

        val categoryTotals = paidBills
            .groupBy { it.category }
            .mapValues { entry ->
                entry.value.sumOf { it.amount }
            }

        return BillsMonthlyReport(
            month = month,
            year = year,
            totalPaidAmount = paidBills.sumOf { it.amount },
            totalUnpaidAmount = unpaidBills.sumOf { it.amount },
            totalBills = monthlyBills.size,
            paidCount = paidBills.size,
            unpaidCount = unpaidBills.size,
            categoryTotals = categoryTotals
        )
    }
}
