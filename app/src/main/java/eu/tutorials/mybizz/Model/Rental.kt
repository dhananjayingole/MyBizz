package eu.tutorials.mybizz.Model

data class Rental(
    val id: String = "",
    val tenantName: String = "",
    val property: String = "",
    val rentAmount: Double = 0.0,
    val month: String = "", // Format: "2025-10"
    val status: String = STATUS_UNPAID,
    val paymentDate: String = "",
    val contactNo: String = ""
) {
    companion object {
        const val STATUS_PAID = "paid"
        const val STATUS_UNPAID = "unpaid"
    }
}
