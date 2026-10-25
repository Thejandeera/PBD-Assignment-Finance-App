package com.clarity.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clarity.finance.model.Expense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Human-friendly date labels for "YYYY-MM-DD" and "YYYY-MM" strings stored by the app. */
object DisplayDates {
    private fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()

    private fun pattern(value: String) = DateTimeFormatter.ofPattern(value, Locale.getDefault())

    /** "Today", "Yesterday", "22 Oct" or "22 Oct 2025" for other years. */
    fun short(date: String): String {
        val parsed = parse(date) ?: return date
        val today = LocalDate.now()
        return when {
            parsed == today -> "Today"
            parsed == today.minusDays(1) -> "Yesterday"
            parsed.year == today.year -> parsed.format(pattern("d MMM"))
            else -> parsed.format(pattern("d MMM yyyy"))
        }
    }

    /** List section titles: "Today", "Yesterday", "Wednesday, 22 October". */
    fun sectionTitle(date: String): String {
        val parsed = parse(date) ?: return date
        val today = LocalDate.now()
        return when {
            parsed == today -> "Today"
            parsed == today.minusDays(1) -> "Yesterday"
            parsed.year == today.year -> parsed.format(pattern("EEEE, d MMMM"))
            else -> parsed.format(pattern("d MMMM yyyy"))
        }
    }

    /** Full date for detail views, in the user's locale. */
    fun long(date: String): String {
        val parsed = parse(date) ?: return date
        return parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))
    }

    /** "2026-10" → "October 2026". */
    fun month(yearMonth: String): String =
        runCatching { YearMonth.parse(yearMonth).format(pattern("MMMM yyyy")) }.getOrDefault(yearMonth)

    /** Eyebrow date for the home header, e.g. "Sunday, 25 October". */
    fun todayEyebrow(): String = LocalDate.now().format(pattern("EEEE, d MMMM"))
}

/** Color that represents an expense's obligation type. */
@Composable
fun spendingTypeColor(type: SpendingType): Color =
    if (type == SpendingType.COMMITTED) ClarityTheme.colors.committed else ClarityTheme.colors.discretionary

/**
 * Transaction row in the Wallet style: category badge, category name, optional note,
 * a "date · type · method" line, and the amount in the primary label color.
 */
@Composable
fun ExpenseRow(
    expense: Expense,
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
        if (showDate) add(DisplayDates.short(expense.date))
        add(expense.type.displayName)
        add(expense.paymentMethod.displayName)
    }.joinToString(" · ")
    val note = expense.note?.takeIf { it.isNotBlank() }
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
        title = expense.categoryName,
        subtitle = note ?: meta,
        leading = { EmojiBadge(emoji = expense.categoryIcon, tint = spendingTypeColor(expense.type)) },
        trailing = {
            Text(
                text = CurrencyFormatter.format(expense.amount, currency, exchangeRate),
                style = ClarityType.amountBody,
                color = colors.label,
                maxLines = 1
            )
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

/**
 * Swipe-left-to-delete wrapper (UITableView trailing swipe). The row always springs back;
 * [onDeleteRequest] should present a confirmation so a stray swipe can't destroy data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDeleteRequest: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = ClarityTheme.colors
    val currentOnDeleteRequest by rememberUpdatedState(onDeleteRequest)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentOnDeleteRequest()
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(shape),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSwiping) colors.red else Color.Transparent)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Delete",
                        style = ClarityType.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    ) {
        content()
    }
}

/** Confirmation shown before an expense is permanently removed. */
@Composable
fun DeleteExpenseAlert(
    expense: Expense,
    currency: String,
    exchangeRate: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val amount = CurrencyFormatter.format(expense.amount, currency, exchangeRate)
    ClarityAlertDialog(
        title = "Delete This Expense?",
        message = "${expense.categoryName} · $amount will be permanently removed from your history.",
        onDismissRequest = onDismiss,
        actions = listOf(
            AlertAction(label = "Cancel", style = AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(label = "Delete", style = AlertActionStyle.Destructive, onClick = onConfirm)
        )
    )
}

/** Wallet-style transaction detail sheet with a destructive delete action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailSheet(
    expense: Expense,
    currency: String,
    exchangeRate: Double,
    onDismiss: () -> Unit,
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
                Text(
                    text = "Expense",
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
                EmojiBadge(emoji = expense.categoryIcon, tint = spendingTypeColor(expense.type), size = 64.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = CurrencyFormatter.format(expense.amount, currency, exchangeRate),
                    style = ClarityType.amountHero,
                    color = colors.label
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = expense.categoryName,
                    style = ClarityType.subheadline,
                    color = colors.secondaryLabel
                )
            }

            GroupedSection(containerColor = colors.elevatedCard) {
                DetailRow(label = "Date", value = DisplayDates.long(expense.date), showDivider = true)
                DetailRow(label = "Type", value = expense.type.displayName, showDivider = true)
                DetailRow(
                    label = "Paid With",
                    value = expense.paymentMethod.displayName,
                    showDivider = !expense.note.isNullOrBlank()
                )
                expense.note?.takeIf { it.isNotBlank() }?.let { note ->
                    DetailRow(label = "Note", value = note)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GroupedSection(containerColor = colors.elevatedCard) {
                ListRow(
                    title = "Delete Expense",
                    titleColor = colors.redText,
                    onClick = { showDeleteConfirmation = true }
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteExpenseAlert(
            expense = expense,
            currency = currency,
            exchangeRate = exchangeRate,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
                hideThen(onDismiss)
            }
        )
    }
}

/** Label/value row used in read-only detail lists. */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    val colors = ClarityTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(top = 12.dp, bottom = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = ClarityType.body, color = colors.label)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value,
                style = ClarityType.body,
                color = colors.secondaryLabel,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        if (showDivider) Hairline()
    }
}
