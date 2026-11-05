package com.clarity.finance.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSource
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.RecurringIncome
import com.clarity.finance.model.toItem
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Color that identifies an income source in badges, bars and legends. Built-in sources have
 * fixed colors; custom sources get a stable color derived from their id.
 */
@Composable
fun incomeSourceColor(sourceId: String): Color {
    val colors = ClarityTheme.colors
    return when (IncomeSource.fromId(sourceId)) {
        IncomeSource.SALARY -> colors.green
        IncomeSource.FREELANCE -> colors.indigo
        IncomeSource.ADSENSE -> colors.teal
        IncomeSource.CRYPTO -> colors.orange
        null -> {
            val palette = listOf(colors.pink, colors.blue, colors.purple, colors.yellow)
            palette[Math.floorMod(sourceId.hashCode(), palette.size)]
        }
    }
}

/** "Outstanding for 23 days" style age of a pending invoice. */
fun pendingAgeLabel(invoiceDate: String): String {
    val date = runCatching { LocalDate.parse(invoiceDate) }.getOrNull() ?: return "Pending"
    val days = ChronoUnit.DAYS.between(date, LocalDate.now())
    return when {
        days <= 0L -> "Invoiced today"
        days == 1L -> "Waiting 1 day"
        else -> "Waiting $days days"
    }
}

/**
 * Income row: source badge, client/source title, a "source · date" line and the amount in the
 * currency it was paid in, with its converted value underneath when that differs.
 */
@Composable
fun IncomeRow(
    income: Income,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDeleteRequest: (() -> Unit)? = null
) {
    val colors = ClarityTheme.colors
    val meta = buildList {
        if (income.projectName.isNotBlank()) add(income.sourceName)
        if (income.isPending) {
            add(pendingAgeLabel(income.date))
        } else if (showDate) {
            add(DisplayDates.short(income.date))
        }
    }.joinToString(" · ").ifEmpty { income.status.displayName }
    val note = income.note.takeIf { it.isNotBlank() }
    val original = CurrencyFormatter.formatOriginal(income.amount, income.currency.name)
    val converted = if (!income.currency.name.equals(currency, ignoreCase = true)) {
        "≈ " + CurrencyFormatter.format(income.amountLKR, currency, exchangeRate)
    } else {
        null
    }
    val accessibilityModifier = if (onDeleteRequest != null) {
        Modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Delete") {
                    onDeleteRequest()
                    true
                }
            )
        }
    } else {
        Modifier
    }

    ListRow(
        title = income.title,
        subtitle = note ?: meta,
        leading = { EmojiBadge(emoji = income.sourceIcon, tint = incomeSourceColor(income.sourceId)) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (income.amount > 0 && !income.isPending) "+$original" else original,
                    style = ClarityType.amountBody,
                    color = if (income.amount > 0 && !income.isPending) colors.greenText else colors.label,
                    maxLines = 1
                )
                if (converted != null) {
                    Text(
                        text = converted,
                        style = ClarityType.footnote,
                        color = colors.secondaryLabel,
                        maxLines = 1
                    )
                }
            }
        },
        below = if (note != null) {
            {
                Text(
                    text = meta,
                    style = ClarityType.footnote,
                    color = colors.secondaryLabel,
                    maxLines = 1
                )
            }
        } else {
            null
        },
        showDivider = showDivider,
        onClick = onClick,
        onClickLabel = "View details",
        modifier = modifier.then(accessibilityModifier)
    )
}

