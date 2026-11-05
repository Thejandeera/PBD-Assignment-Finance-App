package com.clarity.finance.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.ui.components.ClarityNavigationBar
import com.clarity.finance.ui.components.ClarityTextField
import com.clarity.finance.ui.components.InfoNote
import com.clarity.finance.ui.components.InlineErrorMessage
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.components.ProfileAvatar
import com.clarity.finance.ui.components.TextLinkButton
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.components.pressFade
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.viewmodel.AuthUiState
import com.clarity.finance.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = ClarityTheme.colors
    val focusManager = LocalFocusManager.current
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message
    val scrollState = rememberScrollState()
    val isCollapsed = scrollState.isScrolledPast(40.dp)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedPhotoUri = uri
    }

    val photoBitmap = remember(selectedPhotoUri) {
        selectedPhotoUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val clearError = { if (uiState is AuthUiState.Error) authViewModel.resetState() }
    val goBack = {
        authViewModel.resetState()
        onNavigateToLogin()
    }
    val submit = {
        focusManager.clearFocus()
        authViewModel.signUp(fullName, email, password, confirmPassword, selectedPhotoUri)
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AuthUiState.RegisteredSuccess) {
            onRegisterSuccess(state.message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ClarityNavigationBar(
            title = "Create Account",
            collapsed = isCollapsed,
            onBack = goBack,
            backLabel = "Sign In"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            LargeTitleHeader(
                title = "Create Account",
                subtitle = "Set up your profile to start tracking your income and spending."
            )

            // Optional profile photo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.pressFade(onClickLabel = "Choose profile photo") {
                        photoPickerLauncher.launch("image/*")
                    }
                ) {
                    if (photoBitmap == null && fullName.isBlank()) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(colors.tertiaryFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddAPhoto,
                                contentDescription = "Add profile photo",
                                tint = colors.secondaryLabel,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        ProfileAvatar(
                            bitmap = photoBitmap,
                            name = fullName,
                            size = 92.dp,
                            contentDescription = "Profile photo preview"
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.accentFill)
                                .border(2.dp, colors.background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextLinkButton(
                        text = if (selectedPhotoUri != null) "Change Photo" else "Add Photo (Optional)",
                        onClick = { photoPickerLauncher.launch("image/*") },
                        style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (selectedPhotoUri != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        TextLinkButton(
                            text = "Remove",
                            onClick = { selectedPhotoUri = null },
                            color = colors.redText,
                            style = ClarityType.subheadline
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ClarityTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        clearError()
                    },
                    label = "Full Name",
                    placeholder = "e.g. Kavindu Silva",
                    containerColor = colors.card,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(14.dp))

                ClarityTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        clearError()
                    },
                    label = "Email",
                    placeholder = "e.g. kavindu@example.com",
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
                    placeholder = "At least 8 characters",
                    containerColor = colors.card,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    trailingIconContentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                    onTrailingIconClick = { isPasswordVisible = !isPasswordVisible },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(14.dp))

                ClarityTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        clearError()
                    },
                    label = "Confirm Password",
                    placeholder = "Re-enter your password",
                    containerColor = colors.card,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

                Spacer(modifier = Modifier.height(16.dp))

                InfoNote(text = "Your base currency is LKR. You can show amounts in US dollars later from Settings.")

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Create Account",
                    onClick = { submit() },
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = ClarityType.subheadline,
                        color = colors.secondaryLabel
                    )
                    TextLinkButton(
                        text = "Sign In",
                        onClick = goBack,
                        style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}
