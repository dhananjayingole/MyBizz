// Bill.kt - Updated Model
package eu.tutorials.mybizz.Model

data class Bill(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "", // Format: "2024-01-15"
    val status: String = STATUS_UNPAID, // "paid", "unpaid"
    val category: String = "", // "electricity", "water", "internet", "worker", "maintenance"
    val paidDate: String = "",
    val paidBy: String = "", // New field: username who marked as paid
    val createdDate: String = "",
    val createdBy: String = "" // User ID
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
    }
}