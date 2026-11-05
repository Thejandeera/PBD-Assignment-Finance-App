package com.clarity.finance.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Account
import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.DraftTransaction
import com.clarity.finance.model.DraftTransactionType
import com.clarity.finance.model.getDefaultCategories
import com.clarity.finance.ui.components.ActivityIndicator
import com.clarity.finance.ui.components.AlertAction
import com.clarity.finance.ui.components.AlertActionStyle
import com.clarity.finance.ui.components.ClarityAlertDialog
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavigationBar
import com.clarity.finance.ui.components.ClaritySnackbarHost
import com.clarity.finance.ui.components.DisplayDates
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.LegendDot
import com.clarity.finance.ui.components.PrimaryButton
import com.clarity.finance.ui.components.SectionHeaderLabel
import com.clarity.finance.ui.components.TextLinkButton
import com.clarity.finance.ui.components.TintedButton
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.components.pressFade
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.DraftViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraftsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    draftViewModel: DraftViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val colors = ClarityTheme.colors
    val uiState by draftViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by draftViewModel.accounts.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showPasteSmsDialog by remember { mutableStateOf(false) }
    var pastedSmsText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val isCollapsed = listState.isScrolledPast(40.dp)
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            draftViewModel.clearFeedback()
        }
    }

    if (showPasteSmsDialog) {
        AlertDialog(
            onDismissRequest = { showPasteSmsDialog = false },
            containerColor = colors.card,
            title = { Text("Paste Bank SMS", color = colors.label, style = ClarityType.headline) },
            text = {
                OutlinedTextField(
                    value = pastedSmsText,
                    onValueChange = { pastedSmsText = it },
                    placeholder = { Text("Paste BOC or ComBank SMS message...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        draftViewModel.parsePastedSms(pastedSmsText)
                        pastedSmsText = ""
                        showPasteSmsDialog = false
                    }
                ) {
                    Text("Parse SMS", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteSmsDialog = false }) {
                    Text("Cancel", color = colors.secondaryLabel)
                }
            }
        )
    }

    if (showClearConfirmation) {
        val count = uiState.drafts.size
        ClarityAlertDialog(
            title = "Clear All Drafts?",
            message = if (count == 1) {
                "The pending draft will be discarded. Expenses you've already logged aren't affected."
            } else {
                "All $count pending drafts will be discarded. Expenses you've already logged aren't affected."
            },
            onDismissRequest = { showClearConfirmation = false },
            actions = listOf(
                AlertAction(label = "Cancel", style = AlertActionStyle.Cancel) { showClearConfirmation = false },
                AlertAction(label = "Clear All", style = AlertActionStyle.Destructive) {
                    showClearConfirmation = false
                    draftViewModel.clearAll()
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
                title = "SMS Drafts",
                collapsed = isCollapsed,
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = navigationBarPadding + 32.dp)
        ) {
            item(key = "title") {
                LargeTitleHeader(
                    title = "SMS Drafts",
                    subtitle = "Transactions detected in BOC and ComBank messages"
                )
            }

            item(key = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TintedButton(
                        text = "Paste SMS",
                        leadingIcon = Icons.Rounded.Sms,
                        enabled = !uiState.isScanning,
                        minHeight = 44.dp,
                        textStyle = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                        onClick = { showPasteSmsDialog = true }
                    )
                    TintedButton(
                        text = "Load Samples",
                        leadingIcon = Icons.Rounded.Download,
                        enabled = !uiState.isScanning,
                        minHeight = 44.dp,
                        textStyle = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                        onClick = { draftViewModel.loadSampleSmsDrafts() }
                    )
                }
            }

            if (uiState.drafts.isNotEmpty() && !uiState.isScanning) {
                item(key = "pending-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderLabel(
                            text = "${uiState.drafts.size} Pending",
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 7.dp)
                        )
                        TextLinkButton(
                            text = "Clear All",
                            onClick = { showClearConfirmation = true },
                            color = colors.redText,
                            style = ClarityType.subheadline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextLinkButton(
                            text = "Approve All",
                            onClick = { draftViewModel.approveAll() },
                            style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            when {
                uiState.isScanning -> item(key = "scanning") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActivityIndicator(size = 28.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scanning messages…",
                            style = ClarityType.subheadline,
                            color = colors.secondaryLabel
                        )
                    }
                }

                uiState.drafts.isEmpty() -> item(key = "empty") {
                    EmptyStateView(
                        icon = Icons.Rounded.MarkEmailRead,
                        title = "No Drafts to Review",
                        message = "Scan your inbox to draft expenses from BOC and ComBank messages, or load samples to see how it works."
                    )
                }

                else -> items(items = uiState.drafts, key = { it.id }) { draft ->
                    val suggested = remember(draft.id, accounts) {
                        draftViewModel.suggestAccountForDraft(draft)
                    }
                    DraftCard(
                        draft = draft,
                        accounts = accounts,
                        suggestedAccount = suggested,
                        currency = currentCurrency,
                        exchangeRate = exchangeRate,
                        onApprove = { catId, catName, selectedAcc ->
                            draftViewModel.approveDraft(
                                draft = draft,
                                selectedCategoryId = catId,
                                selectedCategoryName = catName,
                                selectedAccount = selectedAcc
                            )
                        },
                        onDiscard = { draftViewModel.discardDraft(draft.id) },
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                            .animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    draft: DraftTransaction,
    accounts: List<Account>,
    suggestedAccount: Account?,
    currency: String,
    exchangeRate: Double,
    onApprove: (selectedCategoryId: String, selectedCategoryName: String, selectedAccount: Account?) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val categories = remember { getDefaultCategories() }
    var selectedCategory by remember(draft.id) {
        mutableStateOf(categories.firstOrNull { it.id == draft.suggestedCategoryId } ?: categories.first())
    }
    var selectedAccount by remember(draft.id, suggestedAccount) {
        mutableStateOf(suggestedAccount)
    }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val isIncome = draft.type == DraftTransactionType.INCOME
    val formattedAmount = CurrencyFormatter.format(draft.amount, currency, exchangeRate)
    val timestamp = listOfNotNull(DisplayDates.short(draft.date), draft.time?.takeIf { it.isNotBlank() })
        .joinToString(" · ")

    ClarityCard(modifier = modifier) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.tertiaryFill)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = draft.sender,
                    style = ClarityType.caption1.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.label
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            LegendDot(color = if (isIncome) colors.income else colors.outflow)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isIncome) "Income" else "Expense",
                style = ClarityType.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.weight(1f)
            )
            Text(text = timestamp, style = ClarityType.footnote, color = colors.secondaryLabel)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.merchantOrTitle,
                    style = ClarityType.headline,
                    color = colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!draft.accountNumber.isNullOrBlank()) {
                    Text(
                        text = draft.accountNumber,
                        style = ClarityType.footnote,
                        color = colors.secondaryLabel
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isIncome) "+$formattedAmount" else formattedAmount,
                style = ClarityType.amountTitle.copy(fontSize = ClarityType.title3.fontSize),
                color = if (isIncome) colors.greenText else colors.label,
                maxLines = 1
            )
        }

        if (!isIncome || accounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isIncome) {
                    Box {
                        CategoryPickerButton(
                            category = selectedCategory,
                            onClick = { showCategoryMenu = true }
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(text = "${category.icon}  ${category.name}", style = ClarityType.body) },
                                    trailingIcon = if (category.id == selectedCategory.id) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = colors.accent
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (accounts.isNotEmpty()) {
                    Box {
                        AccountPickerButton(
                            account = selectedAccount,
                            onClick = { showAccountMenu = true }
                        )
                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = "No Account", style = ClarityType.body) },
                                trailingIcon = if (selectedAccount == null) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = colors.accent
                                        )
                                    }
                                } else null,
                                onClick = {
                                    selectedAccount = null
                                    showAccountMenu = false
                                }
                            )
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(text = "${account.type.icon}  ${account.name}", style = ClarityType.body) },
                                    trailingIcon = if (account.id == selectedAccount?.id) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = colors.accent
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        selectedAccount = account
                                        showAccountMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TintedButton(
                text = "Discard",
                onClick = onDiscard,
                tint = colors.secondaryLabel,
                minHeight = 44.dp,
                textStyle = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Approve",
                onClick = { onApprove(selectedCategory.id, selectedCategory.name, selectedAccount) },
                minHeight = 44.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AccountPickerButton(
    account: Account?,
    onClick: () -> Unit
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = Modifier
            .pressFade(onClickLabel = "Change account", onClick = onClick)
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(colors.tertiaryFill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = account?.type?.icon ?: "🏦", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = account?.name ?: "No Account",
            style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.UnfoldMore,
            contentDescription = null,
            tint = colors.secondaryLabel,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** iOS pop-up button: current value with an up/down chevron that opens a menu. */
@Composable
private fun CategoryPickerButton(
    category: CategoryItem,
    onClick: () -> Unit
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = Modifier
            .pressFade(onClickLabel = "Change category", onClick = onClick)
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(colors.tertiaryFill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = category.icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = category.name,
            style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.UnfoldMore,
            contentDescription = null,
            tint = colors.secondaryLabel,
            modifier = Modifier.size(16.dp)
        )
    }
}
