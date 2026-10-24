package com.clarity.finance

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.data.local.UserPreferencesRepository
import com.clarity.finance.ui.navigation.ClarityNavHost
import com.clarity.finance.ui.theme.ClarityFinanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository.lastActiveTimestamp = System.currentTimeMillis()

        setContent {
            val themeMode by userPreferencesRepository.themeModeFlow.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            ClarityFinanceTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClarityNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userPreferencesRepository.lastActiveTimestamp = System.currentTimeMillis()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        userPreferencesRepository.lastActiveTimestamp = System.currentTimeMillis()
    }
}