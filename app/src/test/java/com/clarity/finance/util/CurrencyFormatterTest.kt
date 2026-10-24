package com.clarity.finance.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun format_inLkr_formatsCorrectlyWithThousandsSeparator() {
        val result = CurrencyFormatter.format(12500.0, "LKR")
        assertEquals("12,500 LKR", result)
    }

    @Test
    fun format_inUsd_convertsUsingFallbackRate330() {
        // 33,000 LKR / 330 = 100 USD
        val result = CurrencyFormatter.format(33000.0, "USD")
        assertEquals("\$100", result)
    }

    @Test
    fun format_inUsd_convertsUsingLiveRatePassedExplicitly() {
        // 30,000 LKR / 300 = 100 USD
        val result = CurrencyFormatter.format(30000.0, "USD", 300.0)
        assertEquals("\$100", result)
    }

    @Test
    fun formatCompact_inLkr_formatsWithK() {
        val result = CurrencyFormatter.formatCompact(185000.0, "LKR")
        assertEquals("185.0k", result)
    }

    @Test
    fun formatCompact_inUsd_formatsWithKUsingFallback330() {
        // 330,000 LKR / 330 = 1,000 USD -> $1.0k
        val result = CurrencyFormatter.formatCompact(330000.0, "USD")
        assertEquals("\$1.0k", result)
    }

    @Test
    fun formatCompact_inUsd_formatsWithKUsingCustomRate() {
        // 200,000 LKR / 200 = 1,000 USD -> $1.0k
        val result = CurrencyFormatter.formatCompact(200000.0, "USD", 200.0)
        assertEquals("\$1.0k", result)
    }

    @Test
    fun getSymbol_returnsDollarForUsdAndLkrForLkr() {
        assertEquals("\$", CurrencyFormatter.getSymbol("USD"))
        assertEquals("LKR", CurrencyFormatter.getSymbol("LKR"))
    }
}
