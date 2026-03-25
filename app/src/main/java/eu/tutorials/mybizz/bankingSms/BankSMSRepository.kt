package eu.tutorials.mybizz.bankingSms

import android.content.Context
import android.provider.Telephony
import eu.tutorials.mybizz.bankingSms.BankSMS

class BankSMSRepository(private val context: Context) {

    // Known bank SMS sender IDs
    private val bankSenders = listOf(
        "HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB",
        "PNBSMS", "BOIIND", "CANBNK", "UNIONB", "INDBNK",
        "PAYTMB", "IDFCBK", "YESBNK", "RBLBNK", "FEDERA",
        "CENTBK", "SYNBNK", "OBCBNK", "ALAHBD", "BRDBNK",
        "UTIBOP", "UTIB",   // Axis Bank
        "SBMSMS", "SBIPSG",
        // Add more as needed
        "AD-", "VM-", "BZ-"  // common prefixes
    )

    // Keywords that indicate bank messages
    private val bankKeywords = listOf(
        "debited", "credited", "transaction", "balance",
        "withdrawn", "deposited", "transferred", "payment",
        "upi", "neft", "imps", "rtgs", "atm", "account",
        "a/c", "ac no", "bank", "rupees", "rs.", "inr",
        "₹", "avl bal", "available balance", "stmt"
    )

    fun getBankMessages(limit: Int = 100): List<BankSMS> {
        val smsList = mutableListOf<BankSMS>()

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < 500) { // scan 500, filter bank ones
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val sender = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))

                    if (isBankMessage(sender, body)) {
                        val amount = extractAmount(body)
                        val type = extractTransactionType(body)

                        smsList.add(
                            BankSMS(
                                id = id,
                                sender = sender,
                                message = body,
                                timestamp = date,
                                amount = amount,
                                transactionType = type
                            )
                        )
                        count++
                        if (count >= limit) break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return smsList
    }

    private fun isBankMessage(sender: String, body: String): Boolean {
        val senderUpper = sender.uppercase()
        val bodyLower = body.lowercase()

        // Check sender ID
        val isBankSender = bankSenders.any { senderUpper.contains(it.uppercase()) }

        // Check body keywords
        val hasBankKeyword = bankKeywords.any { bodyLower.contains(it) }

        return isBankSender || hasBankKeyword
    }

    private fun extractAmount(body: String): Double? {
        // Patterns: Rs.1000, INR 1,000.00, ₹1000, Rs 500.00
        val patterns = listOf(
            Regex("""(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|₹)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1]
                    .replace(",", "")
                    .toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractTransactionType(body: String): String {
        val bodyLower = body.lowercase()
        return when {
            bodyLower.contains("credited") || bodyLower.contains("credit") ||
                    bodyLower.contains("deposited") || bodyLower.contains("received") -> "CREDIT"

            bodyLower.contains("debited") || bodyLower.contains("debit") ||
                    bodyLower.contains("withdrawn") || bodyLower.contains("paid") -> "DEBIT"

            else -> "INFO"
        }
    }
}