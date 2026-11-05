package com.clarity.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountBalanceCalculator
import com.clarity.finance.ui.components.ActivityIndicator
import com.clarity.finance.ui.components.AddEditAccountSheet
import com.clarity.finance.ui.components.CircleIconButton
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavigationBar
import com.clarity.finance.ui.components.ClaritySnackbarHost
import com.clarity.finance.ui.components.DeleteAccountAlert
import com.clarity.finance.ui.components.EmojiBadge
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.SectionFooterLabel
import com.clarity.finance.ui.components.SectionTitle
import com.clarity.finance.ui.components.SwipeToDeleteContainer
import com.clarity.finance.ui.components.accountTypeColor
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AccountViewModel
import com.clarity.finance.viewmodel.AuthViewModel

@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val colors = ClarityTheme.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val displayCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val isCollapsed = scrollState.isScrolledPast(40.dp)

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onUserMessageShown()
        }
    }

    if (showAddSheet || editingAccount != null) {
        val target = editingAccount
        AddEditAccountSheet(
            account = target,
            initialIsPrimary = target != null && target.id == state.primaryAccountId,
            onSave = { name, type, currency, balanceInput, isPrimary ->
                if (target == null) {
                    viewModel.addAccount(name, type, currency, balanceInput, isPrimary)
                } else {
                    viewModel.updateAccount(target, name, type, currency, balanceInput, isPrimary)
                }
            },
            onDelete = if (target != null) {
                { viewModel.deleteAccount(target) }
            } else null,
            onDismiss = {
                showAddSheet = false
                editingAccount = null
            }
        )
    }

    accountToDelete?.let { account ->
        DeleteAccountAlert(
            account = account,
            onConfirm = {
                viewModel.deleteAccount(account)
                accountToDelete = null
            },
            onDismiss = { accountToDelete = null }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { ClaritySnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            ClarityNavigationBar(
                title = "Accounts",
                collapsed = isCollapsed,
                onBack = onNavigateBack,
                trailing = {
                    if (isCollapsed) {
                        CircleIconButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "Add account",
                            onClick = { showAddSheet = true }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(scrollState)
        ) {
            LargeTitleHeader(
                title = "Accounts",
                trailing = {
                    CircleIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add account",
                        filled = true,
                        onClick = { showAddSheet = true }
                    )
                }
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ActivityIndicator(size = 28.dp)
                }

                state.errorMessage != null -> GroupedSection {
                    EmptyStateView(
                        icon = Icons.Rounded.CloudOff,
                        title = "Couldn't Load Accounts",
                        message = state.errorMessage.orEmpty()
                    )
                }

                else -> {
                    TotalBalanceCard(
                        totalBalanceLkr = state.totalBalanceLkr,
                        accountCount = state.accounts.size,
                        displayCurrency = displayCurrency,
                        exchangeRate = exchangeRate,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle(
                        title = "Your Accounts",
                        actionLabel = "+ Add",
                        onAction = { showAddSheet = true }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.accounts.isEmpty()) {
                        GroupedSection {
                            EmptyStateView(
                                icon = Icons.Rounded.AccountBalance,
                                title = "No Accounts Yet",
                                message = "Add your bank accounts, cash, crypto, or savings to track balances automatically.",
                                actionLabel = "Add Account",
                                onAction = { showAddSheet = true }
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            state.accounts.forEachIndexed { index, account ->
                                val nativeBalance = CurrencyFormatter.formatOriginal(
                                    account.currentBalance,
                                    account.currency.name
                                )
                                val convertedText = if (!account.currency.name.equals(displayCurrency, ignoreCase = true)) {
                                    val lkrValue = AccountBalanceCalculator.toLkr(
                                        account.currentBalance,
                                        account.currency,
                                        exchangeRate
                                    )
                                    "≈ " + CurrencyFormatter.format(lkrValue, displayCurrency, exchangeRate)
                                } else {
                                    null
                                }

                                val isPrimary = account.id == state.primaryAccountId
                                SwipeToDeleteContainer(
                                    onDeleteRequest = { accountToDelete = account },
                                    shape = groupedItemShape(index, state.accounts.size)
                                ) {
                                    Box(modifier = Modifier.background(colors.card)) {
                                        ListRow(
                                            title = account.name,
                                            subtitle = buildString {
                                                append(account.type.displayName)
                                                append(" · ")
                                                append(account.currency.name)
                                                if (isPrimary) append(" · ★ Primary")
                                            },
                                            leading = {
                                                EmojiBadge(
                                                    emoji = account.type.icon,
                                                    tint = accountTypeColor(account.type)
                                                )
                                            },
                                            trailing = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = nativeBalance,
                                                            style = ClarityType.amountBody,
                                                            color = if (account.currentBalance < 0) colors.redText else colors.label,
                                                            maxLines = 1
                                                        )
                                                        if (convertedText != null) {
                                                            Text(
                                                                text = convertedText,
                                                                style = ClarityType.footnote,
                                                                color = colors.secondaryLabel,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = { viewModel.togglePrimaryAccount(account) },
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPrimary) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                                            contentDescription = if (isPrimary) "Unset primary account" else "Set as primary account",
                                                            tint = if (isPrimary) colors.accent else colors.tertiaryLabel,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            showChevron = false,
                                            showDivider = index < state.accounts.lastIndex,
                                            onClick = { editingAccount = account },
                                            onClickLabel = "Edit ${account.name}"
                                        )
                                    }
                                }
                            }
                        }
                        SectionFooterLabel(
                            text = "Tap ★ to toggle your primary default account, tap a row to edit, or swipe left to delete.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun TotalBalanceCard(
    totalBalanceLkr: Double,
    accountCount: Int,
    displayCurrency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    ClarityCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = "Total Across Accounts",
            style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = colors.secondaryLabel
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = CurrencyFormatter.format(totalBalanceLkr, displayCurrency, exchangeRate),
            style = ClarityType.amountHero,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (accountCount) {
                0 -> "No active accounts"
                1 -> "1 active account"
                else -> "$accountCount active accounts"
            },
            style = ClarityType.footnote,
            color = colors.secondaryLabel
        )
    }
}
