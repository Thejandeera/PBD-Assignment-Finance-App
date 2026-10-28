package com.clarity.finance

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
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
        // Draw behind the status and navigation bars, as iOS does, so the app's own bar
        // materials extend to the screen edges.
        enableEdgeToEdge()
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

            // Keep status/navigation bar icons legible when the in-app appearance differs
            // from the system setting.
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(LightNavigationScrim, DarkNavigationScrim) { isDark }
                )
                onDispose {}
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

    private companion object {
        // Scrims only appear behind three-button navigation on older Android versions.
        val LightNavigationScrim = Color.argb(0xE6, 0xF9, 0xF9, 0xF9)
        val DarkNavigationScrim = Color.argb(0x80, 0x16, 0x16, 0x18)
    }
}
