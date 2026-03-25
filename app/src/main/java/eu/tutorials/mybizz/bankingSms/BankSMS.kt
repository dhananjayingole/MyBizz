package eu.tutorials.mybizz.bankingSms

data class BankSMS(
    val id: Long,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val amount: Double? = null,        // extracted amount
    val transactionType: String? = null // CREDIT / DEBIT
)