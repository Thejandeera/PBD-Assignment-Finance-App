package com.clarity.finance.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    /** Fallback LKR-per-USD rate if live fetch fails. */
    const val FALLBACK_LKR_PER_USD = 330.0

    /**
     * Format [amountInLkr] in the chosen [currency].
     * [lkrPerUsd] defaults to the fallback rate; pass a live rate when available.
     */
    fun format(amountInLkr: Double, currency: String, lkrPerUsd: Double = FALLBACK_LKR_PER_USD): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return if (currency.equals("USD", ignoreCase = true)) {
            val usd = amountInLkr / lkrPerUsd
            formatter.minimumFractionDigits = if (usd % 1.0 == 0.0) 0 else 2
            formatter.maximumFractionDigits = 2
            "\$" + formatter.format(usd)
        } else {
            formatter.maximumFractionDigits = 0
            formatter.format(amountInLkr) + " LKR"
        }
    }

    /**
     * Compact format — e.g. $1.2k or 145.0k
     */
    fun formatCompact(amountInLkr: Double, currency: String, lkrPerUsd: Double = FALLBACK_LKR_PER_USD): String {
        return if (currency.equals("USD", ignoreCase = true)) {
            val usd = amountInLkr / lkrPerUsd
            if (usd >= 1000.0) {
                String.format(Locale.US, "\$%.1fk", usd / 1000.0)
            } else {
                String.format(Locale.US, "\$%.0f", usd)
            }
        } else {
            if (amountInLkr >= 1000.0) {
                String.format(Locale.US, "%.1fk", amountInLkr / 1000.0)
            } else {
                String.format(Locale.US, "%.0f", amountInLkr)
            }
        }
    }

    /**
     * Format an [amount] that is already in [currencyCode] (LKR, USD or USDT) — used to show
     * income in the currency it was paid in. Negative values (crypto losses) get a minus sign.
     */
    fun formatOriginal(amount: Double, currencyCode: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        val sign = if (amount < 0) "-" else ""
        val digits = formatter.format(abs(amount))
        return when (currencyCode.uppercase(Locale.US)) {
            "USD" -> "$sign\$$digits"
            "LKR" -> "$sign$digits LKR"
            else -> "$sign$digits ${currencyCode.uppercase(Locale.US)}"
        }
    }

    fun getSymbol(currency: String): String {
        return if (currency.equals("USD", ignoreCase = true)) "\$" else "LKR"
    }
}
