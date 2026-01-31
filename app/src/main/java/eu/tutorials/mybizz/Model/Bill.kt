package eu.tutorials.mybizz.Model

import java.util.*

data class Bill(
    val id: String = "",
    val billNumber: String = "", // NEW: Formatted bill number like "2025001"
    val version: Int = 1, // NEW: Version number
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "",
    val status: String = STATUS_UNPAID,
    val category: String = "",
    val paidDate: String = "",
    val paidBy: String = "",
    val createdDate: String = "",
    val createdBy: String = "",
    val modifiedDate: String = "", // NEW: Last modification date
    val modifiedBy: String = "" // NEW: Last modifier email
) {
    companion object {
        const val STATUS_PAID = "paid"
        const val STATUS_UNPAID = "unpaid"

        val CATEGORIES = listOf(
            "Worker Payment",
            "Maintenance",
            "Tax",
            "Insurance",
            "Other"
        )

        // NEW: Generate bill number
        fun generateBillNumber(): String {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return "${currentYear}001" // This will be updated by repository
        }
    }
}

// NEW: Bill History Entry for tracking changes
data class BillHistoryEntry(
    val billNumber: String,
    val version: Int,
    val modifiedBy: String,
    val modifiedDate: String,
    val amount: Double,
    val cumulativeAmount: Double, // Total amount including all previous versions
    val changeType: String // "CREATED" or "MODIFIED"
)
