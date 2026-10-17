package com.clarity.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.ui.components.ClarityTextField
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityTextSecondary
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.util.BiometricPromptHelper
import com.clarity.finance.viewmodel.AuthUiState
import com.clarity.finance.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    initialMessage: String? = null
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val canUseBiometrics by authViewModel.canUseBiometrics.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by rememberSaveable { mutableStateOf(authViewModel.savedUserEmail ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.checkBiometricStatus()
        if (!initialMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(initialMessage)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onLoginSuccess()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClarityWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Title & Subtitle (No placeholder logo box)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Finance App",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextPrimary
                )
                Text(
                    text = "Simple Personal Finance",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your credentials to access your ledger",
                        fontSize = 14.sp,
                        color = ClarityTextSecondary
                    )
                }
            }

            // Form inputs & Action buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                ClarityTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Forgot password?",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextSecondary
                    )
                }

                ClarityTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "",
                    placeholder = "Enter your password",
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingText = if (isPasswordVisible) "[HIDE]" else "[SHOW]",
                    onTrailingTextClick = { isPasswordVisible = !isPasswordVisible },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(20.dp))

                val isLoading = uiState is AuthUiState.Loading
                PrimaryButton(
                    text = "LOGIN",
                    onClick = {
                        authViewModel.signIn(email, password)
                    },
                    isLoading = isLoading
                )

                // Biometric Login Button (Available after first successful login)
                if (canUseBiometrics) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                BiometricPromptHelper.showBiometricPrompt(
                                    activity = activity,
                                    onSuccess = {
                                        authViewModel.onBiometricAuthSuccess()
                                    },
                                    onError = { errorMsg ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(errorMsg)
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClarityBlack)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Login",
                            tint = ClarityBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIGN IN WITH BIOMETRICS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClarityBlack,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Footer: Register link (No wireframe component text)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OR",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "New here? ",
                        fontSize = 14.sp,
                        color = ClarityTextSecondary
                    )
                    Text(
                        text = "Create Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextPrimary,
                        modifier = Modifier.clickable {
                            authViewModel.resetState()
                            onNavigateToRegister()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
