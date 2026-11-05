package com.clarity.finance.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Source of the LKR-per-USD rate, so view models can be tested without the network. */
interface ExchangeRateProvider {
    suspend fun getLkrPerUsdRate(): Double
}

/**
 * Fetches the live LKR-per-USD exchange rate from open.er-api.com.
 * Caches for 1 hour. Falls back to 330 LKR/USD on any error.
 */
@Singleton
class ExchangeRateRepository @Inject constructor() : ExchangeRateProvider {

    companion object {
        private const val FALLBACK_RATE = 330.0
        private const val CACHE_DURATION_MS = 3_600_000L  // 1 hour
        private const val RATE_API_URL = "https://open.er-api.com/v6/latest/USD"
    }

    private var cachedRate: Double = FALLBACK_RATE
    private var lastFetchMs: Long = 0L

    /**
     * Returns LKR-per-USD. Fetches live if cache is stale; otherwise returns cached value.
     * Never throws — returns [FALLBACK_RATE] on any network or parse error.
     */
    override suspend fun getLkrPerUsdRate(): Double {
        val now = System.currentTimeMillis()
        // Return cache if fresh (and not just the fallback initial value)
        if (now - lastFetchMs < CACHE_DURATION_MS && lastFetchMs != 0L) {
            return cachedRate
        }
        return try {
            val json = withContext(Dispatchers.IO) {
                URL(RATE_API_URL).readText()
            }
            val lkr = JSONObject(json).getJSONObject("rates").getDouble("LKR")
            cachedRate = lkr
            lastFetchMs = now
            lkr
        } catch (_: Exception) {
            FALLBACK_RATE
        }
    }
}
