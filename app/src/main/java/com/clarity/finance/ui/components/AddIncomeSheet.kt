package com.clarity.finance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.IncomeViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private val CurrencyOptions = CurrencyType.entries

private const val SOURCES_PER_ROW = 4

/**
 * Add-income sheet (wireframe 12.2): amount first, then source (built-in or the user's own,
 * with "New" to create one), currency with an editable LKR rate, and — for invoiced work —
 * whether the payment is received or still pending. The common case (salary in LKR, received
 * today) needs only an amount and "Add".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeSheet(
    viewModel: IncomeViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val liveRate by viewModel.liveRate.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showNewSourceDialog by remember { mutableStateOf(false) }

    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    val dismiss = {
        viewModel.resetFormState()
        hideThen(onDismiss)
    }
    val canSubmit = (formState.amountInput.toDoubleOrNull() ?: 0.0) > 0.0
    val canBePending = formState.source.canBePending
    val isPending = formState.status == IncomeStatus.PENDING

    ClaritySheet(
        onDismissRequest = {
            viewModel.resetFormState()
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val dismissKeyboard = {
            focusManager.clearFocus()
            keyboardController?.hide()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp)
        ) {
            TextLinkButton(
                text = "Cancel",
                onClick = dismiss,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            )
            Text(
                text = "New Income",
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
                            viewModel.submitIncome(onSuccess = { hideThen(onDismiss) })
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
            // Amount, in the currency it was paid in
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formState.currency.name,
                    style = ClarityType.headline,
                    color = colors.secondaryLabel
                )
                BasicTextField(
                    value = formState.amountInput,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    singleLine = true,
                    textStyle = ClarityType.amountEntry.copy(
                        color = if (formState.isLoss) colors.redText else colors.label,
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

                val preview = formState.amountLkrPreview
                if (formState.isForeignCurrency && preview != null) {
                    Text(
                        text = "≈ " + CurrencyFormatter.formatOriginal(preview, "LKR"),
                        style = ClarityType.subheadline,
                        color = colors.secondaryLabel
                    )
                }

                AnimatedVisibility(visible = formState.amountError != null) {
                    Text(
                        text = formState.amountError.orEmpty(),
                        style = ClarityType.footnote,
                        color = colors.redText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Source
            SectionHeaderLabel(text = "Source", modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A null entry is the trailing "New" tile.
                (sources + null).chunked(SOURCES_PER_ROW).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { source ->
                            if (source == null) {
                                NewSourceTile(
                                    onClick = {
                                        dismissKeyboard()
                                        showNewSourceDialog = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                SourceTile(
                                    source = source,
                                    isSelected = formState.source.id == source.id,
                                    onClick = { viewModel.onSourceSelected(source) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        repeat(SOURCES_PER_ROW - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            // Crypto is a net position for the period, so it can be a gain or a loss.
            if (formState.source.allowsLoss) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeaderLabel(text = "Net Position", modifier = Modifier.padding(horizontal = 16.dp))
                SegmentedControl(
                    options = listOf("Gain", "Loss"),
                    selectedIndex = if (formState.isLoss) 1 else 0,
                    onSelect = { index -> viewModel.onLossSelected(index == 1) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SectionFooterLabel(
                    text = "Record your realised crypto result after trading gains and losses.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Freelance and custom sources can be logged as invoices before the client pays.
            if (canBePending) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeaderLabel(text = "Payment", modifier = Modifier.padding(horizontal = 16.dp))
                SegmentedControl(
                    options = listOf("Received", "Pending"),
                    selectedIndex = if (isPending) 1 else 0,
                    onSelect = { index ->
                        viewModel.onStatusSelected(if (index == 1) IncomeStatus.PENDING else IncomeStatus.RECEIVED)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SectionFooterLabel(
                    text = if (isPending) {
                        "Pending payments aren't counted as income until you mark them received."
                    } else {
                        "The client has paid and the money is in your account."
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ClarityTextField(
                    value = formState.projectName,
                    onValueChange = { viewModel.onProjectNameChanged(it) },
                    label = if (isPending) "Client or Project" else "Client or Project (optional)",
                    placeholder = "e.g. Upwork – Logo redesign",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    isError = formState.projectError != null,
                    errorMessage = formState.projectError,
                    containerColor = colors.elevatedCard,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Currency and rate snapshot
            SectionHeaderLabel(text = "Currency", modifier = Modifier.padding(horizontal = 16.dp))
            SegmentedControl(
                options = CurrencyOptions.map { it.name },
                selectedIndex = CurrencyOptions.indexOf(formState.currency),
                onSelect = { index -> viewModel.onCurrencySelected(CurrencyOptions[index]) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (formState.isForeignCurrency) {
                Spacer(modifier = Modifier.height(12.dp))
                ClarityTextField(
                    value = formState.rateInput,
                    onValueChange = { viewModel.onRateChanged(it) },
                    label = "LKR for 1 ${formState.currency.name}",
                    placeholder = "e.g. 300.00",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    isError = formState.rateError != null,
                    errorMessage = formState.rateError,
                    containerColor = colors.elevatedCard,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SectionFooterLabel(
                    text = liveRate?.let {
                        "Today's rate is ${String.format(Locale.US, "%,.2f", it)} LKR per USD. " +
                            "Change it to the rate you were actually paid; it's saved with this entry."
                    } ?: "Enter the rate you were paid at; it's saved with this entry.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GroupedSection(containerColor = colors.elevatedCard, footer = formState.dateError) {
                ListRow(
                    title = if (canBePending && isPending) "Invoice Date" else "Date Received",
                    trailing = {
                        Text(
                            text = DisplayDates.short(formState.date),
                            style = ClarityType.body,
                            color = colors.secondaryLabel
                        )
                    },
                    showChevron = true,
                    showDivider = true,
                    onClick = {
                        dismissKeyboard()
                        showDatePicker = true
                    },
                    onClickLabel = "Change date"
                )
                NoteField(
                    value = formState.note,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    placeholder = "Note (optional), e.g. October bonus"
                )
            }

            if (formState.errorMessage != null) {
                InlineErrorMessage(
                    message = formState.errorMessage.orEmpty(),
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 12.dp)
                )
            }
        }
    }

    if (showNewSourceDialog) {
        NewIncomeSourceDialog(
            validate = viewModel::validateNewSourceName,
            onCreate = { name, currency -> viewModel.addCustomSource(name, currency) },
            onDismiss = { showNewSourceDialog = false }
        )
    }

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = formState.date,
            onDateSelected = { viewModel.onDateSelected(it) },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Creates a custom income source: a name and the currency it usually pays in.
 * [onCreate] returns a validation message, or null once the source is created.
 */
