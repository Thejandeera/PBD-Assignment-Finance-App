package com.clarity.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.viewmodel.GoalMilestone
import com.clarity.finance.viewmodel.GoalViewModel
import java.text.NumberFormat
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
    onNavigateToHome: () -> Unit = {},
    onNavigateToIncome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {}
) {

    val uiState by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    val actionError by
    viewModel.actionError
        .collectAsStateWithLifecycle()

    var showDepositDialog by remember {
        mutableStateOf(false)
    }

    var showWithdrawDialog by remember {
        mutableStateOf(false)
    }

    val colors =
        ClarityTheme.colors

    Scaffold(
        modifier =
            modifier.fillMaxSize(),

        containerColor =
            colors.background,

        bottomBar = {

            ClarityBottomBar(
                currentTab =
                    ClarityNavTab.GOAL,

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
                        innerPadding
                            .calculateTopPadding() +
                                12.dp,

                    bottom =
                        innerPadding
                            .calculateBottomPadding() +
                                32.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            item {

                Text(
                    text =
                        "My Goal",

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.Bold,

                    style =
                        MaterialTheme
                            .typography
                            .headlineLarge
                )
            }

            item {

                Text(
                    text =
                        "Stay focused on what you're saving for.",

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
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
                            error =
                                uiState.error
                        )
                    }
                }

                else -> {

                    val goal =
                        uiState.goal!!

                    item {

                        GoalProgressCard(
                            goalName =
                                goal.name,

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
                                uiState.nextMilestone
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

                            projectedFinishDate =
                                uiState
                                    .projectedFinishDate
                                    ?.format(
                                        DateTimeFormatter
                                            .ofPattern(
                                                "dd MMM yyyy"
                                            )
                                    )
                        )
                    }

                    item {

                        GoalStatusCard(
                            progress =
                                uiState.progress,

                            requiredMonthlySaving =
                                uiState
                                    .requiredMonthlySaving,

                            monthlyDepositAverage =
                                uiState
                                    .monthlyDepositAverage
                        )
                    }

                    item {

                        GoalActionButtons(
                            hasSavings =
                                uiState.currentSavings >
                                        0.0,

                            onSave = {

                                viewModel
                                    .clearActionError()

                                showDepositDialog =
                                    true
                            },

                            onWithdraw = {

                                viewModel
                                    .clearActionError()

                                showWithdrawDialog =
                                    true
                            }
                        )
                    }

                    item {

                        SavingsHistoryCard(
                            transactions =
                                uiState.transactions
                        )
                    }
                }
            }
        }
    }

    if (showDepositDialog) {

        GoalTransactionDialog(
            title =
                "Save to Goal",

            confirmLabel =
                "Save",

            accentColor =
                goalAccent(),

            errorMessage =
                actionError,

            onDismiss = {

                viewModel
                    .clearActionError()

                showDepositDialog =
                    false
            },

            onConfirm = {
                    amount,
                    note ->

                viewModel.deposit(
                    amount =
                        amount,

                    note =
                        note
                )

                showDepositDialog =
                    false
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

            errorMessage =
                actionError,

            helperText =
                "Available: ${
                    formatLkr(
                        uiState.currentSavings
                    )
                }",

            onDismiss = {

                viewModel
                    .clearActionError()

                showWithdrawDialog =
                    false
            },

            onConfirm = {
                    amount,
                    note ->

                val accepted =
                    viewModel.withdraw(
                        amount =
                            amount,

                        note =
                            note
                    )

                if (accepted) {

                    showWithdrawDialog =
                        false
                }
            }
        )
    }
}

@Composable
private fun goalAccent(): Color {

    return if (
        ClarityTheme.colors.isDark
    ) {
        DarkGoalPurple
    } else {
        LightGoalPurple
    }
}

@Composable
private fun goalSoftBackground(): Color {

    return if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF2D2238)
    } else {
        Color(0xFFF4E9FF)
    }
}

@Composable
private fun successSoftBackground(): Color {

    return if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF14351F)
    } else {
        Color(0xFFE8F7EC)
    }
}

@Composable
private fun errorSoftBackground(): Color {

    return if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF3B1D1D)
    } else {
        Color(0xFFFFECEB)
    }
}

