package com.clarity.finance.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun format_inLkr_formatsCorrectlyWithThousandsSeparator() {
        val result = CurrencyFormatter.format(12500.0, "LKR")
        assertEquals("12,500 LKR", result)
    }

    @Test
    fun format_inUsd_convertsUsingExchangeRate() {
        // 30,500 LKR / 305 = 100 USD
        val result = CurrencyFormatter.format(30500.0, "USD")
        assertEquals("$100", result)
    }

    @Test
    fun formatCompact_inLkr_formatsWithK() {
        val result = CurrencyFormatter.formatCompact(185000.0, "LKR")
        assertEquals("185.0k", result)
    }

    @Test
    fun formatCompact_inUsd_formatsWithK() {
        // 305,000 LKR / 305 = 1,000 USD -> $1.0k
        val result = CurrencyFormatter.formatCompact(305000.0, "USD")
        assertEquals("$1.0k", result)
    }

    @Test
    fun getSymbol_returnsDollarForUsdAndLkrForLkr() {
        assertEquals("$", CurrencyFormatter.getSymbol("USD"))
        assertEquals("LKR", CurrencyFormatter.getSymbol("LKR"))
    }
}
