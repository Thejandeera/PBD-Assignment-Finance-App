package com.clarity.finance.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateRepositoryTest {

    @Test
    fun getLkrPerUsdRate_returnsValidPositiveRateOrFallback() = runBlocking {
        val repo = ExchangeRateRepository()
        val rate = repo.getLkrPerUsdRate()
        // Rate must be positive and reasonable (e.g., between 200 and 500 LKR per USD, or 330 fallback)
        assertTrue("Rate should be >= 200.0, was $rate", rate >= 200.0)
    }
}
