package com.clarity.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityBorder
import com.clarity.finance.ui.theme.ClarityBorderDark
import com.clarity.finance.ui.theme.ClarityGrayBackground
import com.clarity.finance.ui.theme.ClarityRed
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityTextSecondary
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ClarityWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // =================================================================
            // Top Bar: Mode Toggle & Cancel (Wireframes 1 & 2)
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (formState.isQuickMode) "QUICK LOG" else "NEW EXPENSE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(4.dp))
                            .clickable { viewModel.setQuickMode(!formState.isQuickMode) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (formState.isQuickMode) "Full Form" else "Quick Form",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = ClarityTextSecondary
                        )
                    }
                }

                Text(
                    text = "✕ Cancel",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = ClarityTextSecondary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // Outflow Amount Box with Quick Increments (Wireframe 1 & 2)
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.5.dp, if (formState.amountError != null) ClarityRed else ClarityBlack), RoundedCornerShape(8.dp))
                    .padding(vertical = 14.dp, horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OUTFLOW AMOUNT (LKR)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "LKR ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextPrimary
                        )
                        BasicTextField(
                            value = formState.amountInput,
                            onValueChange = { viewModel.onAmountChanged(it) },
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ClarityTextPrimary,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(ClarityBlack),
                            decorationBox = { innerTextField ->
                                if (formState.amountInput.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = ClarityTextMuted
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Increment Chips [+500], [+1,000], [+5,000]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickIncrementChip(label = "+500") { viewModel.onQuickAmountIncrement(500.0) }
                        QuickIncrementChip(label = "+1,000") { viewModel.onQuickAmountIncrement(1000.0) }
                        QuickIncrementChip(label = "+5,000") { viewModel.onQuickAmountIncrement(5000.0) }
                    }
                }
            }

            if (formState.amountError != null) {
                Text(
                    text = formState.amountError.orEmpty(),
                    fontSize = 11.sp,
                    color = ClarityRed,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // Category Selection (Standard Chips vs Quick 3x3 Grid)
            // =================================================================
            if (formState.isQuickMode) {
                // Wireframe 2: 3x3 Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. TAP CATEGORY",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextSecondary
                    )
                    Text(
                        text = "9 Core Buckets",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display in 3 columns
                val gridCategories = categories.take(9)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in gridCategories.chunked(3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (cat in row) {
                                val isSelected = formState.selectedCategory?.id == cat.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) ClarityBlack else ClarityWhite)
                                        .border(BorderStroke(1.dp, if (isSelected) ClarityBlack else ClarityBorder), RoundedCornerShape(6.dp))
                                        .clickable { viewModel.onCategorySelected(cat) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ClarityWhite else ClarityTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Wireframe 1: Horizontal Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORY CLASSIFICATION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextSecondary
                    )
                    Text(
                        text = "+ Custom",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityBlack,
                        modifier = Modifier.clickable { showAddCategoryDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cat in categories) {
                        val isSelected = formState.selectedCategory?.id == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ClarityBlack else ClarityWhite)
                                .border(BorderStroke(1.dp, if (isSelected) ClarityBlack else ClarityBorder), RoundedCornerShape(6.dp))
                                .clickable { viewModel.onCategorySelected(cat) }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = if (isSelected) "${cat.name} •" else cat.name,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ClarityWhite else ClarityTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // Obligation Type: Committed vs Discretionary Cards
            // =================================================================
            Text(
                text = if (formState.isQuickMode) "2. NATURE" else "OBLIGATION TYPE (CRUCIAL METRIC)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isCommitted = formState.spendingType == SpendingType.COMMITTED
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ClarityWhite)
                        .border(BorderStroke(if (isCommitted) 2.dp else 1.dp, if (isCommitted) ClarityBlack else ClarityBorder), RoundedCornerShape(8.dp))
                        .clickable { viewModel.onSpendingTypeSelected(SpendingType.COMMITTED) }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isCommitted) "• Committed" else "Committed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fixed Obligation",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextSecondary
                        )
                    }
                }

                val isDiscretionary = formState.spendingType == SpendingType.DISCRETIONARY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ClarityWhite)
                        .border(BorderStroke(if (isDiscretionary) 2.dp else 1.dp, if (isDiscretionary) ClarityBlack else ClarityBorder), RoundedCornerShape(8.dp))
                        .clickable { viewModel.onSpendingTypeSelected(SpendingType.DISCRETIONARY) }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isDiscretionary) "• Discretionary" else "Discretionary",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Flexible / Leisure",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // Payment Method / Channel: Cash, Card, Online
            // =================================================================
            Text(
                text = if (formState.isQuickMode) "3. CHANNEL" else "PAYMENT METHOD",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodTab(
                    label = "Cash",
                    isSelected = formState.paymentMethod == PaymentMethod.CASH,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.onPaymentMethodSelected(PaymentMethod.CASH)
                }

                PaymentMethodTab(
                    label = "Card",
                    isSelected = formState.paymentMethod == PaymentMethod.CARD,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.onPaymentMethodSelected(PaymentMethod.CARD)
                }

                PaymentMethodTab(
                    label = if (formState.isQuickMode) "Digital" else "Online",
                    isSelected = formState.paymentMethod == PaymentMethod.SECONDARY,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.onPaymentMethodSelected(PaymentMethod.SECONDARY)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================================
            // Date & Optional Note (Wireframe 1)
            // =================================================================
            if (!formState.isQuickMode) {
                // Date Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date:",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextSecondary
                        )
                        Text(
                            text = formState.formattedDateDisplay,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Note
                Text(
                    text = "OPTIONAL NOTE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = formState.note,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    placeholder = {
                        Text(
                            text = "e.g. Lunch at Bambalapitiya",
                            color = ClarityTextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClarityBlack,
                        unfocusedBorderColor = ClarityBorder,
                        focusedTextColor = ClarityTextPrimary,
                        unfocusedTextColor = ClarityTextPrimary
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date: Today (Auto)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextSecondary
                    )
                    Text(
                        text = "▸ More details",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityBlack,
                        modifier = Modifier.clickable { viewModel.setQuickMode(false) }
                    )
                }
            }

            if (formState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formState.errorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = ClarityRed,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================================
            // Primary Submission Button
            // =================================================================
            Button(
                onClick = {
                    viewModel.submitExpense(onSuccess = onDismiss)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !formState.isSubmitting,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClarityBlack,
                    contentColor = ClarityWhite,
                    disabledContainerColor = ClarityBorderDark
                )
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ClarityWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (formState.isQuickMode) "LOG IN < 5 SECONDS" else "SAVE EXPENSE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // =========================================================================
    // Add Custom Category Dialog
    // =========================================================================
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var isCommittedCategory by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = "Add Custom Category",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        placeholder = { Text("e.g. Doctor, Books", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isCommittedCategory) "Type: Committed" else "Type: Discretionary",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { isCommittedCategory = !isCommittedCategory }) {
                            Text(text = "Toggle", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(
                                name = newCategoryName,
                                defaultType = if (isCommittedCategory) SpendingType.COMMITTED else SpendingType.DISCRETIONARY,
                                onSuccess = { showAddCategoryDialog = false }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClarityBlack)
                ) {
                    Text("Add", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", fontFamily = FontFamily.Monospace, color = ClarityTextSecondary)
                }
            },
            containerColor = ClarityWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun QuickIncrementChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ClarityGrayBackground)
            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = ClarityTextPrimary
        )
    }
}

@Composable
private fun PaymentMethodTab(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ClarityBlack else ClarityWhite)
            .border(BorderStroke(1.dp, if (isSelected) ClarityBlack else ClarityBorder), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ClarityWhite else ClarityTextPrimary
        )
    }
}
