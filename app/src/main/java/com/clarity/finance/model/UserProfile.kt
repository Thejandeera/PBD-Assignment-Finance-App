package com.clarity.finance.model

/**
 * Profile document stored at users/{uid}.
 * rates holds LKR per 1 unit of each foreign currency, e.g. USD -> 305.0.
 */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val baseCurrency: String = CurrencyType.LKR.name,
    val rates: Map<String, Double> = DEFAULT_RATES
) {
    companion object {
        const val DEFAULT_USD_RATE = 305.0
        const val DEFAULT_USDT_RATE = 305.0

        val DEFAULT_RATES: Map<String, Double> = mapOf(
            CurrencyType.USD.name to DEFAULT_USD_RATE,
            CurrencyType.USDT.name to DEFAULT_USDT_RATE
        )
    }
}
