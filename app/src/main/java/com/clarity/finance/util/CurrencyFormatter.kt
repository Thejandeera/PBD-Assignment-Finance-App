package com.clarity.finance.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    const val USD_TO_LKR_RATE = 305.0

    fun format(amountInLkr: Double, currency: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return if (currency.equals("USD", ignoreCase = true)) {
            val usd = amountInLkr / USD_TO_LKR_RATE
            formatter.minimumFractionDigits = if (usd % 1.0 == 0.0) 0 else 2
            formatter.maximumFractionDigits = 2
            "$" + formatter.format(usd)
        } else {
            formatter.maximumFractionDigits = 0
            formatter.format(amountInLkr) + " LKR"
        }
    }

    fun formatCompact(amountInLkr: Double, currency: String): String {
        return if (currency.equals("USD", ignoreCase = true)) {
            val usd = amountInLkr / USD_TO_LKR_RATE
            if (usd >= 1000.0) {
                String.format(Locale.US, "$%.1fk", usd / 1000.0)
            } else {
                String.format(Locale.US, "$%.0f", usd)
            }
        } else {
            if (amountInLkr >= 1000.0) {
                String.format(Locale.US, "%.1fk", amountInLkr / 1000.0)
            } else {
                String.format(Locale.US, "%.0f", amountInLkr)
            }
        }
    }

    fun getSymbol(currency: String): String {
        return if (currency.equals("USD", ignoreCase = true)) "$" else "LKR"
    }
}