@Composable
private fun NewIncomeSourceDialog(
    validate: (String) -> String?,
    onCreate: (String, CurrencyType) -> String?,
    onDismiss: () -> Unit
) {
    val colors = ClarityTheme.colors
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyType.LKR) }
    var submitError by remember { mutableStateOf<String?>(null) }
    // Only flag a problem once something is typed, e.g. a duplicate name.
    val liveError = if (name.isBlank()) null else validate(name)
    val error = submitError ?: liveError

    ClarityAlertDialog(
        title = "New Income Source",
        message = "Name the source and choose the currency it usually pays in.",
        onDismissRequest = onDismiss,
        actions = listOf(
            AlertAction(label = "Cancel", style = AlertActionStyle.Cancel, onClick = onDismiss),
            AlertAction(
                label = "Add",
                style = AlertActionStyle.Preferred,
                enabled = name.isNotBlank() && liveError == null,
                onClick = {
                    val result = onCreate(name, currency)
                    if (result == null) onDismiss() else submitError = result
                }
            )
        )
    ) {
        ClarityTextField(
            value = name,
            onValueChange = {
                name = it
                submitError = null
            },
            label = "",
            placeholder = "e.g. Tutoring, Rental",
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            isError = error != null,
            errorMessage = error,
            containerColor = colors.card
        )
        Spacer(modifier = Modifier.height(10.dp))
        SegmentedControl(
            options = CurrencyOptions.map { it.name },
            selectedIndex = CurrencyOptions.indexOf(currency),
            onSelect = { index -> currency = CurrencyOptions[index] }
        )
    }
}

/** Trailing tile that opens [NewIncomeSourceDialog]. */
@Composable
private fun NewSourceTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Column(
        modifier = modifier
            .pressScale(onClickLabel = "Create a new income source", onClick = onClick)
            .heightIn(min = 72.dp)
            .clip(ClarityShapes.card)
            .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.12f))
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "New",
            style = ClarityType.caption1.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent,
            maxLines = 1
        )
    }
}

/** Source tile: emoji over a short name; selection shown by tint, border and weight. */
@Composable
private fun SourceTile(
    source: IncomeSourceItem,
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
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = source.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = source.name,
            style = ClarityType.caption1.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (isSelected) colors.accent else colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
