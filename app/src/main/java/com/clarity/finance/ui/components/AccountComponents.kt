package com.clarity.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clarity.finance.model.ACCOUNT_CURRENCIES
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountType
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import kotlinx.coroutines.launch

@Composable
fun accountTypeColor(type: AccountType): Color {
    val colors = ClarityTheme.colors
    return when (type) {
        AccountType.BANK -> colors.blue
        AccountType.CASH -> colors.green
        AccountType.CRYPTO -> colors.orange
        AccountType.SAVINGS -> colors.purple
        AccountType.INVESTMENTS -> colors.teal
        AccountType.OTHERS -> colors.gray
    }
}

/**
 * Reusable horizontal account chip selector for Expense, Income, and Recurring forms.
 */
@Composable
fun AccountPickerSection(
    accounts: List<Account>,
    selectedAccount: Account?,
    onSelectAccount: (Account?) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Account",
    primaryAccountId: String? = null,
    onManageAccountsClick: (() -> Unit)? = null
) {
    val orderedAccounts = remember(accounts, primaryAccountId) {
        if (primaryAccountId == null) accounts
        else accounts.sortedByDescending { it.id == primaryAccountId }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeaderLabel(text = title, modifier = Modifier.weight(1f))
            if (onManageAccountsClick != null) {
                TextLinkButton(
                    text = if (accounts.isEmpty()) "+ Create" else "Manage",
                    onClick = onManageAccountsClick,
                    style = ClarityType.footnote.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        if (accounts.isEmpty()) {
            SectionFooterLabel(
                text = "No accounts yet. Create one in Accounts to track balances automatically.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "none") {
                AccountPickerChip(
                    label = "No Account",
                    icon = "—",
                    isSelected = selectedAccount == null,
                    onClick = { onSelectAccount(null) }
                )
            }
            items(items = orderedAccounts, key = { it.id }) { account ->
                val balance = CurrencyFormatter.formatOriginal(account.currentBalance, account.currency.name)
                val prefix = if (account.id == primaryAccountId) "★ " else ""
                AccountPickerChip(
                    label = "$prefix${account.name} ($balance)",
                    icon = account.type.icon,
                    isSelected = selectedAccount?.id == account.id,
                    onClick = { onSelectAccount(account) }
                )
            }
        }
    }
}

@Composable
private fun AccountPickerChip(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = Modifier
            .pressScale(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = isSelected }
            .heightIn(min = 38.dp)
            .clip(CircleShape)
            .background(if (isSelected) colors.accentFill else colors.elevatedCard)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = ClarityType.subheadline.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) colors.onAccent else colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Reusable fields for creating or editing an Account (shared by RegisterScreen and AddEditAccountSheet).
 */
@Composable
fun AccountFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    type: AccountType,
    onTypeChange: (AccountType) -> Unit,
    currency: CurrencyType,
    onCurrencyChange: (CurrencyType) -> Unit,
    balanceInput: String,
    onBalanceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    nameError: String? = null,
    balanceError: String? = null,
    fieldContainerColor: Color = ClarityTheme.colors.elevatedCard
) {
    val colors = ClarityTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        ClarityTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Account Name",
            placeholder = "e.g. Commercial Bank",
            containerColor = fieldContainerColor,
            isError = nameError != null,
            errorMessage = nameError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeaderLabel(text = "Account Type")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.chunked(3).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTypes.forEach { item ->
                        val isSelected = type == item
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .pressScale(role = Role.RadioButton) { onTypeChange(item) }
                                .semantics { selected = isSelected }
                                .heightIn(min = 68.dp)
                                .clip(ClarityShapes.card)
                                .background(
                                    if (isSelected) {
                                        colors.accent.copy(alpha = if (colors.isDark) 0.24f else 0.12f)
                                    } else {
                                        fieldContainerColor
                                    }
                                )
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, colors.accent, ClarityShapes.card)
                                    else Modifier
                                )
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = item.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.displayName,
                                style = ClarityType.caption1.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) colors.accent else colors.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    repeat(3 - rowTypes.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeaderLabel(text = "Currency")
        SegmentedControl(
            options = ACCOUNT_CURRENCIES.map { it.name },
            selectedIndex = ACCOUNT_CURRENCIES.indexOf(currency).coerceAtLeast(0),
            onSelect = { index -> onCurrencyChange(ACCOUNT_CURRENCIES[index]) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ClarityTextField(
            value = balanceInput,
            onValueChange = { raw ->
                val filtered = raw.filterIndexed { i, ch ->
                    ch.isDigit() || ch == '.' || (ch == '-' && i == 0)
                }
                onBalanceChange(filtered)
            },
            label = "Current Balance (${currency.name})",
            placeholder = "e.g. 100000",
            containerColor = fieldContainerColor,
            isError = balanceError != null,
            errorMessage = balanceError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountSheet(
    account: Account?,
    initialIsPrimary: Boolean = false,
    onSave: (name: String, type: AccountType, currency: CurrencyType, balanceInput: String, isPrimary: Boolean) -> Boolean,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember(account) { mutableStateOf(account?.name.orEmpty()) }
    var type by remember(account) { mutableStateOf(account?.type ?: AccountType.BANK) }
    var currency by remember(account) {
        mutableStateOf(
            if (account?.currency == CurrencyType.USD) CurrencyType.USD else CurrencyType.LKR
        )
    }
    var balanceInput by remember(account) {
        mutableStateOf(
            account?.let {
                if (it.currentBalance % 1.0 == 0.0) it.currentBalance.toLong().toString()
                else it.currentBalance.toString()
            } ?: ""
        )
    }
    var isPrimary by remember(account, initialIsPrimary) { mutableStateOf(initialIsPrimary) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var balanceError by remember { mutableStateOf<String?>(null) }
    var showDeleteAlert by remember { mutableStateOf(false) }

    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ClaritySheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp)
        ) {
            TextLinkButton(
                text = "Cancel",
                onClick = { hideThen(onDismiss) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            )
            Text(
                text = if (account == null) "New Account" else "Edit Account",
                style = ClarityType.headline,
                color = colors.label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { heading() }
            )
            TextLinkButton(
                text = if (account == null) "Add" else "Save",
                onClick = {
                    val trimmedName = name.trim()
                    val parsed = balanceInput.trim().toDoubleOrNull()
                    nameError = if (trimmedName.isEmpty()) "Enter an account name" else null
                    balanceError = if (parsed == null || !parsed.isFinite()) "Enter a valid balance" else null
                    if (nameError == null && balanceError == null) {
                        if (onSave(trimmedName, type, currency, balanceInput, isPrimary)) {
                            hideThen(onDismiss)
                        }
                    }
                },
                style = ClarityType.body.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AccountFormFields(
                name = name,
                onNameChange = {
                    name = it
                    nameError = null
                },
                type = type,
                onTypeChange = { type = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                balanceInput = balanceInput,
                onBalanceChange = {
                    balanceInput = it
                    balanceError = null
                },
                nameError = nameError,
                balanceError = balanceError
            )

            Spacer(modifier = Modifier.height(20.dp))

            GroupedSection(containerColor = colors.elevatedCard) {
                SwitchRow(
                    title = "Primary Account",
                    subtitle = "Default selection for new expenses & incomes",
                    checked = isPrimary,
                    onCheckedChange = { isPrimary = it }
                )
            }

            if (account != null && onDelete != null) {
                Spacer(modifier = Modifier.height(24.dp))
                GroupedSection(containerColor = colors.elevatedCard) {
                    ListRow(
                        title = "Delete Account",
                        titleColor = colors.redText,
                        onClick = { showDeleteAlert = true }
                    )
                }
                SectionFooterLabel(
                    text = "Existing expenses and incomes remain in your history.",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showDeleteAlert && account != null && onDelete != null) {
        DeleteAccountAlert(
            account = account,
            onConfirm = {
                showDeleteAlert = false
                onDelete()
                hideThen(onDismiss)
            },
            onDismiss = { showDeleteAlert = false }
        )
    }
}

@Composable
fun DeleteAccountAlert(
    account: Account,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ClarityAlertDialog(
        title = "Delete ${account.name}?",
        message = "This account will be removed from your Accounts list. Past expenses and incomes stay in your history.",
        onDismissRequest = onDismiss,
        actions = listOf(
            AlertAction(label = "Cancel", style = AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(label = "Delete", style = AlertActionStyle.Destructive, onClick = onConfirm)
        )
    )
}
