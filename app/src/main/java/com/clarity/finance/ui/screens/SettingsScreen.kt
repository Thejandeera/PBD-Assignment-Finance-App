package com.clarity.finance.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.ui.components.AlertAction
import com.clarity.finance.ui.components.AlertActionStyle
import com.clarity.finance.ui.components.CheckmarkRow
import com.clarity.finance.ui.components.ClarityAlertDialog
import com.clarity.finance.ui.components.ClarityNavigationBar
import com.clarity.finance.ui.components.ClaritySnackbarHost
import com.clarity.finance.ui.components.DetailRow
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.Hairline
import com.clarity.finance.ui.components.IconTile
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.components.ProfileAvatar
import com.clarity.finance.ui.components.SegmentedControl
import com.clarity.finance.ui.components.SwitchRow
import com.clarity.finance.ui.components.TextLinkButton
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.components.pressFade
import com.clarity.finance.ui.components.rowHighlight
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.ProfileImageManager
import com.clarity.finance.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private val ThemeModes = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSignOutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToDrafts: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = ClarityTheme.colors
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by authViewModel.currentUserFlow.collectAsStateWithLifecycle()
    var displayName by remember(currentUser) { mutableStateOf(currentUser?.displayName.orEmpty()) }
    var savedDisplayName by remember(currentUser) { mutableStateOf(currentUser?.displayName.orEmpty()) }
    var selectedNewPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var isSavingProfile by remember { mutableStateOf(false) }

    // Settings states
    val themeMode by authViewModel.themeModeFlow.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    var isSmsEnabled by remember { mutableStateOf(authViewModel.isSmsScanEnabled) }
    var isDailyReminderEnabled by remember { mutableStateOf(authViewModel.isDailyReminderEnabled) }
    val canUseBiometrics by authViewModel.canUseBiometrics.collectAsStateWithLifecycle()

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            authViewModel.setSmsScanEnabled(true)
            isSmsEnabled = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedNewPhotoUri = uri
    }

    val currentPhotoPath = authViewModel.profilePhotoPath
    val avatarBitmap = remember(selectedNewPhotoUri, currentPhotoPath) {
        val newUri = selectedNewPhotoUri
        if (newUri != null) {
            try {
                context.contentResolver.openInputStream(newUri)?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        } else {
            ProfileImageManager.loadBitmap(currentPhotoPath)
        }
    }

    val hasProfileChanges = displayName.trim() != savedDisplayName.trim() || selectedNewPhotoUri != null
    val scrollState = rememberScrollState()
    val isCollapsed = scrollState.isScrolledPast(40.dp)

    if (showSignOutDialog) {
        ClarityAlertDialog(
            title = "Sign Out?",
            message = "You can sign back in at any time with your email and password.",
            onDismissRequest = { showSignOutDialog = false },
            actions = listOf(
                AlertAction(label = "Cancel", style = AlertActionStyle.Cancel) { showSignOutDialog = false },
                AlertAction(label = "Sign Out", style = AlertActionStyle.Destructive) {
                    showSignOutDialog = false
                    authViewModel.signOut()
                    onSignOutSuccess()
                }
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { ClaritySnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            ClarityNavigationBar(
                title = "Settings",
                collapsed = isCollapsed,
                onBack = onNavigateBack,
                backLabel = "Home"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            LargeTitleHeader(title = "Settings")

            // =================================================================
            // Profile header
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.pressFade(onClickLabel = "Change profile photo") {
                        photoPickerLauncher.launch("image/*")
                    }
                ) {
                    ProfileAvatar(
                        bitmap = avatarBitmap,
                        name = displayName.ifBlank { currentUser?.displayName.orEmpty() },
                        size = 96.dp,
                        contentDescription = "Profile photo"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.accentFill)
                            .border(2.dp, colors.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = savedDisplayName.ifBlank { "Your Name" },
                    style = ClarityType.title2,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Text(
                    text = currentUser?.email.orEmpty(),
                    style = ClarityType.subheadline,
                    color = colors.secondaryLabel
                )
                TextLinkButton(
                    text = if (selectedNewPhotoUri != null) "Choose a Different Photo" else "Change Photo",
                    onClick = { photoPickerLauncher.launch("image/*") },
                    style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // =================================================================
            // Profile details
            // =================================================================
            GroupedSection(
                header = "Profile",
                footer = "Your email address is used to sign in and can't be changed."
            ) {
                NameFieldRow(
                    value = displayName,
                    onValueChange = { displayName = it },
                    onDone = { focusManager.clearFocus() }
                )
                Hairline(modifier = Modifier.padding(start = 16.dp))
                DetailRow(label = "Email", value = currentUser?.email.orEmpty())
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = "Save Changes",
                isLoading = isSavingProfile,
                enabled = hasProfileChanges && displayName.isNotBlank() && !isSavingProfile,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    focusManager.clearFocus()
                    isSavingProfile = true
                    authViewModel.updateProfile(displayName, selectedNewPhotoUri) { success ->
                        isSavingProfile = false
                        if (success) {
                            savedDisplayName = displayName.trim()
                            selectedNewPhotoUri = null
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Profile updated." else "Couldn't update your profile. Try again."
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // =================================================================
            // Appearance
            // =================================================================
            GroupedSection(
                header = "Appearance",
                footer = "System matches your device's light or dark setting."
            ) {
                SegmentedControl(
                    options = ThemeModes.map { it.second },
                    selectedIndex = ThemeModes.indexOfFirst { it.first == themeMode }.coerceAtLeast(0),
                    onSelect = { index -> authViewModel.setThemeMode(ThemeModes[index].first) },
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // =================================================================
            // Currency
            // =================================================================
            GroupedSection(
                header = "Currency",
                footer = "Amounts are stored in LKR. US dollar values use the latest exchange rate."
            ) {
                CheckmarkRow(
                    title = "Sri Lankan Rupee",
                    detail = "LKR",
                    selected = currentCurrency.equals("LKR", ignoreCase = true),
                    onClick = { authViewModel.setCurrency("LKR") },
                    showDivider = true
                )
                CheckmarkRow(
                    title = "US Dollar",
                    detail = "USD",
                    selected = currentCurrency.equals("USD", ignoreCase = true),
                    onClick = { authViewModel.setCurrency("USD") }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // =================================================================
            // Automation
            // =================================================================
            GroupedSection(
                header = "Automation",
                footer = if (hasSmsPermission) {
                    "Detects BOC and ComBank debit and credit messages and drafts them for your review."
                } else {
                    "SMS access lets Clarity read BOC and ComBank messages and draft them as transactions for you to review."
                }
            ) {
                SwitchRow(
                    title = "Bank SMS Drafts",
                    checked = isSmsEnabled,
                    leading = { IconTile(icon = Icons.Rounded.Sms, background = colors.indigo) },
                    showDivider = true,
                    onCheckedChange = { checked ->
                        if (checked && !hasSmsPermission) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else {
                            isSmsEnabled = checked
                            authViewModel.setSmsScanEnabled(checked)
                        }
                    }
                )
                ListRow(
                    title = "Review SMS Drafts",
                    leading = { IconTile(icon = Icons.Rounded.Inbox, background = colors.blue) },
                    showChevron = true,
                    onClick = onNavigateToDrafts,
                    showDivider = true
                )
                if (!hasSmsPermission) {
                    ListRow(
                        title = "Allow SMS Access",
                        titleColor = colors.accent,
                        onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                        showDivider = true
                    )
                }
                SwitchRow(
                    title = "Daily Reminder",
                    subtitle = "If you haven't logged an expense today",
                    checked = isDailyReminderEnabled,
                    leading = { IconTile(icon = Icons.Rounded.Notifications, background = colors.red) },
                    onCheckedChange = { checked ->
                        isDailyReminderEnabled = checked
                        authViewModel.setDailyReminderEnabled(checked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // =================================================================
            // Security
            // =================================================================
            GroupedSection(
                header = "Security",
                footer = if (canUseBiometrics) {
                    "Sign in with your fingerprint instead of typing your password."
                } else {
                    "Available after you sign in with your password on this device."
                }
            ) {
                SwitchRow(
                    title = "Fingerprint Sign-In",
                    checked = canUseBiometrics,
                    leading = { IconTile(icon = Icons.Rounded.Fingerprint, background = colors.green) },
                    onCheckedChange = { checked -> authViewModel.setBiometricEnabled(checked) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // =================================================================
            // Session
            // =================================================================
            GroupedSection {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .rowHighlight(onClick = { showSignOutDialog = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Sign Out", style = ClarityType.body, color = colors.redText)
                }
            }

            Text(
                text = "Clarity Finance 1.0 · SE3092 Platform Based Development",
                style = ClarityType.footnote,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 20.dp)
            )

            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/** Editable "Name" row in the iOS Contacts style: label on the leading side, field beside it. */
@Composable
private fun NameFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Name",
            style = ClarityType.body,
            color = colors.label,
            modifier = Modifier.width(88.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = ClarityType.body.copy(color = colors.label, textAlign = TextAlign.End),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 13.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Your full name",
                            style = ClarityType.body,
                            color = colors.placeholder,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
