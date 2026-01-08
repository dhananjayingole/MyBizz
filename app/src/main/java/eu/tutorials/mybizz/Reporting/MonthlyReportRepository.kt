package eu.tutorials.mybizz.Reporting

import android.util.Log
import com.google.api.services.sheets.v4.model.*
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Logic.Task.TaskSheetsRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import java.util.*

class MonthlyReportRepository {
    companion object {
        private const val TAG = "MonthlyReportRepository"
    }

    // Get bills for a specific month
    suspend fun getBillsForMonth(
        month: String,
        billSheetsRepo: BillSheetsRepository
    ): MonthlyReportSummary {
        return try {
            val allBills = billSheetsRepo.getAllBills()
            val monthBills = allBills.filter { bill ->
                bill.dueDate.startsWith(month) || bill.paidDate.startsWith(month)
            }

            val items = monthBills.map { bill ->
                BillReportItem(
                    id = bill.id,
                    billNumber = bill.billNumber,
                    title = bill.title,
                    amount = bill.amount,
                    dueDate = bill.dueDate,
                    status = bill.status,
                    category = bill.category,
                    paidDate = bill.paidDate
                )
            }

            val paidBills = items.filter { it.status == Bill.STATUS_PAID }
            val unpaidBills = items.filter { it.status == Bill.STATUS_UNPAID }

            MonthlyReportSummary(
                reportType = ReportType.BILLS,
                month = month,
                totalAmount = items.sumOf { it.amount },
                paidAmount = paidBills.sumOf { it.amount },
                unpaidAmount = unpaidBills.sumOf { it.amount },
                totalCount = items.size,
                paidCount = paidBills.size,
                unpaidCount = unpaidBills.size,
                items = items
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bills for month $month", e)
            MonthlyReportSummary(
                reportType = ReportType.BILLS,
                month = month,
                totalAmount = 0.0,
                paidAmount = 0.0,
                unpaidAmount = 0.0,
                totalCount = 0,
                paidCount = 0,
                unpaidCount = 0,
                items = emptyList()
            )
        }
    }

    // Get rentals for a specific month
    suspend fun getRentalsForMonth(
        month: String,
        rentalSheetsRepo: RentalSheetsRepository
    ): MonthlyReportSummary {
        return try {
            val allRentals = rentalSheetsRepo.getAllRentals()
            val monthRentals = allRentals.filter { rental ->
                rental.month.startsWith(month) || rental.paymentDate.startsWith(month)
            }

            val items = monthRentals.map { rental ->
                RentalReportItem(
                    id = rental.id,
                    tenantName = rental.tenantName,
                    property = rental.property,
                    amount = rental.rentAmount,
                    month = rental.month,
                    status = rental.status,
                    paymentDate = rental.paymentDate,
                    contactNo = rental.contactNo
                )
            }

            val paidRentals = items.filter { it.status == Rental.STATUS_PAID }
            val unpaidRentals = items.filter { it.status == Rental.STATUS_UNPAID }

            MonthlyReportSummary(
                reportType = ReportType.RENTALS,
                month = month,
                totalAmount = items.sumOf { it.amount },
                paidAmount = paidRentals.sumOf { it.amount },
                unpaidAmount = unpaidRentals.sumOf { it.amount },
                totalCount = items.size,
                paidCount = paidRentals.size,
                unpaidCount = unpaidRentals.size,
                items = items
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting rentals for month $month", e)
            MonthlyReportSummary(
                reportType = ReportType.RENTALS,
                month = month,
                totalAmount = 0.0,
                paidAmount = 0.0,
                unpaidAmount = 0.0,
                totalCount = 0,
                paidCount = 0,
                unpaidCount = 0,
                items = emptyList()
            )
        }
    }

    // Get tasks for a specific month
    suspend fun getTasksForMonth(
        month: String,
        taskSheetsRepo: TaskSheetsRepository
    ): MonthlyReportSummary {
        return try {
            val allTasks = taskSheetsRepo.getAllTasks()
            val monthTasks = allTasks.filter { task ->
                task.dueDate.startsWith(month)
            }

            val items = monthTasks.map { task ->
                TaskReportItem(
                    id = task.id,
                    title = task.title,
                    assignedTo = task.assignedTo,
                    dueDate = task.dueDate,
                    status = task.status,
                    description = task.description
                )
            }

            val completedTasks = items.filter {
                it.status.equals("completed", ignoreCase = true)
            }
            val pendingTasks = items.filter {
                !it.status.equals("completed", ignoreCase = true)
            }

            MonthlyReportSummary(
                reportType = ReportType.TASKS,
                month = month,
                totalAmount = 0.0,
                paidAmount = 0.0,
                unpaidAmount = 0.0,
                totalCount = items.size,
                paidCount = completedTasks.size,
                unpaidCount = pendingTasks.size,
                items = items
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks for month $month", e)
            MonthlyReportSummary(
                reportType = ReportType.TASKS,
                month = month,
                totalAmount = 0.0,
                paidAmount = 0.0,
                unpaidAmount = 0.0,
                totalCount = 0,
                paidCount = 0,
                unpaidCount = 0,
                items = emptyList()
            )
        }
    }

    // Get complete monthly report
    suspend fun getMonthlyReport(
        month: String,
        billSheetsRepo: BillSheetsRepository,
        rentalSheetsRepo: RentalSheetsRepository,
        taskSheetsRepo: TaskSheetsRepository
    ): MonthlyReport {
        return MonthlyReport(
            month = month,
            billsSummary = getBillsForMonth(month, billSheetsRepo),
            rentalsSummary = getRentalsForMonth(month, rentalSheetsRepo),
            tasksSummary = getTasksForMonth(month, taskSheetsRepo)
        )
    }
}