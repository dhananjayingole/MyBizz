package eu.tutorials.mybizz.Reporting

// Sealed class for different report types
sealed class MonthlyReportItem {
    abstract val id: String
    abstract val amount: Double
    abstract val date: String
    abstract val status: String
    abstract val category: String
}

data class BillReportItem(
    override val id: String,
    val billNumber: String,
    val title: String,
    override val amount: Double,
    val dueDate: String,
    override val status: String,
    override val category: String,
    val paidDate: String,
    override val date: String = dueDate
) : MonthlyReportItem()

data class RentalReportItem(
    override val id: String,
    val tenantName: String,
    val property: String,
    override val amount: Double,
    val month: String,
    override val status: String,
    val paymentDate: String,
    val contactNo: String,
    override val date: String = month,
    override val category: String = "Rental Income"
) : MonthlyReportItem()

data class TaskReportItem(
    override val id: String,
    val title: String,
    val assignedTo: String,
    val dueDate: String,
    override val status: String,
    val description: String,
    override val date: String = dueDate,
    override val amount: Double = 0.0, // Tasks don't have amounts
    override val category: String = "Task"
) : MonthlyReportItem()

// Report type enum
enum class ReportType {
    BILLS,
    RENTALS,
    TASKS
}

// Monthly summary for each type
data class MonthlyReportSummary(
    val reportType: ReportType,
    val month: String, // Format: "2025-01"
    val totalAmount: Double,
    val paidAmount: Double,
    val unpaidAmount: Double,
    val totalCount: Int,
    val paidCount: Int,
    val unpaidCount: Int,
    val items: List<MonthlyReportItem>
)

// Overall report container
data class MonthlyReport(
    val month: String,
    val billsSummary: MonthlyReportSummary,
    val rentalsSummary: MonthlyReportSummary,
    val tasksSummary: MonthlyReportSummary
) {
    fun getTotalIncome(): Double = rentalsSummary.paidAmount
    fun getTotalExpenses(): Double = billsSummary.paidAmount
    fun getNetAmount(): Double = getTotalIncome() - getTotalExpenses()
}