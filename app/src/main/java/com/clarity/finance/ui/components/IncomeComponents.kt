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
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSource
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
