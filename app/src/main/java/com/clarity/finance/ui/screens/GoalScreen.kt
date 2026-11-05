package com.clarity.finance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.ExpenseViewModel
import com.clarity.finance.viewmodel.GoalMilestone
import com.clarity.finance.viewmodel.GoalViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LightGoalPurple =
    Color(0xFF6200EE)

private val DarkGoalPurple =
    Color(0xFFB388FF)

private val GoalGreen =
    Color(0xFF30D158)

private val GoalRed =
    Color(0xFFFF453A)

@Composable
fun GoalScreen(
    modifier: Modifier = Modifier,
    viewModel: GoalViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToIncome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {}
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val actionError by
    viewModel.actionError.collectAsStateWithLifecycle()

    val currentCurrency by
    authViewModel.currencyFlow.collectAsStateWithLifecycle()

    val exchangeRate by
    authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()

    var showDepositDialog by remember {
        mutableStateOf(false)
    }

    var showWithdrawDialog by remember {
        mutableStateOf(false)
    }

    var showAddGoalDialog by remember {
        mutableStateOf(false)
    }

    var showEditGoalDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteGoalDialog by remember {
        mutableStateOf(false)
    }

    var showAddExpenseSheet by remember {
        mutableStateOf(false)
    }

    val colors =
        ClarityTheme.colors

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            viewModel = expenseViewModel,
            onDismiss = { showAddExpenseSheet = false }
        )
    }

    Scaffold(
        modifier =
            modifier.fillMaxSize(),

        containerColor =
            colors.background,

        bottomBar = {
            ClarityBottomBar(
                currentTab =
                    ClarityNavTab.GOAL,

                onAddClick = {
                    showAddExpenseSheet = true
                },

                onTabSelected = { tab ->
                    when (tab) {
                        ClarityNavTab.HOME ->
                            onNavigateToHome()

                        ClarityNavTab.INCOME ->
                            onNavigateToIncome()

                        ClarityNavTab.INSIGHTS ->
                            onNavigateToInsights()

                        ClarityNavTab.GOAL ->
                            Unit
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),

            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top =
                        innerPadding.calculateTopPadding() +
                                12.dp,
                    bottom =
                        innerPadding.calculateBottomPadding() +
                                32.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            item {
                GoalHeader(
                    onAddGoal = {
                        viewModel.clearActionError()
                        showAddGoalDialog = true
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        LoadingGoalCard()
                    }
                }

                uiState.goal == null -> {
                    item {
                        EmptyGoalCard(
                            onAddGoal = {
                                showAddGoalDialog = true
                            }
                        )
                    }
                }

                else -> {
                    val goal =
                        uiState.goal!!

                    if (uiState.goals.size > 1) {
                        item {
                            GoalListSection(
                                goals =
                                    uiState.goals,

                                selectedGoalId =
                                    uiState.goal?.id,

                                onSelectGoal =
                                    viewModel::selectGoal,

                                onSetPrimary =
                                    viewModel::setPrimaryGoal,

                                currency =
                                    currentCurrency,

                                exchangeRate =
                                    exchangeRate
                            )
                        }
                    }

                    item {
                        GoalProgressCard(
                            goalName =
                                goal.name,

                            isPrimary =
                                goal.isPrimary,

                            onSetPrimary = {
                                viewModel.setPrimaryGoal(goal.id)
                            },

                            currentSavings =
                                uiState.currentSavings,

                            targetAmount =
                                goal.targetAmount,

                            progress =
                                uiState.progress,

                            deadline =
                                goal.deadline,

                            milestones =
                                uiState.milestones,

                            nextMilestone =
                                uiState.nextMilestone,

                            achievementMessage =
                                uiState.achievementMessage,

                            onEdit = {
                                viewModel.clearActionError()
                                showEditGoalDialog = true
                            },

                            onDelete = {
                                viewModel.clearActionError()
                                showDeleteGoalDialog = true
                            },

                            currency =
                                currentCurrency,

                            exchangeRate =
                                exchangeRate
                        )
                    }

                    item {
                        GoalMetricsSection(
                            remainingAmount =
                                uiState.remainingAmount,

                            requiredMonthlySaving =
                                uiState.requiredMonthlySaving,

                            monthsRemaining =
                                uiState.monthsRemaining,

                            predictDeadline =
                                uiState.predictDeadline
                                    ?.format(
                                        DateTimeFormatter
                                            .ofPattern(
                                                "dd MMM yyyy"
                                            )
                                    ),

                            currency =
                                currentCurrency,

                            exchangeRate =
                                exchangeRate
                        )
                    }

                    item {
                        GoalActionButtons(
                            hasSavings =
                                uiState.currentSavings > 0.0,

                            onSave = {
                                viewModel.clearActionError()
                                showDepositDialog = true
                            },

                            onWithdraw = {
                                viewModel.clearActionError()
                                showWithdrawDialog = true
                            }
                        )
                    }

                    item {
                        SavingsHistoryCard(
                            transactions =
                                uiState.transactions,

                            currency =
                                currentCurrency,

                            exchangeRate =
                                exchangeRate
                        )
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            currency =
                currentCurrency,

            errorMessage =
                actionError,

            onDismiss = {
                viewModel.clearActionError()
                showAddGoalDialog = false
            },

            onConfirm = {
                    name,
                    target,
                    starting,
                    deadline ->

                viewModel.addGoal(
                    name = name,
                    targetAmount = target,
                    startingAmount = starting,
                    deadline = deadline
                ) {
                    showAddGoalDialog = false
                }
            }
        )
    }

    if (showEditGoalDialog && uiState.goal != null) {
        val currentGoal = uiState.goal!!
        AddGoalDialog(
            title = "Edit Goal",
            confirmLabel = "Save Changes",
            initialName = currentGoal.name,
            initialTarget = if (currentGoal.targetAmount > 0) currentGoal.targetAmount.toLong().toString() else "",
            initialDeadline = currentGoal.deadline.ifBlank { LocalDate.now().plusYears(1).toString() },
            isEdit = true,
            currency = currentCurrency,
            errorMessage = actionError,
            onDismiss = {
                viewModel.clearActionError()
                showEditGoalDialog = false
            },
            onDelete = {
                viewModel.deleteGoal {
                    showEditGoalDialog = false
                }
            },
            onConfirm = { name, target, _, deadline ->
                viewModel.updateGoal(
                    name = name,
                    targetAmount = target,
                    deadline = deadline
                ) {
                    showEditGoalDialog = false
                }
            }
        )
    }

    if (showDeleteGoalDialog && uiState.goal != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteGoalDialog = false
            },
            title = {
                Text("Delete Goal")
            },
            text = {
                Text("Are you sure you want to delete \"${uiState.goal?.name}\"? All its transaction history will be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal {
                            showDeleteGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoalRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteGoalDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDepositDialog) {
        GoalTransactionDialog(
            title =
                "Save to Goal",

            confirmLabel =
                "Save",

            accentColor =
                goalAccent(),

            currency =
                currentCurrency,

            errorMessage =
                actionError,

            onDismiss = {
                viewModel.clearActionError()
                showDepositDialog = false
            },

            onConfirm = {
                    amount,
                    note ->

                viewModel.deposit(
                    amount = amount,
                    note = note
                )

                showDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        GoalTransactionDialog(
            title =
                "Withdraw Savings",

            confirmLabel =
                "Withdraw",

            accentColor =
                GoalRed,

            currency =
                currentCurrency,

            errorMessage =
                actionError,

            helperText =
                "Available: ${
                    formatLkr(
                        uiState.currentSavings,
                        currentCurrency,
                        exchangeRate
                    )
                }",

            onDismiss = {
                viewModel.clearActionError()
                showWithdrawDialog = false
            },

            onConfirm = {
                    amount,
                    note ->

                val accepted =
                    viewModel.withdraw(
                        amount = amount,
                        note = note
                    )

                if (accepted) {
                    showWithdrawDialog = false
                }
            }
        )
    }
}

@Composable
private fun GoalHeader(
    onAddGoal: () -> Unit
) {
    val colors =
        ClarityTheme.colors

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text =
                    "My Goals",

                color =
                    colors.label,

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme
                        .typography
                        .headlineLarge
            )

            Text(
                text =
                    "Build your savings one goal at a time.",

                color =
                    colors.secondaryLabel,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }

        IconButton(
            onClick =
                onAddGoal,

            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        color =
                            goalAccent(),
                        shape =
                            CircleShape
                    )
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Add,

                contentDescription =
                    "Add goal",

                tint =
                    if (
                        ClarityTheme.colors.isDark
                    ) {
                        Color.Black
                    } else {
                        Color.White
                    }
            )
        }
    }
}

@Composable
private fun GoalListSection(
    goals: List<Goal>,
    selectedGoalId: String?,
    onSelectGoal: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
    currency: String = "LKR",
    exchangeRate: Double = 1.0
) {
    val colors = ClarityTheme.colors
    val accent = goalAccent()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Your Goals (${goals.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.label
        )

        for (goal in goals) {
            val isSelected = goal.id == selectedGoalId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else colors.separator.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(if (isSelected) accent.copy(alpha = 0.08f) else colors.card)
                    .clickable { onSelectGoal(goal.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (goal.isPrimary) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "★ Primary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Target: ${formatLkr(goal.targetAmount, currency, exchangeRate)}" +
                            if (goal.deadline.isNotBlank()) " · Due ${goal.deadline}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryLabel
                    )
                }

                if (!goal.isPrimary) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetPrimary(goal.id) }
                            .background(colors.secondaryLabel.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Set Primary",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryLabel,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(
    goalName: String,
    currentSavings: Double,
    targetAmount: Double,
    progress: Float,
    deadline: String,
    milestones: List<GoalMilestone>,
    nextMilestone: GoalMilestone?,
    achievementMessage: String?,
    isPrimary: Boolean = false,
    onSetPrimary: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    currency: String = "LKR",
    exchangeRate: Double = 1.0
) {
    val colors =
        ClarityTheme.colors

    val accent =
        goalAccent()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        colors.card,
                    shape =
                        RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        goalName,

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis,

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                if (isPrimary) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text =
                                "★ Primary",

                            style =
                                MaterialTheme.typography.labelSmall,

                            color =
                                accent,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.secondaryLabel.copy(alpha = 0.12f))
                                .clickable { onSetPrimary() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text =
                                "Set Primary",

                            style =
                                MaterialTheme.typography.labelSmall,

                            color =
                                colors.secondaryLabel,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick =
                        onEdit,

                    modifier =
                        Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.Edit,

                        contentDescription =
                            "Edit goal",

                        tint =
                            accent,

                        modifier =
                            Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick =
                        onDelete,

                    modifier =
                        Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.Delete,

                        contentDescription =
                            "Delete goal",

                        tint =
                            GoalRed,

                        modifier =
                            Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        MilestoneProgressRing(
            progress =
                progress,

            milestones =
                milestones,

            nextMilestone =
                nextMilestone
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        ProgressMessageCard(
            progress =
                progress,

            goalName =
                goalName,

            achievementMessage =
                achievementMessage
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text =
                        "Saved",

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Text(
                    text =
                        formatLkr(
                            currentSavings,
                            currency,
                            exchangeRate
                        ),

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    text =
                        "Target",

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Text(
                    text =
                        formatLkr(
                            targetAmount,
                            currency,
                            exchangeRate
                        ),

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.CalendarMonth,

                contentDescription =
                    null,

                tint =
                    accent,

                modifier =
                    Modifier.size(18.dp)
            )

            Spacer(
                modifier =
                    Modifier.size(8.dp)
            )

            Text(
                text =
                    "Target date: $deadline",

                color =
                    colors.secondaryLabel,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}

@Composable
private fun MilestoneProgressRing(
    progress: Float,
    @Suppress("UNUSED_PARAMETER") milestones: List<GoalMilestone>,
    nextMilestone: GoalMilestone?
) {
    val colors = ClarityTheme.colors
    val accent = goalAccent()
    val isDark = colors.isDark

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "goalProgressAnimation"
    )

    val trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val glowColor = accent.copy(alpha = if (isDark) 0.35f else 0.20f)
    val completeColor = GoalGreen

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // 1. Smooth background track
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Soft outer glow behind the progress arc
                if (animatedProgress > 0.01f) {
                    drawArc(
                        color = glowColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 3. Main gradient progress arc
                    val ringColors = if (animatedProgress >= 1f) {
                        listOf(completeColor.copy(alpha = 0.8f), completeColor, Color(0xFF86EFAC))
                    } else {
                        listOf(accent.copy(alpha = 0.8f), accent, Color(0xFFC084FC))
                    }
                    val sweepGradient = Brush.sweepGradient(
                        colors = ringColors,
                        center = centerOffset
                    )

                    drawArc(
                        brush = sweepGradient,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 4. Milestone checkpoint markers (25%, 50%, 75%, 100%)
                listOf(25, 50, 75, 100).forEach { pct ->
                    val angleDeg = -90f + (360f * (pct / 100f))
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val markerX = centerOffset.x + radius * cos(rad).toFloat()
                    val markerY = centerOffset.y + radius * sin(rad).toFloat()
                    val isReached = animatedProgress >= (pct / 100f)

                    if (isReached) {
                        // Reached: White dot inside accent/green circle with border
                        drawCircle(
                            color = if (pct == 100) completeColor else accent,
                            radius = 6.dp.toPx(),
                            center = Offset(markerX, markerY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(markerX, markerY)
                        )
                    } else {
                        // Unreached: Subtle checkpoint marker on track
                        drawCircle(
                            color = if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC),
                            radius = 3.5.dp.toPx(),
                            center = Offset(markerX, markerY)
                        )
                    }
                }
            }

            // Central animated percentage & status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = colors.label,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (animatedProgress >= 1f) {
                    Text(
                        text = "ACHIEVED",
                        color = completeColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    Text(
                        text = "completed",
                        color = colors.secondaryLabel,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sleek milestone pills below the ring
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(25, 50, 75, 100).forEach { pct ->
                val reached = animatedProgress >= (pct / 100f)
                val isTargetNext = nextMilestone?.percentage == pct && !reached

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                reached -> completeColor.copy(alpha = 0.15f)
                                isTargetNext -> accent.copy(alpha = 0.15f)
                                else -> colors.background
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (reached) "✓ $pct%" else "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (reached || isTargetNext) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            reached -> completeColor
                            isTargetNext -> accent
                            else -> colors.secondaryLabel
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressMessageCard(
    progress: Float,
    goalName: String,
    achievementMessage: String?
) {
    val percent =
        (progress * 100)
            .toInt()

    val message =
        when {
            progress >= 1f ->
                "🎉 Goal Completed! You reached your $goalName target."

            percent >= 75 ->
                "🚀 Almost there! You've crossed 75% of your goal."

            percent >= 50 ->
                "🔥 Halfway there! You've already saved more than 50%."

            percent >= 25 ->
                "🏆 Great progress! You've completed the first 25%."

            else ->
                "🌱 Nice start! Keep saving to reach your first 25% milestone."
        }

    val displayMessage =
        when {
            progress >= 1f ->
                message

            achievementMessage != null &&
                    percent >= 25 ->
                achievementMessage

            else ->
                message
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        when {
                            progress >= 1f ->
                                successSoftBackground()

                            else ->
                                goalSoftBackground()
                        },

                    shape =
                        RoundedCornerShape(12.dp)
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                displayMessage,

            color =
                if (progress >= 1f) {
                    GoalGreen
                } else {
                    goalAccent()
                },

            fontWeight =
                FontWeight.SemiBold,

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

@Composable
private fun GoalMetricsSection(
    remainingAmount: Double,
    requiredMonthlySaving: Double,
    monthsRemaining: Long,
    predictDeadline: String?,
    currency: String = "LKR",
    exchangeRate: Double = 1.0
) {
    val colors =
        ClarityTheme.colors

    Column(
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text =
                "Goal Summary",

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            GoalMetricCard(
                title =
                    "Remaining",

                value =
                    formatLkr(
                        remainingAmount,
                        currency,
                        exchangeRate
                    ),

                modifier =
                    Modifier.weight(1f)
            )

            GoalMetricCard(
                title =
                    "Required / Month",

                value =
                    formatLkr(
                        requiredMonthlySaving,
                        currency,
                        exchangeRate
                    ),

                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            GoalMetricCard(
                title =
                    "Months Left",

                value =
                    monthsRemaining
                        .toString(),

                modifier =
                    Modifier.weight(1f)
            )

            GoalMetricCard(
                title =
                    "Predict Deadline",

                value =
                    predictDeadline
                        ?: "Not enough data",

                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors =
        ClarityTheme.colors

    Column(
        modifier =
            modifier
                .background(
                    colors.card,
                    RoundedCornerShape(14.dp)
                )
                .padding(14.dp)
    ) {
        Text(
            text =
                title,

            color =
                colors.secondaryLabel,

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(
            text =
                value,

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun GoalActionButtons(
    hasSavings: Boolean,
    onSave: () -> Unit,
    onWithdraw: () -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick =
                onSave,

            modifier =
                Modifier
                    .weight(1f)
                    .height(50.dp),

            shape =
                RoundedCornerShape(15.dp),

            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            goalAccent()
                    )
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Savings,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(18.dp)
            )

            Spacer(
                modifier =
                    Modifier.size(6.dp)
            )

            Text(
                text =
                    "Save to Goal",

                fontWeight =
                    FontWeight.SemiBold,

                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )
        }

        OutlinedButton(
            onClick =
                onWithdraw,

            enabled =
                hasSavings,

            modifier =
                Modifier
                    .weight(1f)
                    .height(50.dp),

            shape =
                RoundedCornerShape(15.dp),

            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor =
                        GoalRed,

                    disabledContentColor =
                        GoalRed.copy(
                            alpha = 0.35f
                        )
                )
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.ArrowUpward,

                contentDescription =
                    null,

                tint =
                    GoalRed,

                modifier =
                    Modifier.size(18.dp)
            )

            Spacer(
                modifier =
                    Modifier.size(6.dp)
            )

            Text(
                text =
                    "Withdraw",

                color =
                    GoalRed,

                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )
        }
    }
}

@Composable
private fun SavingsHistoryCard(
    transactions: List<GoalTransaction>,
    currency: String = "LKR",
    exchangeRate: Double = 1.0
) {
    val colors =
        ClarityTheme.colors

    var showAll by
    remember {
        mutableStateOf(false)
    }

    val visibleTransactions =
        if (showAll) {
            transactions
        } else {
            transactions.take(2)
        }

    Column {
        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "Savings History",

                color =
                    colors.label,

                fontWeight =
                    FontWeight.SemiBold,

                modifier =
                    Modifier.weight(1f)
            )

            if (transactions.size > 2) {
                Row(
                    modifier =
                        Modifier
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                showAll =
                                    !showAll
                            }
                            .padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            if (showAll) {
                                "Show Less"
                            } else {
                                "View All"
                            },

                        color =
                            goalAccent(),

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Icon(
                        imageVector =
                            if (showAll) {
                                Icons.Rounded
                                    .ArrowDropUp
                            } else {
                                Icons.Rounded
                                    .ArrowDropDown
                            },

                        contentDescription =
                            null,

                        tint =
                            goalAccent()
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        if (transactions.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            colors.card,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(28.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector =
                        Icons.Rounded.Savings,

                    contentDescription =
                        null,

                    tint =
                        goalAccent(),

                    modifier =
                        Modifier.size(34.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "No savings activity yet",

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        "Your deposits and withdrawals will appear here.",

                    color =
                        colors.secondaryLabel,

                    textAlign =
                        TextAlign.Center,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

        } else {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(16.dp)
                        )
                        .background(
                            colors.card
                        )
            ) {
                visibleTransactions
                    .forEachIndexed {
                            index,
                            transaction ->

                        SavingsHistoryRow(
                            transaction =
                                transaction,

                            currency =
                                currency,

                            exchangeRate =
                                exchangeRate
                        )

                        if (
                            index <
                            visibleTransactions.lastIndex
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            colors.separator
                                        )
                            )
                        }
                    }
            }

            AnimatedVisibility(
                visible =
                    showAll &&
                            transactions.size > 6
            ) {
                Text(
                    text =
                        "Showing ${transactions.size} transactions",

                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        ),

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun SavingsHistoryRow(
    transaction: GoalTransaction,
    currency: String = "LKR",
    exchangeRate: Double = 1.0
) {
    val colors =
        ClarityTheme.colors

    val isDeposit =
        transaction.type ==
                GoalTransactionType.DEPOSIT

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .background(
                        color =
                            if (isDeposit) {
                                successSoftBackground()
                            } else {
                                errorSoftBackground()
                            },

                        shape =
                            CircleShape
                    ),

            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                imageVector =
                    if (isDeposit) {
                        Icons.Rounded
                            .ArrowDownward
                    } else {
                        Icons.Rounded
                            .ArrowUpward
                    },

                contentDescription =
                    null,

                tint =
                    if (isDeposit) {
                        GoalGreen
                    } else {
                        GoalRed
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.size(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text =
                    if (isDeposit) {
                        "Savings deposit"
                    } else {
                        "Savings withdrawal"
                    },

                color =
                    colors.label,

                fontWeight =
                    FontWeight.Medium
            )

            if (
                transaction.note
                    .isNotBlank()
            ) {
                Text(
                    text =
                        transaction.note,

                    color =
                        colors.secondaryLabel,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        Text(
            text =
                if (isDeposit) {
                    "+${
                        formatLkr(
                            transaction.amount,
                            currency,
                            exchangeRate
                        )
                    }"
                } else {
                    "-${
                        formatLkr(
                            transaction.amount,
                            currency,
                            exchangeRate
                        )
                    }"
                },

            color =
                if (isDeposit) {
                    GoalGreen
                } else {
                    GoalRed
                },

            fontWeight =
                FontWeight.SemiBold,

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    title: String = "Add New Goal",
    confirmLabel: String = "Create Goal",
    initialName: String = "",
    initialTarget: String = "",
    initialStarting: String = "",
    initialDeadline: String = LocalDate.now().plusYears(1).toString(),
    isEdit: Boolean = false,
    currency: String = "LKR",
    errorMessage: String?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (
        String,
        Double,
        Double,
        String
    ) -> Unit
) {
    var name by
    remember(initialName) {
        mutableStateOf(initialName)
    }

    var targetText by
    remember(initialTarget) {
        mutableStateOf(initialTarget)
    }

    var startingText by
    remember(initialStarting) {
        mutableStateOf(initialStarting)
    }

    var deadline by
    remember(initialDeadline) {
        mutableStateOf(initialDeadline)
    }

    var showDatePicker by
    remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {
            Text(
                title
            )
        },

        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value =
                        name,

                    onValueChange = {
                        name = it
                    },

                    label = {
                        Text(
                            "Goal Name"
                        )
                    },

                    placeholder = {
                        Text(
                            "e.g. Japan Trip"
                        )
                    },

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value =
                        targetText,

                    onValueChange = {
                        targetText =
                            sanitizeAmountInput(it)
                    },

                    label = {
                        Text(
                            "Target Amount"
                        )
                    },

                    prefix = {
                        Text(
                            "${CurrencyFormatter.getSymbol(currency)} "
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (!isEdit) {
                    OutlinedTextField(
                        value =
                            startingText,

                        onValueChange = {
                            startingText =
                                sanitizeAmountInput(it)
                        },

                        label = {
                            Text(
                                "Starting Savings"
                            )
                        },

                        prefix = {
                            Text(
                                "${CurrencyFormatter.getSymbol(currency)} "
                            )
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),

                        singleLine =
                            true,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value =
                        deadline,

                    onValueChange = {},

                    readOnly =
                        true,

                    label = {
                        Text(
                            "Target Date"
                        )
                    },

                    trailingIcon = {
                        IconButton(
                            onClick = {
                                showDatePicker = true
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Rounded.CalendarMonth,

                                contentDescription =
                                    "Choose target date",

                                tint =
                                    goalAccent()
                            )
                        }
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePicker = true
                            }
                )

                if (errorMessage != null) {
                    Text(
                        text =
                            errorMessage,

                        color =
                            GoalRed,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,

                        targetText
                            .toDoubleOrNull()
                            ?: 0.0,

                        startingText
                            .toDoubleOrNull()
                            ?: 0.0,

                        deadline
                    )
                },

                enabled =
                    name.isNotBlank() &&
                            (
                                    targetText
                                        .toDoubleOrNull()
                                        ?: 0.0
                                    ) > 0.0
            ) {
                Text(
                    confirmLabel
                )
            }
        },

        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = onDelete
                    ) {
                        Text(
                            "Delete",
                            color = GoalRed
                        )
                    }
                }
                TextButton(
                    onClick =
                        onDismiss
                ) {
                    Text(
                        "Cancel"
                    )
                }
            }
        }
    )

    if (showDatePicker) {
        GoalDatePickerDialog(
            currentDate =
                deadline,

            onDismiss = {
                showDatePicker = false
            },

            onDateSelected = {
                    selectedDate ->

                deadline =
                    selectedDate

                showDatePicker =
                    false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialDate =
        runCatching {
            LocalDate.parse(
                currentDate
            )
        }.getOrDefault(
            LocalDate.now()
        )

    val initialMillis =
        initialDate
            .atStartOfDay(
                ZoneId.systemDefault()
            )
            .toInstant()
            .toEpochMilli()

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                initialMillis
        )

    DatePickerDialog(
        onDismissRequest =
            onDismiss,

        confirmButton = {
            TextButton(
                onClick = {
                    val millis =
                        datePickerState
                            .selectedDateMillis

                    if (millis != null) {
                        val selectedDate =
                            Instant
                                .ofEpochMilli(millis)
                                .atZone(
                                    ZoneId.of("UTC")
                                )
                                .toLocalDate()
                                .toString()

                        onDateSelected(
                            selectedDate
                        )
                    }
                }
            ) {
                Text(
                    "Select"
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(
                    "Cancel"
                )
            }
        }
    ) {
        DatePicker(
            state =
                datePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalTransactionDialog(
    title: String,
    confirmLabel: String,
    accentColor: Color,
    errorMessage: String?,
    helperText: String? = null,
    currency: String = "LKR",
    onDismiss: () -> Unit,
    onConfirm: (
        Double,
        String
    ) -> Unit
) {
    var amountText by
    remember {
        mutableStateOf("")
    }

    var noteText by
    remember {
        mutableStateOf("")
    }

    val amount =
        amountText
            .toDoubleOrNull()
            ?: 0.0

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {
            Text(
                title
            )
        },

        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                helperText?.let {
                    Text(
                        text =
                            it,

                        color =
                            ClarityTheme
                                .colors
                                .secondaryLabel
                    )
                }

                OutlinedTextField(
                    value =
                        amountText,

                    onValueChange = {
                        amountText =
                            sanitizeAmountInput(it)
                    },

                    label = {
                        Text(
                            "Amount"
                        )
                    },

                    prefix = {
                        Text(
                            "${CurrencyFormatter.getSymbol(currency)} "
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),

                    isError =
                        errorMessage != null,

                    supportingText =
                        if (errorMessage != null) {
                            {
                                Text(
                                    text =
                                        errorMessage,

                                    color =
                                        GoalRed
                                )
                            }
                        } else {
                            null
                        },

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value =
                        noteText,

                    onValueChange = {
                        noteText =
                            it
                    },

                    label = {
                        Text(
                            "Note (optional)"
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        amount,
                        noteText
                    )
                },

                enabled =
                    amount > 0.0,

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                accentColor
                        )
            ) {
                Text(
                    confirmLabel
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(
                    "Cancel"
                )
            }
        }
    )
}

@Composable
private fun LoadingGoalCard() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(220.dp),

        contentAlignment =
            Alignment.Center
    ) {
        CircularProgressIndicator(
            color =
                goalAccent()
        )
    }
}

@Composable
private fun EmptyGoalCard(
    onAddGoal: () -> Unit
) {
    val colors =
        ClarityTheme.colors

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    colors.card,
                    RoundedCornerShape(16.dp)
                )
                .padding(28.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector =
                Icons.Rounded.Savings,

            contentDescription =
                null,

            tint =
                goalAccent(),

            modifier =
                Modifier.size(42.dp)
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "No goals yet",

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold
        )

        Text(
            text =
                "Create your first savings goal to get started.",

            color =
                colors.secondaryLabel,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Button(
            onClick =
                onAddGoal,

            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            goalAccent()
                    )
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Add,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.size(6.dp)
            )

            Text(
                "Add Goal"
            )
        }
    }
}

@Composable
private fun goalAccent(): Color =
    if (
        ClarityTheme.colors.isDark
    ) {
        DarkGoalPurple
    } else {
        LightGoalPurple
    }

@Composable
private fun goalSoftBackground(): Color =
    if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF2D2238)
    } else {
        Color(0xFFF4E9FF)
    }

@Composable
private fun successSoftBackground(): Color =
    if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF14351F)
    } else {
        Color(0xFFE8F7EC)
    }

@Composable
private fun errorSoftBackground(): Color =
    if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF3B1D1D)
    } else {
        Color(0xFFFFECEB)
    }

private fun sanitizeAmountInput(
    input: String
): String {
    val filtered =
        input.filter {
            it.isDigit() ||
                    it == '.'
        }

    val firstDot =
        filtered.indexOf('.')

    return if (
        firstDot == -1
    ) {
        filtered
    } else {
        filtered.substring(
            0,
            firstDot + 1
        ) +
                filtered
                    .substring(
                        firstDot + 1
                    )
                    .replace(
                        ".",
                        ""
                    )
    }
}

private fun formatLkr(
    amount: Double,
    currency: String = "LKR",
    exchangeRate: Double = 1.0
): String {
    return CurrencyFormatter.format(amount, currency, exchangeRate)
}