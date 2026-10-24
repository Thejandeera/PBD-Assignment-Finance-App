package com.clarity.finance.util

import com.clarity.finance.model.DraftStatus
import com.clarity.finance.model.DraftTransaction
import com.clarity.finance.model.DraftTransactionType
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

object BankSmsParser {

    private val BOC_SENDER_REGEX = Regex("(?i).*(BOC|BANK OF CEYLON).*")
    private val COMBANK_SENDER_REGEX = Regex("(?i).*(COMBANK|COMMERCIAL).*")
    private val GENERAL_BANK_REGEX = Regex("(?i).*(HNB|SAMPATH|NDB|SEYLAN|DFCC|NTB|HSBC).*")

    // OTP / Security filter keywords: Never draft OTPs!
    private val SECURITY_KEYWORDS = listOf(
        "please use code",
        "do not share this number",
        "otp",
        "verification code",
        "security code",
        "password",
        "scheduled system maintenance"
    )

    /**
     * Parse an SMS body and sender.
     * Returns a [DraftTransaction] if recognized, or null if it's an OTP/informational message.
     */
    fun parseSms(sender: String, body: String, timestampMillis: Long = System.currentTimeMillis()): DraftTransaction? {
        val lowerBody = body.lowercase(Locale.ROOT)

        // Rule 0: Skip security OTPs & maintenance alerts
        if (SECURITY_KEYWORDS.any { lowerBody.contains(it) }) {
            return null
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestampMillis))
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestampMillis))

        // Check Bank of Ceylon (BOC)
        if (BOC_SENDER_REGEX.containsMatchIn(sender) || lowerBody.contains("boc") || lowerBody.contains("bank of ceylon")) {
            parseBocMessage(body, dateStr, timeStr)?.let { return it }
        }

        // Check Commercial Bank (COMBANK)
        if (COMBANK_SENDER_REGEX.containsMatchIn(sender) || lowerBody.contains("combank") || lowerBody.contains("commercial bank")) {
            parseComBankMessage(body, dateStr, timeStr)?.let { return it }
        }

        // Generic Sri Lankan Bank Fallback parser
        return parseGenericBankMessage(sender, body, dateStr, timeStr)
    }

    /**
     * Parses Bank of Ceylon (BOC) transaction SMS:
     * 1) "POS/ATM Transaction Rs 1020.00 From A/C No XXXXXXXXX838. Balance available Rs 3878.41 - Thank you for banking with BOC"
     * 2) "Online Transfer Credit Rs 1010.00 To A/C No XXXXXXXXX838. Balance available Rs 2015.41 - Thank you for banking with BOC"
     */
    private fun parseBocMessage(body: String, defaultDate: String, defaultTime: String): DraftTransaction? {
        // Pattern 1: POS/ATM Transaction (Outcome)
        val posPattern = Regex("(?i)POS/ATM\\s*Transaction\\s*Rs\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*From\\s*A/C\\s*No\\s*([A-Za-z0-9]+)")
        posPattern.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val account = match.groupValues[2]
            return DraftTransaction(
                id = UUID.randomUUID().toString(),
                type = DraftTransactionType.EXPENSE,
                amount = amount,
                sender = "BOC",
                merchantOrTitle = "BOC POS/ATM ($account)",
                accountNumber = account,
                suggestedCategoryId = "OTHER",
                suggestedCategoryName = "Other",
                spendingType = SpendingType.DISCRETIONARY,
                paymentMethod = PaymentMethod.CARD,
                date = defaultDate,
                time = defaultTime,
                rawMessage = body,
                status = DraftStatus.PENDING
            )
        }

        // Pattern 2: Online Transfer Credit (Income)
        val creditPattern = Regex("(?i)Online\\s*Transfer\\s*Credit\\s*Rs\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*To\\s*A/C\\s*No\\s*([A-Za-z0-9]+)")
        creditPattern.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val account = match.groupValues[2]
            return DraftTransaction(
                id = UUID.randomUUID().toString(),
                type = DraftTransactionType.INCOME,
                amount = amount,
                sender = "BOC",
                merchantOrTitle = "Online Transfer Credit ($account)",
                accountNumber = account,
                suggestedCategoryId = "INCOME",
                suggestedCategoryName = "Freelance / Income",
                spendingType = SpendingType.DISCRETIONARY,
                paymentMethod = PaymentMethod.ONLINE,
                date = defaultDate,
                time = defaultTime,
                rawMessage = body,
                status = DraftStatus.PENDING
            )
        }

        // Pure balance inquiry: skip
        if (body.contains("Balance available", ignoreCase = true) && !body.contains("Transaction", ignoreCase = true)) {
            return null
        }

        return null
    }

    /**
     * Parses Commercial Bank (COMBANK) transaction SMS:
     * 1) "Dear Cardholder, Purchase at CHATHURA BAKERS MULLERIYAWA N LK for LKR 1,690.00 on 23/10/26 01:56 PM has been authorised on your debit card ending #4261."
     */
    private fun parseComBankMessage(body: String, defaultDate: String, defaultTime: String): DraftTransaction? {
        val purchasePattern = Regex(
            "(?i)Purchase\\s*at\\s+(.+?)\\s+for\\s+(?:LKR|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+on\\s+([0-9/]+)\\s+([0-9:]+\\s*[APMapm]{2}).*?card ending #?([0-9]+)"
        )

        purchasePattern.find(body)?.let { match ->
            val rawMerchant = match.groupValues[1].trim()
            val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val rawDate = match.groupValues[3].trim()
            val rawTime = match.groupValues[4].trim()
            val cardEnding = match.groupValues[5].trim()

            val cleanMerchant = cleanMerchantName(rawMerchant)
            val (categoryId, categoryName, spendingType) = guessCategory(cleanMerchant)
            val parsedDate = normalizeDate(rawDate) ?: defaultDate

            return DraftTransaction(
                id = UUID.randomUUID().toString(),
                type = DraftTransactionType.EXPENSE,
                amount = amount,
                sender = "COMBANK",
                merchantOrTitle = cleanMerchant,
                accountNumber = "Card #$cardEnding",
                suggestedCategoryId = categoryId,
                suggestedCategoryName = categoryName,
                spendingType = spendingType,
                paymentMethod = PaymentMethod.CARD,
                date = parsedDate,
                time = rawTime.ifBlank { defaultTime },
                rawMessage = body,
                status = DraftStatus.PENDING
            )
        }

        // Simpler ComBank debit/transfer pattern
        val simpleDebit = Regex("(?i)(?:debited|paid|transfer(?:red)?).*?(?:LKR|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        simpleDebit.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return DraftTransaction(
                id = UUID.randomUUID().toString(),
                type = DraftTransactionType.EXPENSE,
                amount = amount,
                sender = "COMBANK",
                merchantOrTitle = "ComBank Digital Transfer",
                suggestedCategoryId = "OTHER",
                suggestedCategoryName = "Other",
                spendingType = SpendingType.DISCRETIONARY,
                paymentMethod = PaymentMethod.ONLINE,
                date = defaultDate,
                time = defaultTime,
                rawMessage = body,
                status = DraftStatus.PENDING
            )
        }

        return null
    }

    /**
     * Generic parser for other Sri Lankan banks (Sampath, HNB, Seylan, etc.)
     */
    private fun parseGenericBankMessage(sender: String, body: String, defaultDate: String, defaultTime: String): DraftTransaction? {
        val debitRegex = Regex("(?i)(?:debited|purchase|withdrawn|paid)\\s*(?:for|with|of)?\\s*(?:LKR|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        val creditRegex = Regex("(?i)(?:credited|received|deposit(?:ed)?)\\s*(?:for|with|of)?\\s*(?:LKR|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")

        debitRegex.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val (catId, catName, spendType) = guessCategory(body)
            return DraftTransaction(
                id = UUID.randomUUID().toString(),
                type = DraftTransactionType.EXPENSE,
                amount = amount,
                sender = sender.ifBlank { "BANK" },
                merchantOrTitle = "Debit Transaction",
                suggestedCategoryId = catId,
                suggestedCategoryName = catName,
                spendingType = spendType,
                paymentMethod = PaymentMethod.CARD,
                date = defaultDate,
                time = defaultTime,
                rawMessage = body,
                status = DraftStatus.PENDING
            )
        }

        creditRegex.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return DraftTransaction(
                id = UUID.randomUUID().toString(),
                type = DraftTransactionType.INCOME,
                amount = amount,
                sender = sender.ifBlank { "BANK" },
                merchantOrTitle = "Credit Deposit",
                suggestedCategoryId = "INCOME",
                suggestedCategoryName = "Income",
                spendingType = SpendingType.DISCRETIONARY,
                paymentMethod = PaymentMethod.ONLINE,
                date = defaultDate,
                time = defaultTime,
                rawMessage = body,
                status = DraftStatus.PENDING
            )
        }

        return null
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.replace(" N LK", "")
            .replace(" LK", "")
            .trim()
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Guesses category and committed/discretionary type based on merchant or text.
     */
    private fun guessCategory(text: String): Triple<String, String, SpendingType> {
        val upper = text.uppercase(Locale.ROOT)
        return when {
            listOf("BAKER", "BAKERY", "CAFE", "COFFEE", "FOOD", "RESTAURANT", "UBEREATS", "UBER EATS", "BARISTA", "JAVA", "PIZZA", "BURGER", "KFC", "MCDONALD").any { upper.contains(it) } ->
                Triple("FOOD_AND_COFFEE", "Food & Coffee", SpendingType.DISCRETIONARY)

            listOf("PICKME", "UBER", "PETROL", "FUEL", "CEYPETCO", "LIOC", "LAUGFS", "TAXI", "PARKING").any { upper.contains(it) } ->
                Triple("TRANSPORT", "Transport", SpendingType.DISCRETIONARY)

            listOf("CEB", "LECO", "WATER", "NWSDB", "DIALOG", "MOBITEL", "SLT", "FIBER", "TELECOM").any { upper.contains(it) } ->
                Triple("UTILITIES", "Utilities", SpendingType.COMMITTED)

            listOf("DARAZ", "CARGILLS", "KELLS", "ARPICO", "SPAR", "GLOMARK", "SUPERMARKET", "ODEL", "FASHION").any { upper.contains(it) } ->
                Triple("SHOPPING", "Shopping", SpendingType.DISCRETIONARY)

            listOf("NETFLIX", "SPOTIFY", "APPLE", "GOOGLE", "YOUTUBE", "GITHUB", "CHATGPT").any { upper.contains(it) } ->
                Triple("SUBSCRIPTIONS", "Subscriptions", SpendingType.COMMITTED)

            listOf("GYM", "FITNESS", "WORKOUT").any { upper.contains(it) } ->
                Triple("GYM", "Gym & Fitness", SpendingType.COMMITTED)

            listOf("RENT", "LEASE").any { upper.contains(it) } ->
                Triple("RENT", "Rent", SpendingType.COMMITTED)

            else -> Triple("OTHER", "Other", SpendingType.DISCRETIONARY)
        }
    }

    /**
     * Converts formats like "23/10/26" (DD/MM/YY) or "2026/10/23" to ISO "2026-10-23".
     */
    private fun normalizeDate(rawDate: String): String? {
        return try {
            val parts = rawDate.split("/", "-")
            if (parts.size == 3) {
                // Sri Lankan bank standard: DD/MM/YY or DD/MM/YYYY
                if (parts[2].length == 2 || parts[2].length == 4) {
                    val day = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year = if (parts[2].length == 2) "20${parts[2]}" else parts[2]
                    return "$year-$month-$day"
                }
                // ISO format: YYYY/MM/DD
                if (parts[0].length == 4) {
                    val year = parts[0]
                    val month = parts[1].padStart(2, '0')
                    val day = parts[2].padStart(2, '0')
                    return "$year-$month-$day"
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Preloaded sample bank SMS messages from the user's uploaded images and typical Kavindu transactions.
     * Perfect for 1-click loading during Live Demo and Emulator testing!
     */
    fun getSampleBankSmsList(): List<Pair<String, String>> = listOf(
        Pair(
            "COMBANK",
            "Dear Cardholder, Purchase at CHATHURA BAKERS MULLERIYAWA N LK for LKR 1,690.00 on 23/10/26 01:56 PM has been authorised on your debit card ending #4261."
        ),
        Pair(
            "BOC",
            "POS/ATM Transaction Rs 1020.00 From A/C No XXXXXXXXX838. Balance available Rs 3878.41 - Thank you for banking with BOC"
        ),
        Pair(
            "BOC",
            "POS/ATM Transaction Rs 1563.00 From A/C No XXXXXXXXX838. Balance available Rs 2315.41 - Thank you for banking with BOC"
        ),
        Pair(
            "BOC",
            "POS/ATM Transaction Rs 1310.00 From A/C No XXXXXXXXX838. Balance available Rs 1005.41 - Thank you for banking with BOC"
        ),
        Pair(
            "BOC",
            "Online Transfer Credit Rs 1010.00 To A/C No XXXXXXXXX838. Balance available Rs 2015.41 - Thank you for banking with BOC"
        ),
        Pair(
            "COMBANK",
            "Dear Cardholder, Purchase at PICKME RIDES COLOMBO LK for LKR 640.00 on 24/10/26 09:15 AM has been authorised on your debit card ending #4261."
        ),
        Pair(
            "COMBANK",
            "Dear Cardholder, Purchase at DIALOG BROADBAND BILL for LKR 4,950.00 on 24/10/26 11:30 AM has been authorised on your debit card ending #4261."
        )
    )
}
