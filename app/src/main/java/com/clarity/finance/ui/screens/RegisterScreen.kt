package com.clarity.finance.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.clarity.finance.model.ACCOUNT_CURRENCIES
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountType
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavigationBar
import com.clarity.finance.ui.components.ClarityTextField
import com.clarity.finance.ui.components.EmojiBadge
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.InfoNote
import com.clarity.finance.ui.components.InlineErrorMessage
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.components.ProfileAvatar
import com.clarity.finance.ui.components.SectionHeaderLabel
import com.clarity.finance.ui.components.TextLinkButton
import com.clarity.finance.ui.components.TintedButton
import com.clarity.finance.ui.components.accountTypeColor
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.components.pressFade
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthUiState
import com.clarity.finance.viewmodel.AuthViewModel
import java.util.UUID

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

    // 2-step onboarding: Step 1 = profile details, Step 2 = optional initial accounts
    var step by rememberSaveable { mutableIntStateOf(1) }
    var pendingAccounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var accName by rememberSaveable { mutableStateOf("") }
    var accType by rememberSaveable { mutableStateOf(AccountType.BANK) }
    var accCurrency by rememberSaveable { mutableStateOf(CurrencyType.LKR) }
    var accBalance by rememberSaveable { mutableStateOf("") }
    var accNameError by remember { mutableStateOf<String?>(null) }
    var accBalanceError by remember { mutableStateOf<String?>(null) }

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
        if (step == 2 && !isLoading) {
            step = 1
        } else {
            onNavigateToLogin()
        }
    }
    val goNextStep = {
        focusManager.clearFocus()
        if (authViewModel.validateSignUpStepOne(fullName, email, password, confirmPassword)) {
            step = 2
        }
    }
    val tryAddPendingAccount = {
        focusManager.clearFocus()
        val trimmed = accName.trim()
        val parsedBal = accBalance.trim().toDoubleOrNull()
        accNameError = if (trimmed.isEmpty()) "Enter an account name" else null
        accBalanceError = if (parsedBal == null || !parsedBal.isFinite()) "Enter a valid balance" else null
        if (accNameError == null && accBalanceError == null && parsedBal != null) {
            pendingAccounts = pendingAccounts + Account(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                type = accType,
                currency = accCurrency,
                currentBalance = parsedBal
            )
            accName = ""
            accBalance = ""
            accType = AccountType.BANK
            accCurrency = CurrencyType.LKR
        }
    }
    val finishSignUp: (Boolean) -> Unit = { skipAccounts ->
        focusManager.clearFocus()
        val finalAccounts = if (skipAccounts) {
            emptyList()
        } else {
            val draftBal = accBalance.trim().toDoubleOrNull()
            if (accName.isNotBlank() && draftBal != null && draftBal.isFinite()) {
                pendingAccounts + Account(
                    id = UUID.randomUUID().toString(),
                    name = accName.trim(),
                    type = accType,
                    currency = accCurrency,
                    currentBalance = draftBal
                )
            } else {
                pendingAccounts
            }
        }
        authViewModel.signUp(
            name = fullName,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            photoUri = selectedPhotoUri,
            initialAccounts = finalAccounts
        )
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
            title = if (step == 1) "Create Account" else "Set Up Accounts",
            collapsed = isCollapsed,
            onBack = goBack,
            backLabel = if (step == 1) "Sign In" else "Back"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            if (step == 1) {
                LargeTitleHeader(
                    title = "Create Account",
                    subtitle = "Step 1 of 2 · Your profile details"
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
                    keyboardActions = KeyboardActions(onDone = { goNextStep() })
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    InlineErrorMessage(
                        message = errorMessage.orEmpty(),
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoNote(text = "Your base currency is LKR. Next, you can add your bank, cash, or crypto accounts — or skip.")

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Next",
                    onClick = { goNextStep() },
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
            } else {
                LargeTitleHeader(
                    title = "Add Accounts",
                    subtitle = "Step 2 of 2 · Add bank, cash, or crypto accounts, or skip for later."
                )

                if (pendingAccounts.isNotEmpty()) {
                    SectionHeaderLabel(
                        text = "Added Accounts (${pendingAccounts.size})",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    GroupedSection {
                        pendingAccounts.forEachIndexed { index, account ->
                            ListRow(
                                title = account.name,
                                subtitle = "${account.type.displayName} · ${CurrencyFormatter.formatOriginal(account.currentBalance, account.currency.name)}",
                                leading = {
                                    EmojiBadge(
                                        emoji = account.type.icon,
                                        tint = accountTypeColor(account.type)
                                    )
                                },
                                trailing = {
                                    IconButton(
                                        onClick = {
                                            pendingAccounts = pendingAccounts.filterNot { it.id == account.id }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove ${account.name}",
                                            tint = colors.secondaryLabel
                                        )
                                    }
                                },
                                showDivider = index < pendingAccounts.lastIndex
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    var typeMenuExpanded by remember { mutableStateOf(false) }
                    var currencyMenuExpanded by remember { mutableStateOf(false) }

                    ClarityCard {
                        ClarityTextField(
                            value = accName,
                            onValueChange = {
                                accName = it
                                accNameError = null
                            },
                            label = "Account Name",
                            placeholder = "e.g. Commercial Bank",
                            isError = accNameError != null,
                            errorMessage = accNameError,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OnboardingDropdownField(
                                    label = "Account Type",
                                    value = "${accType.icon} ${accType.displayName}",
                                    onClick = { typeMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = typeMenuExpanded,
                                    onDismissRequest = { typeMenuExpanded = false }
                                ) {
                                    AccountType.entries.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${item.icon}  ${item.displayName}",
                                                    style = ClarityType.body
                                                )
                                            },
                                            trailingIcon = if (accType == item) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = colors.accent
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                accType = item
                                                typeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(0.72f)) {
                                OnboardingDropdownField(
                                    label = "Currency",
                                    value = accCurrency.name,
                                    onClick = { currencyMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = currencyMenuExpanded,
                                    onDismissRequest = { currencyMenuExpanded = false }
                                ) {
                                    ACCOUNT_CURRENCIES.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(text = item.name, style = ClarityType.body) },
                                            trailingIcon = if (accCurrency == item) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = colors.accent
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                accCurrency = item
                                                currencyMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ClarityTextField(
                            value = accBalance,
                            onValueChange = { raw ->
                                accBalance = raw.filterIndexed { i, ch ->
                                    ch.isDigit() || ch == '.' || (ch == '-' && i == 0)
                                }
                                accBalanceError = null
                            },
                            label = "Starting Balance (${accCurrency.name})",
                            placeholder = "e.g. 100000",
                            isError = accBalanceError != null,
                            errorMessage = accBalanceError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TintedButton(
                        text = "Add Another Account",
                        leadingIcon = Icons.Rounded.Add,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { tryAddPendingAccount() }
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        InlineErrorMessage(
                            message = errorMessage.orEmpty(),
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TintedButton(
                            text = "Skip",
                            enabled = !isLoading,
                            tint = colors.secondaryLabel,
                            modifier = Modifier.weight(1f),
                            onClick = { finishSignUp(true) }
                        )
                        PrimaryButton(
                            text = "Create Account",
                            isLoading = isLoading,
                            modifier = Modifier.weight(2f),
                            onClick = { finishSignUp(false) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

@Composable
private fun OnboardingDropdownField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = ClarityType.footnote.copy(fontWeight = FontWeight.Medium),
            color = colors.secondaryLabel,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .clip(com.clarity.finance.ui.theme.ClarityShapes.control)
                .background(colors.tertiaryFill)
                .pressFade(onClickLabel = "Select $label", onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = ClarityType.body,
                color = colors.label,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = colors.secondaryLabel,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