/** Confirmation shown before an income entry is permanently removed. */
@Composable
fun DeleteIncomeAlert(
    income: Income,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val amount = CurrencyFormatter.formatOriginal(income.amount, income.currency.name)
    ClarityAlertDialog(
        title = "Delete This Income?",
        message = "${income.title} · $amount will be permanently removed.",
        onDismissRequest = onDismiss,
        actions = listOf(
            AlertAction(label = "Cancel", style = AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(label = "Delete", style = AlertActionStyle.Destructive, onClick = onConfirm)
        )
    )
}

/**
 * Income detail sheet: original amount, LKR equivalent and the stored rate snapshot, with Edit
 * in the navigation row. Pending payments get a "Mark as Received" action for when they're paid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDetailSheet(
    income: Income,
    currency: String,
    exchangeRate: Double,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkReceived: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = ClarityTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ClaritySheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
            ) {
                TextLinkButton(
                    text = "Edit",
                    onClick = { hideThen(onEdit) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                )
                Text(
                    text = "Income",
                    style = ClarityType.headline,
                    color = colors.label,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics { heading() }
                )
                TextLinkButton(
                    text = "Done",
                    onClick = { hideThen(onDismiss) },
                    style = ClarityType.body.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmojiBadge(emoji = income.sourceIcon, tint = incomeSourceColor(income.sourceId), size = 64.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = CurrencyFormatter.formatOriginal(income.amount, income.currency.name),
                    style = ClarityType.amountHero,
                    color = colors.label
                )
                if (income.isForeignCurrency) {
                    Text(
                        text = "≈ " + CurrencyFormatter.formatOriginal(income.amountLKR, "LKR"),
                        style = ClarityType.subheadline,
                        color = colors.secondaryLabel
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = income.title,
                    style = ClarityType.subheadline,
                    color = colors.secondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            GroupedSection(containerColor = colors.elevatedCard) {
                DetailRow(label = "Source", value = income.sourceName, showDivider = true)
                DetailRow(
                    label = "Status",
                    value = if (income.isPending) "Pending · ${pendingAgeLabel(income.date)}" else income.status.displayName,
                    showDivider = true
                )
                DetailRow(
                    label = if (income.isPending) "Invoice Date" else "Received",
                    value = DisplayDates.long(income.date),
                    showDivider = income.isForeignCurrency || income.projectName.isNotBlank() || income.note.isNotBlank()
                )
                if (income.isForeignCurrency) {
                    DetailRow(
                        label = "Rate",
                        value = "1 ${income.currency.name} = ${String.format(Locale.US, "%,.2f", income.exchangeRate)} LKR",
                        showDivider = true
                    )
                    DetailRow(
                        label = "In ${currency.uppercase(Locale.US)}",
                        value = CurrencyFormatter.format(income.amountLKR, currency, exchangeRate),
                        showDivider = income.projectName.isNotBlank() || income.note.isNotBlank()
                    )
                }
                if (income.projectName.isNotBlank()) {
                    DetailRow(label = "Client", value = income.projectName, showDivider = income.note.isNotBlank())
                }
                if (income.note.isNotBlank()) {
                    DetailRow(label = "Note", value = income.note)
                }
            }

            if (income.isPending) {
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Mark as Received",
                    leadingIcon = Icons.Rounded.CheckCircle,
                    onClick = {
                        onMarkReceived()
                        hideThen(onDismiss)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SectionFooterLabel(
                    text = "It will count as income received today.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GroupedSection(containerColor = colors.elevatedCard) {
                ListRow(
                    title = "Delete Income",
                    titleColor = colors.redText,
                    onClick = { showDeleteConfirmation = true }
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteIncomeAlert(
            income = income,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
                hideThen(onDismiss)
            }
        )
    }
}

/** Calendar date picker limited to today and earlier. Dates are exchanged as LocalDate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(
    initialDate: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ClarityTheme.colors
    val today = LocalDate.now()
    val todayMillis = today.toUtcMillis()
    val initial = runCatching { LocalDate.parse(initialDate) }.getOrDefault(today)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    )
    val pickerColors = DatePickerDefaults.colors(containerColor = colors.alertBackground)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        dismissButton = { TextLinkButton(text = "Cancel", onClick = onDismiss) },
        confirmButton = {
            TextLinkButton(
                text = "Done",
                enabled = pickerState.selectedDateMillis != null,
                style = ClarityType.body.copy(fontWeight = FontWeight.SemiBold),
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                }
            )
        }
    ) {
        DatePicker(state = pickerState, colors = pickerColors)
    }
}

// The Material date picker works in UTC midnight milliseconds.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** "+" toolbar button that opens the add-income sheet. */
@Composable
fun AddIncomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CircleIconButton(
        icon = Icons.Rounded.Add,
        contentDescription = "Add income",
        onClick = onClick,
        filled = true,
        modifier = modifier
    )
}

