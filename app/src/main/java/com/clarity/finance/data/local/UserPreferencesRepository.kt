package com.clarity.finance.data.local

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import com.clarity.finance.data.repository.ExchangeRateProvider
import com.clarity.finance.util.CurrencyFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface PrimaryAccountProvider {
    val primaryAccountIdFlow: StateFlow<String?>
    val defaultCurrencyFlow: StateFlow<String>
    fun setPrimaryAccountId(accountId: String?)
    fun togglePrimaryAccount(accountId: String)
    fun clearPrimaryAccountIfMatches(accountId: String)
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PrimaryAccountProvider, ExchangeRateProvider {
    private val prefs = context.getSharedPreferences("clarity_finance_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_SAVED_EMAIL = "key_saved_email"
        private const val KEY_SAVED_TOKEN = "key_saved_token"
        private const val KEY_PROFILE_PHOTO_PATH = "key_profile_photo_path"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_CURRENCY = "key_currency"
        private const val KEY_USD_TO_LKR_RATE = "key_usd_to_lkr_rate"
        private const val KEY_LAST_ACTIVE_TIME = "key_last_active_time"
        private const val KEY_SMS_SCAN_ENABLED = "key_sms_scan_enabled"
        private const val KEY_DAILY_REMINDER = "key_daily_reminder"
        private const val KEY_PRIMARY_ACCOUNT_ID = "key_primary_account_id"
    }

    private val _primaryAccountIdFlow = MutableStateFlow(
        prefs.getString(KEY_PRIMARY_ACCOUNT_ID, null)
    )
    override val primaryAccountIdFlow: StateFlow<String?> = _primaryAccountIdFlow.asStateFlow()

    override fun setPrimaryAccountId(accountId: String?) {
        prefs.edit().apply {
            if (accountId.isNullOrBlank()) remove(KEY_PRIMARY_ACCOUNT_ID)
            else putString(KEY_PRIMARY_ACCOUNT_ID, accountId)
        }.apply()
        _primaryAccountIdFlow.value = accountId?.takeIf { it.isNotBlank() }
    }

    override fun togglePrimaryAccount(accountId: String) {
        val next = if (_primaryAccountIdFlow.value == accountId) null else accountId
        setPrimaryAccountId(next)
    }

    override fun clearPrimaryAccountIfMatches(accountId: String) {
        if (_primaryAccountIdFlow.value == accountId) {
            setPrimaryAccountId(null)
        }
    }

    var profilePhotoPath: String?
        get() = prefs.getString(KEY_PROFILE_PHOTO_PATH, null)
        set(value) = prefs.edit().putString(KEY_PROFILE_PHOTO_PATH, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }

    private val _themeModeFlow = MutableStateFlow(themeMode)
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "LKR") ?: "LKR"
        set(value) {
            prefs.edit().putString(KEY_CURRENCY, value).apply()
            _currencyFlow.value = value
        }

    private val _currencyFlow = MutableStateFlow(currency)
    val currencyFlow: StateFlow<String> = _currencyFlow.asStateFlow()
    override val defaultCurrencyFlow: StateFlow<String> = currencyFlow

    var usdToLkrRate: Double
        get() {
            val stored = java.lang.Double.longBitsToDouble(
                prefs.getLong(
                    KEY_USD_TO_LKR_RATE,
                    java.lang.Double.doubleToRawLongBits(CurrencyFormatter.FALLBACK_LKR_PER_USD)
                )
            )
            return if (stored > 0.0 && stored.isFinite()) stored else CurrencyFormatter.FALLBACK_LKR_PER_USD
        }
        set(value) {
            if (value > 0.0 && value.isFinite()) {
                prefs.edit().putLong(KEY_USD_TO_LKR_RATE, java.lang.Double.doubleToRawLongBits(value)).apply()
                _usdToLkrRateFlow.value = value
            }
        }

    private val _usdToLkrRateFlow = MutableStateFlow(usdToLkrRate)
    val usdToLkrRateFlow: StateFlow<Double> = _usdToLkrRateFlow.asStateFlow()

    override suspend fun getLkrPerUsdRate(): Double = usdToLkrRate

    override fun observeLkrPerUsdRate(): Flow<Double> = usdToLkrRateFlow

    var lastActiveTimestamp: Long
        get() = prefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ACTIVE_TIME, value).apply()

    var isSmsScanEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_SCAN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SMS_SCAN_ENABLED, value).apply()

    var isDailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDER, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun saveCredentials(email: String, password: String) {
        val encodedPass = Base64.encodeToString(password.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_SAVED_EMAIL, email.trim())
            .putString(KEY_SAVED_TOKEN, encodedPass)
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .apply()
    }

    fun getSavedEmail(): String? = prefs.getString(KEY_SAVED_EMAIL, null)

    fun getSavedPassword(): String? {
        val encoded = prefs.getString(KEY_SAVED_TOKEN, null) ?: return null
        return try {
            String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun clearSavedCredentials() {
        prefs.edit()
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_SAVED_TOKEN)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
    }

    fun isDeviceBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun canUseBiometrics(): Boolean {
        return isBiometricEnabled &&
                isDeviceBiometricAvailable() &&
                !getSavedEmail().isNullOrBlank() &&
                !getSavedPassword().isNullOrBlank()
    }
}
