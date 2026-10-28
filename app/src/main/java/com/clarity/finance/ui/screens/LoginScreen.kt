package com.clarity.finance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.ui.components.ClaritySnackbarHost
import com.clarity.finance.ui.components.ClarityTextField
import com.clarity.finance.ui.components.InlineErrorMessage
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.components.TextLinkButton
import com.clarity.finance.ui.components.TintedButton
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
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
    val colors = ClarityTheme.colors
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val canUseBiometrics by authViewModel.canUseBiometrics.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf(authViewModel.savedUserEmail ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    val clearError = { if (uiState is AuthUiState.Error) authViewModel.resetState() }
    val submit = {
        focusManager.clearFocus()
        authViewModel.signIn(email, password)
    }

    LaunchedEffect(Unit) {
        authViewModel.checkBiometricStatus()
        if (!initialMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(initialMessage)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            AppMark()

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Clarity Finance",
                style = ClarityType.title1,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sign in to see where every rupee goes.",
                style = ClarityType.body,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                ClarityTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        clearError()
                    },
                    label = "Email",
                    placeholder = "name@example.com",
                    containerColor = colors.card,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        autoCorrect = false,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(14.dp))

                ClarityTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        clearError()
                    },
                    label = "Password",
                    placeholder = "Enter your password",
                    containerColor = colors.card,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    trailingIconContentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                    onTrailingIconClick = { isPasswordVisible = !isPasswordVisible },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    InlineErrorMessage(
                        message = errorMessage.orEmpty(),
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Sign In",
                    onClick = { submit() },
                    isLoading = isLoading
                )

                if (canUseBiometrics) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TintedButton(
                        text = "Sign In with Fingerprint",
                        leadingIcon = Icons.Rounded.Fingerprint,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                BiometricPromptHelper.showBiometricPrompt(
                                    activity = activity,
                                    onSuccess = {
                                        authViewModel.signInWithBiometrics()
                                    },
                                    onError = { errorMsg ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(errorMsg)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "New to Clarity?",
                    style = ClarityType.subheadline,
                    color = colors.secondaryLabel
                )
                TextLinkButton(
                    text = "Create Account",
                    style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    onClick = {
                        authViewModel.resetState()
                        onNavigateToRegister()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        ClaritySnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

/** App mark: the brand glyph on a rounded-square tile, echoing the home-screen icon shape. */
@Composable
internal fun AppMark(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(84.dp)
            .shadow(elevation = 10.dp, shape = shape, ambientColor = Color(0xFF248A3D), spotColor = Color(0xFF248A3D))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF1E7A35)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PieChart,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(46.dp)
        )
    }
}
