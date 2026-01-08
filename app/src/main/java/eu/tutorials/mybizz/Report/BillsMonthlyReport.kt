package eu.tutorials.mybizz.Report

data class BillsMonthlyReport(
    val month: Int,
    val year: Int,
    val totalPaidAmount: Double,
    val totalUnpaidAmount: Double,
    val totalBills: Int,
    val paidCount: Int,
    val unpaidCount: Int,
    val categoryTotals: Map<String, Double>
)