@Composable
private fun lockedBackground(): Color {

    return if (
        ClarityTheme.colors.isDark
    ) {
        Color(0xFF303033)
    } else {
        Color(0xFFF2F2F7)
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
    nextMilestone: GoalMilestone?
) {

    val colors =
        ClarityTheme.colors

    val accent =
        goalAccent()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color =
                    colors.card,

                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            )
            .padding(
                18.dp
            )
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(
                        46.dp
                    )
                    .background(
                        color =
                            goalSoftBackground(),

                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Rounded
                            .LaptopMac,

                    contentDescription =
                        null,

                    tint =
                        accent,

                    modifier =
                        Modifier.size(
                            27.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.size(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        goalName,

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.SemiBold,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    text =
                        "Primary savings goal",

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            Text(
                text =
                    "${(progress * 100).toInt()}%",

                color =
                    accent,

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
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
                Modifier.height(
                    22.dp
                )
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
                            currentSavings
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
                            targetAmount
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
                Modifier.height(
                    16.dp
                )
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Rounded
                        .CalendarMonth,

                contentDescription =
                    null,

                tint =
                    accent,

                modifier =
                    Modifier.size(
                        18.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.size(
                        8.dp
                    )
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
    milestones: List<GoalMilestone>,
    nextMilestone: GoalMilestone?
) {

    val colors =
        ClarityTheme.colors

    val accent =
        goalAccent()

    Box(
        modifier =
            Modifier.fillMaxWidth(),

        contentAlignment =
            Alignment.Center
    ) {

        Box(
            modifier =
                Modifier.size(
                    190.dp
                ),

            contentAlignment =
                Alignment.Center
        ) {

            CircularProgressIndicator(
                progress = {
                    progress
                },

                modifier =
                    Modifier.size(
                        150.dp
                    ),

                color =
                    accent,

                trackColor =
                    if (
                        colors.isDark
                    ) {
                        Color(
                            0xFF3A3A3C
                        )
                    } else {
                        Color(
                            0xFFE5E5EA
                        )
                    },

                strokeWidth =
                    12.dp
            )

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        "${(progress * 100).toInt()}%",

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.Bold,

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )

                Text(
                    text =
                        "completed",

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            milestones
                .getOrNull(0)
                ?.let { milestone ->

                    MilestoneBubble(
                        milestone =
                            milestone,

                        isNext =
                            nextMilestone
                                ?.percentage ==
                                    milestone.percentage,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopCenter
                                )
                    )
                }

            milestones
                .getOrNull(1)
                ?.let { milestone ->

                    MilestoneBubble(
                        milestone =
                            milestone,

                        isNext =
                            nextMilestone
                                ?.percentage ==
                                    milestone.percentage,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.CenterEnd
                                )
                    )
                }

            milestones
                .getOrNull(2)
                ?.let { milestone ->

                    MilestoneBubble(
                        milestone =
                            milestone,

                        isNext =
                            nextMilestone
                                ?.percentage ==
                                    milestone.percentage,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                    )
                }

            milestones
                .getOrNull(3)
                ?.let { milestone ->

                    MilestoneBubble(
                        milestone =
                            milestone,

                        isNext =
                            nextMilestone
                                ?.percentage ==
                                    milestone.percentage,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.CenterStart
                                )
                    )
                }
        }
    }

    Spacer(
        modifier =
            Modifier.height(
                6.dp
            )
    )

    if (nextMilestone != null) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    goalSoftBackground(),
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .padding(
                    horizontal =
                        12.dp,

                    vertical =
                        10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Rounded
                        .TrackChanges,

                contentDescription =
                    null,

                tint =
                    accent,

                modifier =
                    Modifier.size(
                        18.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.size(
                        8.dp
                    )
            )

            Text(
                text =
                    "Next milestone: ${nextMilestone.percentage}% · ${nextMilestone.title}",

                color =
                    colors.label,

                fontWeight =
                    FontWeight.Medium,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

    } else {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    successSoftBackground(),
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .padding(
                    12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Rounded.Check,

                contentDescription =
                    null,

                tint =
                    GoalGreen
            )

            Spacer(
                modifier =
                    Modifier.size(
                        8.dp
                    )
            )

            Text(
                text =
                    "All milestones completed!",

                color =
                    GoalGreen,

                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MilestoneBubble(
    milestone: GoalMilestone,
    isNext: Boolean,
    modifier: Modifier = Modifier
) {

    val colors =
        ClarityTheme.colors

    val accent =
        goalAccent()

    val background =
        when {

            milestone.isReached ->
                GoalGreen

            isNext ->
                accent

            else ->
                lockedBackground()
        }

    val foreground =
        when {

            milestone.isReached ->
                Color.White

            isNext ->
                if (
                    colors.isDark
                ) {
                    Color.Black
                } else {
                    Color.White
                }

            else ->
                colors.secondaryLabel
        }

    Box(
        modifier =
            modifier
                .size(
                    38.dp
                )
                .clip(
                    CircleShape
                )
                .background(
                    background
                )
                .then(
                    if (
                        isNext &&
                        !milestone.isReached
                    ) {

                        Modifier.border(
                            width =
                                2.dp,

                            color =
                                if (
                                    colors.isDark
                                ) {
                                    Color.White
                                        .copy(
                                            alpha =
                                                0.35f
                                        )
                                } else {
                                    accent.copy(
                                        alpha =
                                            0.25f
                                    )
                                },

                            shape =
                                CircleShape
                        )

                    } else {
                        Modifier
                    }
                ),

        contentAlignment =
            Alignment.Center
    ) {

        if (milestone.isReached) {

            Icon(
                imageVector =
                    Icons.Rounded.Check,

                contentDescription =
                    "${milestone.percentage}% reached",

                tint =
                    foreground,

                modifier =
                    Modifier.size(
                        18.dp
                    )
            )

        } else {

            Text(
                text =
                    milestone.percentage
                        .toString(),

                color =
                    foreground,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center,

                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )
        }
    }
}

@Composable
private fun GoalMetricsSection(
    remainingAmount: Double,
    requiredMonthlySaving: Double,
    monthsRemaining: Long,
    projectedFinishDate: String?
) {

    val colors =
        ClarityTheme.colors

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        Text(
            text =
                "Goal Summary",

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold,

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            GoalMetricCard(
                title =
                    "Remaining",

                value =
                    formatLkr(
                        remainingAmount
                    ),

                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            GoalMetricCard(
                title =
                    "Required / Month",

                value =
                    formatLkr(
                        requiredMonthlySaving
                    ),

                modifier =
                    Modifier.weight(
                        1f
                    )
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            GoalMetricCard(
                title =
                    "Months Left",

                value =
                    monthsRemaining
                        .toString(),

                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            GoalMetricCard(
                title =
                    "Projected Finish",

                value =
                    projectedFinishDate
                        ?: "Not enough data",

                modifier =
                    Modifier.weight(
                        1f
                    )
            )
        }

        Text(
            text =
                "Projected Finish estimates when you may reach the goal if your recent average saving rate continues.",

            color =
                colors.secondaryLabel,

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
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
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .padding(
                    14.dp
                )
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
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                value,

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold,

            style =
                MaterialTheme
                    .typography
                    .bodyLarge
        )
    }
}

@Composable
private fun GoalStatusCard(
    progress: Float,
    requiredMonthlySaving: Double,
    monthlyDepositAverage: Double
) {

    val colors =
        ClarityTheme.colors

    val accent =
        goalAccent()

    val message =
        when {

            progress >= 1f ->
                "Goal completed! You reached your target."

            monthlyDepositAverage <= 0.0 ->
                "Start saving to calculate your projected finish date."

            monthlyDepositAverage >=
                    requiredMonthlySaving ->

                "You're on track. Keep up your current saving rate."

            else ->
                "Increase your monthly savings to stay on track."
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color =
                    goalSoftBackground(),

                shape =
                    RoundedCornerShape(
                        14.dp
                    )
            )
            .padding(
                14.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Rounded
                    .TrackChanges,

            contentDescription =
                null,

            tint =
                accent
        )

        Spacer(
            modifier =
                Modifier.size(
                    12.dp
                )
        )

        Column {

            Text(
                text =
                    "Goal Status",

                color =
                    accent,

                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    message,

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
private fun GoalActionButtons(
    hasSavings: Boolean,
    onSave: () -> Unit,
    onWithdraw: () -> Unit
) {

    val accent =
        goalAccent()

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        Button(
            onClick =
                onSave,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .height(
                        52.dp
                    ),

            shape =
                RoundedCornerShape(
                    16.dp
                ),

            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            accent
                    )
        ) {

            Icon(
                imageVector =
                    Icons.Rounded
                        .Savings,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.size(
                        8.dp
                    )
            )

            Text(
                text =
                    "Save to Goal",

                fontWeight =
                    FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick =
                onWithdraw,

            enabled =
                hasSavings,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .height(
                        52.dp
                    ),

            shape =
                RoundedCornerShape(
                    16.dp
                )
        ) {

            Icon(
                imageVector =
                    Icons.Rounded
                        .ArrowUpward,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.size(
                        7.dp
                    )
            )

            Text(
                text =
                    "Withdraw"
            )
        }
    }
}

@Composable
private fun SavingsHistoryCard(
    transactions: List<GoalTransaction>
) {

    val colors =
        ClarityTheme.colors

    Column {

        Text(
            text =
                "Savings History",

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold,

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        if (transactions.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.card,
                        RoundedCornerShape(
                            16.dp
                        )
                    )
                    .padding(
                        28.dp
                    ),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(
                            50.dp
                        )
                        .background(
                            goalSoftBackground(),
                            CircleShape
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Rounded.Savings,

                        contentDescription =
                            null,

                        tint =
                            goalAccent(),

                        modifier =
                            Modifier.size(
                                25.dp
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Text(
                    text =
                        "No savings activity yet",

                    color =
                        colors.label,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(
                    text =
                        "Save money toward your goal and your history will appear here.",

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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 100.dp,
                        max = 360.dp
                    )
                    .background(
                        color =
                            colors.card,

                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
                    .clip(
                        RoundedCornerShape(
                            16.dp
                        )
                    ),

                contentPadding =
                    PaddingValues(
                        vertical =
                            4.dp
                    )
            ) {

                itemsIndexed(
                    items =
                        transactions,

                    key = {
                            index,
                            transaction ->

                        transaction.id
                            .ifBlank {
                                "$index-${transaction.createdAt}"
                            }
                    }
                ) {
                        index,
                        transaction ->

                    SavingsHistoryRow(
                        transaction =
                            transaction
                    )

                    if (
                        index <
                        transactions.lastIndex
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    1.dp
                                )
                                .background(
                                    colors.separator
                                )
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    "Swipe inside the card to view older savings activity.",

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
private fun SavingsHistoryRow(
    transaction: GoalTransaction
) {

    val colors =
        ClarityTheme.colors

    val isDeposit =
        transaction.type ==
                GoalTransactionType.DEPOSIT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal =
                    14.dp,

                vertical =
                    12.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        38.dp
                    )
                    .background(
                        color =
                            if (
                                isDeposit
                            ) {
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
                    if (
                        isDeposit
                    ) {
                        Icons.Rounded
                            .ArrowDownward
                    } else {
                        Icons.Rounded
                            .ArrowUpward
                    },

                contentDescription =
                    null,

                tint =
                    if (
                        isDeposit
                    ) {
                        GoalGreen
                    } else {
                        GoalRed
                    },

                modifier =
                    Modifier.size(
                        20.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.size(
                    12.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    if (
                        isDeposit
                    ) {
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
                transaction
                    .note
                    .isNotBlank()
            ) {

                Text(
                    text =
                        transaction.note,

                    color =
                        colors.secondaryLabel,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        Text(
            text =
                if (
                    isDeposit
                ) {
                    "+${
                        formatLkr(
                            transaction.amount
                        )
                    }"
                } else {
                    "-${
                        formatLkr(
                            transaction.amount
                        )
                    }"
                },

            color =
                if (
                    isDeposit
                ) {
                    GoalGreen
                } else {
                    GoalRed
                },

            fontWeight =
                FontWeight.SemiBold,

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}

@Composable
private fun LoadingGoalCard() {

    val colors =
        ClarityTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                220.dp
            )
            .background(
                colors.card,
                RoundedCornerShape(
                    16.dp
                )
            ),

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
    error: String?
) {

    val colors =
        ClarityTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colors.card,
                RoundedCornerShape(
                    16.dp
                )
            )
            .padding(
                24.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                Icons.Rounded
                    .TrackChanges,

            contentDescription =
                null,

            tint =
                goalAccent(),

            modifier =
                Modifier.size(
                    42.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        Text(
            text =
                "Goal unavailable",

            color =
                colors.label,

            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        Text(
            text =
                error
                    ?: "Your savings goal could not be loaded.",

            color =
                colors.secondaryLabel,

            textAlign =
                TextAlign.Center
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun GoalTransactionDialog(
    title: String,
    confirmLabel: String,
    accentColor: Color,
    errorMessage: String?,
    helperText: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        note: String
    ) -> Unit
) {

    var amountText by remember {
        mutableStateOf("")
    }

    var noteText by remember {
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
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                if (
                    helperText != null
                ) {

                    Text(
                        text =
                            helperText,

                        color =
                            ClarityTheme
                                .colors
                                .secondaryLabel,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                OutlinedTextField(
                    value =
                        amountText,

                    onValueChange = {

                        amountText =
                            sanitizeAmountInput(
                                it
                            )
                    },

                    label = {

                        Text(
                            "Amount"
                        )
                    },

                    prefix = {

                        Text(
                            "LKR "
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
                        if (
                            errorMessage != null
                        ) {

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

                    if (
                        amount > 0.0
                    ) {

                        onConfirm(
                            amount,
                            noteText
                        )
                    }
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

private fun sanitizeAmountInput(
    input: String
): String {

    val filtered =
        input.filter {
            it.isDigit() ||
                    it == '.'
        }

    val firstDot =
        filtered.indexOf(
            '.'
        )

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
    amount: Double
): String {

    val formatter =
        NumberFormat
            .getNumberInstance(
                Locale.US
            )

    formatter.maximumFractionDigits =
        0

    return "LKR ${
        formatter.format(
            amount
        )
    }"
}