/** Row for a recurring monthly income entry. */
@Composable
fun RecurringIncomeRow(
    recurring: RecurringIncome,
    currentMonth: String,
    monthTitle: String,
    currency: String,
    exchangeRate: Double,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val isCommittedThisMonth = recurring.lastCommittedMonth == currentMonth
    val formattedAmount = CurrencyFormatter.format(recurring.amount, currency, exchangeRate)

    ListRow(
        title = recurring.name,
        subtitle = "Every month on day ${recurring.dayOfMonth}",
        leading = { EmojiBadge(emoji = recurring.sourceIcon, tint = incomeSourceColor(recurring.sourceId)) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+$formattedAmount",
                    style = ClarityType.amountBody,
                    color = colors.greenText,
                    maxLines = 1
                )
                Text(
                    text = if (isCommittedThisMonth) "✓ Added for $monthTitle" else "Day ${recurring.dayOfMonth}",
                    style = ClarityType.footnote,
                    color = if (isCommittedThisMonth) colors.greenText else colors.secondaryLabel,
                    maxLines = 1
                )
            }
        },
        showDivider = showDivider,
        onClick = onClick,
        onClickLabel = "Edit recurring income",
        modifier = modifier
    )
}

/** Confirmation shown before a recurring income schedule is removed. */
@Composable
fun DeleteRecurringAlert(
    recurring: RecurringIncome,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ClarityAlertDialog(
        title = "Delete Recurring Income?",
        message = "Stop auto-adding \"${recurring.name}\"? Past committed incomes will not be removed.",
        onDismissRequest = onDismiss,
        actions = listOf(
            AlertAction(label = "Cancel", style = AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(label = "Delete", style = AlertActionStyle.Destructive, onClick = onConfirm)
        )
    )
}

/** Sheet to add or edit a recurring income. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringIncomeSheet(
    recurring: RecurringIncome?,
    sources: List<IncomeSourceItem>,
    onSave: (name: String, amount: Double, sourceId: String, sourceName: String, sourceIcon: String, dayOfMonth: Int, currency: CurrencyType) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember(recurring) { mutableStateOf(recurring?.name.orEmpty()) }
    var amountText by remember(recurring) {
        mutableStateOf(
            if (recurring != null) {
                if (recurring.amount % 1.0 == 0.0) recurring.amount.toLong().toString() else recurring.amount.toString()
            } else ""
        )
    }
    var dayText by remember(recurring) { mutableStateOf(recurring?.dayOfMonth?.toString() ?: "1") }
    var selectedSource by remember(recurring, sources) {
        mutableStateOf(
            sources.find { it.id == recurring?.sourceId }
                ?: sources.firstOrNull()
                ?: IncomeSource.SALARY.toItem()
        )
    }

    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val day = dayText.toIntOrNull() ?: 0
    val canSubmit = name.isNotBlank() && amount > 0.0 && day in 1..31

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
                text = if (recurring == null) "New Recurring Income" else "Edit Recurring Income",
                style = ClarityType.headline,
                color = colors.label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { heading() }
            )
            TextLinkButton(
                text = if (recurring == null) "Add" else "Save",
                enabled = canSubmit,
                onClick = {
                    onSave(
                        name.trim(),
                        amount,
                        selectedSource.id,
                        selectedSource.name,
                        selectedSource.icon,
                        day,
                        recurring?.currency ?: CurrencyType.LKR
                    )
                    hideThen(onDismiss)
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ClarityTextField(
                value = name,
                onValueChange = { name = it },
                label = "Income Name",
                placeholder = "e.g. Monthly Salary, Retainer"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClarityTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = "Amount",
                placeholder = "0.00",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClarityTextField(
                value = dayText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(2)
                    dayText = filtered
                },
                label = "Day of Month (1 - 31)",
                placeholder = "e.g. 7",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = dayText.isNotEmpty() && (day !in 1..31),
                errorMessage = if (dayText.isNotEmpty() && day !in 1..31) "Day must be between 1 and 31" else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Source",
                style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.secondaryLabel
            )
            Spacer(modifier = Modifier.height(8.dp))

            sources.chunked(4).forEach { rowSources ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSources.forEach { source ->
                        SourceTile(
                            source = source,
                            isSelected = selectedSource.id == source.id,
                            onClick = { selectedSource = source },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - rowSources.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Every month on day ${if (day in 1..31) day else "X"}, this income will automatically be recorded as received.",
                style = ClarityType.footnote,
                color = colors.secondaryLabel
            )

            if (onDelete != null) {
                Spacer(modifier = Modifier.height(24.dp))
                TextLinkButton(
                    text = "Delete Recurring Income",
                    color = colors.redText,
                    onClick = { hideThen(onDelete) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

