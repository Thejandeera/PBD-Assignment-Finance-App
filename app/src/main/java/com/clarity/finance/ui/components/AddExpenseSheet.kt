package com.clarity.finance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

private val PaymentOptions = listOf(
    "Cash" to PaymentMethod.CASH,
    "Card" to PaymentMethod.CARD,
    "Digital" to PaymentMethod.SECONDARY
)

/**
 * Add-expense sheet (HIG > Sheets): Cancel / title / Add navigation row, a large Apple Cash
 * style amount, then category, obligation type and payment method. Quick mode trims the form
 * to the three essentials for sub-five-second logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToDrafts: (() -> Unit)? = null,
    pendingDraftCount: Int = 0
) {
    val colors = ClarityTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    val canSubmit = (formState.amountInput.toDoubleOrNull() ?: 0.0) > 0.0

    ClaritySheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        // The sheet is its own window, so focus and keyboard must be read from inside it.
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val dismissKeyboard = {
            focusManager.clearFocus()
            keyboardController?.hide()
        }

        // Navigation row stays fixed while the form scrolls.
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
                text = if (formState.isQuickMode) "Quick Log" else "New Expense",
                style = ClarityType.headline,
                color = colors.label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { heading() }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (formState.isSubmitting) {
                    ActivityIndicator(size = 20.dp, modifier = Modifier.padding(horizontal = 14.dp))
                } else {
                    TextLinkButton(
                        text = "Add",
                        enabled = canSubmit,
                        onClick = {
                            dismissKeyboard()
                            viewModel.submitExpense(onSuccess = { hideThen(onDismiss) })
                        },
                        style = ClarityType.body.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
                .animateContentSize()
        ) {
            SegmentedControl(
                options = listOf("Quick", "Detailed"),
                selectedIndex = if (formState.isQuickMode) 0 else 1,
                onSelect = { index -> viewModel.setQuickMode(index == 0) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // SMS drafts shortcut
            if (pendingDraftCount > 0 && onNavigateToDrafts != null) {
                Spacer(modifier = Modifier.height(12.dp))
                GroupedSection(containerColor = colors.elevatedCard) {
                    ListRow(
                        title = if (pendingDraftCount == 1) "1 SMS draft waiting" else "$pendingDraftCount SMS drafts waiting",
                        subtitle = "Review bank messages instead of typing",
                        leading = { IconTile(icon = Icons.Rounded.Sms, background = colors.indigo) },
                        showChevron = true,
                        onClick = { hideThen(onNavigateToDrafts) }
                    )
                }
            }

            // Amount
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LKR",
                    style = ClarityType.headline,
                    color = colors.secondaryLabel
                )
                BasicTextField(
                    value = formState.amountInput,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    singleLine = true,
                    textStyle = ClarityType.amountEntry.copy(
                        color = colors.label,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (formState.amountInput.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = ClarityType.amountEntry,
                                    color = colors.placeholder,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                AnimatedVisibility(visible = formState.amountError != null) {
                    Text(
                        text = formState.amountError.orEmpty(),
                        style = ClarityType.footnote,
                        color = colors.redText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAmountChip(label = "+500") { viewModel.onQuickAmountIncrement(500.0) }
                    QuickAmountChip(label = "+1,000") { viewModel.onQuickAmountIncrement(1000.0) }
                    QuickAmountChip(label = "+5,000") { viewModel.onQuickAmountIncrement(5000.0) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category
            if (formState.isQuickMode) {
                SectionHeaderLabel(text = "Category", modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.take(9).chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { category ->
                                CategoryTile(
                                    category = category,
                                    isSelected = formState.selectedCategory?.id == category.id,
                                    onClick = { viewModel.onCategorySelected(category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                SectionHeaderLabel(text = "Category", modifier = Modifier.padding(horizontal = 16.dp))
                val chipRowState = rememberLazyListState()
                // Bring the current selection into view, e.g. after switching from Quick mode.
                LaunchedEffect(Unit) {
                    val selectedIndex = categories.indexOfFirst { it.id == formState.selectedCategory?.id }
                    if (selectedIndex >= 2) chipRowState.scrollToItem(selectedIndex)
                }
                LazyRow(
                    state = chipRowState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "new-category") {
                        NewCategoryChip(onClick = { showAddCategoryDialog = true })
                    }
                    items(items = categories, key = { it.id }) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = formState.selectedCategory?.id == category.id,
                            onClick = { viewModel.onCategorySelected(category) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Obligation type
            SectionHeaderLabel(text = "Type", modifier = Modifier.padding(horizontal = 16.dp))
            SegmentedControl(
                options = listOf("Committed", "Discretionary"),
                selectedIndex = if (formState.spendingType == SpendingType.COMMITTED) 0 else 1,
                onSelect = { index ->
                    viewModel.onSpendingTypeSelected(
                        if (index == 0) SpendingType.COMMITTED else SpendingType.DISCRETIONARY
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SectionFooterLabel(
                text = if (formState.spendingType == SpendingType.COMMITTED) {
                    "Fixed obligations such as rent, utilities and subscriptions."
                } else {
                    "Flexible spending such as food, transport and shopping."
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Payment method
            SectionHeaderLabel(text = "Paid With", modifier = Modifier.padding(horizontal = 16.dp))
            val paymentIndex = when (formState.paymentMethod) {
                PaymentMethod.CASH -> 0
                PaymentMethod.CARD -> 1
                PaymentMethod.SECONDARY, PaymentMethod.ONLINE -> 2
            }
            SegmentedControl(
                options = PaymentOptions.map { it.first },
                selectedIndex = paymentIndex,
                onSelect = { index -> viewModel.onPaymentMethodSelected(PaymentOptions[index].second) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (formState.isQuickMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged for today",
                        style = ClarityType.footnote,
                        color = colors.secondaryLabel,
                        modifier = Modifier.weight(1f)
                    )
                    TextLinkButton(
                        text = "Add a Note",
                        onClick = { viewModel.setQuickMode(false) },
                        style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            } else {
                GroupedSection(containerColor = colors.elevatedCard, footer = formState.dateError) {
                    DetailRow(label = "Date", value = formState.formattedDateDisplay, showDivider = true)
                    NoteField(
                        value = formState.note,
                        onValueChange = { viewModel.onNoteChanged(it) }
                    )
                }
            }

            if (formState.errorMessage != null) {
                Text(
                    text = formState.errorMessage.orEmpty(),
                    style = ClarityType.footnote,
                    color = colors.redText,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 12.dp)
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var isCommittedCategory by remember { mutableStateOf(false) }

        ClarityAlertDialog(
            title = "New Category",
            message = "Name the category and choose how it's usually classified.",
            onDismissRequest = { showAddCategoryDialog = false },
            actions = listOf(
                AlertAction(
                    label = "Cancel",
                    style = AlertActionStyle.Cancel,
                    onClick = { showAddCategoryDialog = false }
                ),
                AlertAction(
                    label = "Add",
                    style = AlertActionStyle.Preferred,
                    enabled = newCategoryName.isNotBlank(),
                    onClick = {
                        viewModel.addCustomCategory(
                            name = newCategoryName,
                            defaultType = if (isCommittedCategory) SpendingType.COMMITTED else SpendingType.DISCRETIONARY,
                            onSuccess = { showAddCategoryDialog = false }
                        )
                    }
                )
            )
        ) {
            ClarityTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = "",
                placeholder = "e.g. Doctor, Books",
                containerColor = colors.card
            )
            Spacer(modifier = Modifier.height(10.dp))
            SegmentedControl(
                options = listOf("Discretionary", "Committed"),
                selectedIndex = if (isCommittedCategory) 1 else 0,
                onSelect = { index -> isCommittedCategory = index == 1 }
            )
        }
    }
}

@Composable
private fun QuickAmountChip(
    label: String,
    onClick: () -> Unit
) {
    val colors = ClarityTheme.colors
    Box(
        modifier = Modifier
            .pressScale(onClickLabel = "Add $label", onClick = onClick)
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(colors.tertiaryFill)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent
        )
    }
}

/** Quick-mode category tile: emoji over a short name; selection shown by tint, border and weight. */
@Composable
private fun CategoryTile(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Column(
        modifier = modifier
            .pressScale(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = isSelected }
            .heightIn(min = 72.dp)
            .clip(ClarityShapes.card)
            .background(if (isSelected) colors.accent.copy(alpha = if (colors.isDark) 0.24f else 0.12f) else colors.elevatedCard)
            .then(
                if (isSelected) Modifier.border(1.5.dp, colors.accent, ClarityShapes.card) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = category.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = ClarityType.caption1.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (isSelected) colors.accent else colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Detailed-mode category capsule. */
@Composable
private fun CategoryChip(
    category: CategoryItem,
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
        Text(text = category.icon, fontSize = 15.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = category.name,
            style = ClarityType.subheadline.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected) colors.onAccent else colors.label,
            maxLines = 1
        )
    }
}

/** Leading chip that creates a custom category. */
@Composable
private fun NewCategoryChip(onClick: () -> Unit) {
    val colors = ClarityTheme.colors
    Row(
        modifier = Modifier
            .pressScale(onClickLabel = "Create a new category", onClick = onClick)
            .heightIn(min = 38.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.12f))
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "New",
            style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent
        )
    }
}

/** Inline multi-line note field styled as a grouped-list row. */
@Composable
internal fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Note (optional), e.g. Lunch at Bambalapitiya"
) {
    val colors = ClarityTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = ClarityType.body.copy(color = colors.label),
        cursorBrush = SolidColor(colors.accent),
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = ClarityType.body,
                        color = colors.placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        }
    )
}

/** Shared "+" toolbar button that opens the add-expense sheet. */
@Composable
fun AddExpenseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CircleIconButton(
        icon = Icons.Rounded.Add,
        contentDescription = "Add expense",
        onClick = onClick,
        filled = true,
        modifier = modifier
    )
}
