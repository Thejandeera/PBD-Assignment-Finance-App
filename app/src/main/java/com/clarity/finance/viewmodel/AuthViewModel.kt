package com.clarity.finance.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.local.UserPreferencesRepository
import com.clarity.finance.data.repository.AuthRepository
import com.clarity.finance.model.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import com.clarity.finance.util.ProfileImageManager
import android.net.Uri
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: AuthUser) : AuthUiState
    data class RegisteredSuccess(val message: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val profileImageManager: ProfileImageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _canUseBiometrics = MutableStateFlow(userPreferencesRepository.canUseBiometrics())
    val canUseBiometrics: StateFlow<Boolean> = _canUseBiometrics.asStateFlow()

    val savedUserEmail: String?
        get() = userPreferencesRepository.getSavedEmail()

    val currentUserFlow: StateFlow<AuthUser?> = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUser
        )

    val isUserLoggedIn: Boolean
        get() = authRepository.currentUser != null

    fun checkBiometricStatus() {
        _canUseBiometrics.value = userPreferencesRepository.canUseBiometrics()
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your email address.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your password.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signIn(trimmedEmail, password)
                .onSuccess { user ->
                    // First login successful -> Save credentials securely for biometric sign-in
                    userPreferencesRepository.saveCredentials(trimmedEmail, password)
                    _canUseBiometrics.value = userPreferencesRepository.canUseBiometrics()
                    _uiState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(mapFirebaseError(error))
                }
        }
    }

    fun signUp(name: String, email: String, password: String, confirmPassword: String, photoUri: Uri? = null) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your full name.")
            return
        }
        if (trimmedEmail.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your email address.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 8) {
            _uiState.value = AuthUiState.Error("Password must be at least 8 characters.")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val tempId = java.util.UUID.randomUUID().toString()
            val initialSavedPath = photoUri?.let { profileImageManager.saveProfileImage(tempId, it) }

            authRepository.signUp(trimmedName, trimmedEmail, password, initialSavedPath)
                .onSuccess { user ->
                    if (photoUri != null) {
                        val permanentPath = profileImageManager.saveProfileImage(user.uid, photoUri)
                        userPreferencesRepository.profilePhotoPath = permanentPath
                    }
                    // Sign out immediately so user must perform their first login on LoginScreen
                    authRepository.signOut()
                    _uiState.value = AuthUiState.RegisteredSuccess(
                        "Account created successfully! Please log in for the first time."
                    )
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(mapFirebaseError(error))
                }
        }
    }

    fun updateProfile(name: String, photoUri: Uri? = null, onResult: ((Boolean) -> Unit)? = null) {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            val newPath = photoUri?.let { profileImageManager.saveProfileImage(user.uid, it) }
                ?: userPreferencesRepository.profilePhotoPath
            val result = authRepository.updateProfile(name.trim(), newPath)
            if (result.isSuccess) {
                userPreferencesRepository.profilePhotoPath = newPath
            }
            onResult?.invoke(result.isSuccess)
        }
    }

    val profilePhotoPath: String?
        get() = userPreferencesRepository.profilePhotoPath

    val themeMode: String
        get() = userPreferencesRepository.themeMode

    fun setThemeMode(mode: String) {
        userPreferencesRepository.themeMode = mode
    }

    val isSmsScanEnabled: Boolean
        get() = userPreferencesRepository.isSmsScanEnabled

    fun setSmsScanEnabled(enabled: Boolean) {
        userPreferencesRepository.isSmsScanEnabled = enabled
    }

    val isDailyReminderEnabled: Boolean
        get() = userPreferencesRepository.isDailyReminderEnabled

    fun setDailyReminderEnabled(enabled: Boolean) {
        userPreferencesRepository.isDailyReminderEnabled = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        userPreferencesRepository.isBiometricEnabled = enabled
        _canUseBiometrics.value = userPreferencesRepository.canUseBiometrics()
    }

    /**
     * Authenticates with real Firebase servers using saved credentials
     * after biometric hardware verification succeeds.
     */
    fun signInWithBiometrics() {
        val email = userPreferencesRepository.getSavedEmail()
        val password = userPreferencesRepository.getSavedPassword()

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            _uiState.value = AuthUiState.Error("No biometric credentials saved. Please log in with password first.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(mapFirebaseError(error))
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Idle
        checkBiometricStatus()
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun mapFirebaseError(throwable: Throwable): String {
        val msg = throwable.message.orEmpty()
        return when {
            msg.contains("password", ignoreCase = true) -> "Invalid or weak password. Please try again."
            msg.contains("email", ignoreCase = true) && msg.contains("already in use", ignoreCase = true) ->
                "An account with this email already exists."
            msg.contains("no user", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) ->
                "No account found with this email."
            msg.contains("network", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            else -> throwable.localizedMessage ?: "Authentication failed. Please check your credentials."
        }
    }
}
