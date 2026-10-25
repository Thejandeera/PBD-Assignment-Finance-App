package com.clarity.finance.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme

/**
 * iOS-style modal sheet (HIG > Sheets): grouped background, rounded top corners and a grabber.
 *
 * The scrim covers the whole screen, status bar included, as iOS sheets do. Insets are applied
 * to the sheet content instead, and the sheet never rises behind the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaritySheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ClarityTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.sheetBackground,
        contentColor = colors.label,
        shape = ClarityShapes.sheet,
        tonalElevation = 0.dp,
        dragHandle = { SheetGrabber() },
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight - statusBarHeight - 8.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                content = content
            )
        }
    }
}
