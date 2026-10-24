package com.clarity.finance.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.ui.components.ClarityTextField
import com.clarity.finance.ui.components.DashedInfoBox
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityBorder
import com.clarity.finance.ui.theme.ClarityGrayBackground
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityTextSecondary
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.util.ProfileImageManager
import com.clarity.finance.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSignOutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by authViewModel.currentUserFlow.collectAsStateWithLifecycle()
    var displayName by remember(currentUser) { mutableStateOf(currentUser?.displayName.orEmpty()) }
    var selectedNewPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Settings States
    var themeMode by remember { mutableStateOf(authViewModel.themeMode) }
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
        selectedNewPhotoUri = uri
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from Clarity Finance?",
                    color = ClarityTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.signOut()
                        onSignOutSuccess()
                    }
                ) {
                    Text(text = "Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(text = "Cancel", color = ClarityTextPrimary)
                }
            },
            containerColor = ClarityWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClarityWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ClarityTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "CONFIGURATION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextMuted
                    )
                    Text(
                        text = "Settings & Preferences",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // =========================================================================
            // Section 1: User Profile & Account
            // =========================================================================
            Text(
                text = "USER ACCOUNT",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClarityWhite, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Avatar with edit badge
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(ClarityGrayBackground)
                            .border(BorderStroke(1.5.dp, ClarityBorder), CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val currentPath = authViewModel.profilePhotoPath
                        val bitmap = remember(selectedNewPhotoUri, currentPath) {
                            if (selectedNewPhotoUri != null) {
                                try {
                                    context.contentResolver.openInputStream(selectedNewPhotoUri!!)?.use {
                                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                                    }
                                } catch (_: Exception) { null }
                            } else {
                                ProfileImageManager.loadBitmap(currentPath)
                            }
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                tint = ClarityTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Camera icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ClarityBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Picture",
                                tint = ClarityWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap photo to change",
                        fontSize = 11.sp,
                        color = ClarityTextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ClarityTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Display Name",
                        placeholder = "Your full name"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Read-only email field with lock icon
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = ClarityTextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EMAIL ADDRESS (FIXED)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ClarityTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ClarityGrayBackground, RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(6.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = currentUser?.email.orEmpty(),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ClarityTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "SAVE PROFILE CHANGES",
                        onClick = {
                            authViewModel.updateProfile(displayName, selectedNewPhotoUri) { success ->
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Profile updated successfully!")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to update profile.")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================================
            // Section 2: Appearance & Theme
            // =========================================================================
            Text(
                text = "APPEARANCE & THEME",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClarityWhite, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "App Theme Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClarityTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select visual style for Clarity Finance interface",
                        fontSize = 11.sp,
                        color = ClarityTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) ClarityBlack else ClarityWhite,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) ClarityBlack else ClarityBorder),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        themeMode = mode
                                        authViewModel.setThemeMode(mode)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ClarityWhite else ClarityTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================================
            // Section 3: Smart Automations & Permissions
            // =========================================================================
            Text(
                text = "PERMISSIONS & AUTOMATION",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClarityWhite, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    // SMS Auto-Scan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS",
                                    tint = ClarityTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bank SMS Auto-Drafting",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ClarityTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Detect BOC & COMBANK debit/credit messages",
                                fontSize = 11.sp,
                                color = ClarityTextSecondary
                            )
                        }

                        Switch(
                            checked = isSmsEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !hasSmsPermission) {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                } else {
                                    isSmsEnabled = checked
                                    authViewModel.setSmsScanEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ClarityWhite,
                                checkedTrackColor = ClarityBlack
                            )
                        )
                    }

                    if (!hasSmsPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SMS Permission required to read messages. Tap below to grant:",
                            fontSize = 11.sp,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ClarityBorder),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "GRANT SMS PERMISSION",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ClarityTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = ClarityTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Daily Expense Reminder",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ClarityTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gentle alert if no expenses logged for the day",
                                fontSize = 11.sp,
                                color = ClarityTextSecondary
                            )
                        }

                        Switch(
                            checked = isDailyReminderEnabled,
                            onCheckedChange = { checked ->
                                isDailyReminderEnabled = checked
                                authViewModel.setDailyReminderEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ClarityWhite,
                                checkedTrackColor = ClarityBlack
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometrics Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint",
                                    tint = ClarityTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Biometric Fingerprint Login",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ClarityTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (canUseBiometrics) "Hardware sensor verified & active" else "Available after password login",
                                fontSize = 11.sp,
                                color = ClarityTextSecondary
                            )
                        }

                        Switch(
                            checked = canUseBiometrics,
                            onCheckedChange = { checked ->
                                authViewModel.setBiometricEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ClarityWhite,
                                checkedTrackColor = ClarityBlack
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // =========================================================================
            // Section 5: Session Management (Bottom Log Out)
            // =========================================================================
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, Color(0xFFD32F2F))
            ) {
                Text(
                    text = "LOG OUT OF CLARITY",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Clarity Finance v1.0 • SE3092 Platform Based Development",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ClarityTextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
