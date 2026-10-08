package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Uygulama genelinde standart, 3 kademeli (PartiallyExpanded -> Expanded -> Dismiss)
 * ve Çift Yönlü Kenar Kaydırma (Dual Edge Dismiss) özellikli ModalBottomSheet sarmalayıcısı.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheetWrapper(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    preventDismissOnDrag: Boolean = false,
    dismissOnScrim: Boolean = true,
    sheetState: SheetState? = null,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    enableHorizontalSwipeDismiss: Boolean = true,
    horizontalSwipeThreshold: Dp = 45.dp,
    enableVerticalScroll: Boolean = false,
    contentPadding: Dp = 0.dp,
    contentModifier: Modifier = Modifier,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.(dismissSheet: () -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var allowDismiss by remember { mutableStateOf(!preventDismissOnDrag) }

    val actualSheetState = sheetState ?: rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (preventDismissOnDrag && targetValue == androidx.compose.material3.SheetValue.Hidden) {
                allowDismiss
            } else {
                true
            }
        }
    )

    val density = LocalDensity.current
    val swipeThresholdPx = remember(density, horizontalSwipeThreshold) {
        with(density) { horizontalSwipeThreshold.toPx() }
    }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    val dismissAction: () -> Unit = {
        allowDismiss = true
        coroutineScope.launch {
            try {
                actualSheetState.hide()
            } catch (_: Exception) {}
            onDismissRequest()
        }
    }

    BackHandler(enabled = actualSheetState.isVisible) {
        dismissAction()
    }

    val dragModifier = if (enableHorizontalSwipeDismiss) {
        Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    totalDragX += dragAmount
                    if (kotlin.math.abs(totalDragX) > swipeThresholdPx) {
                        change.consume()
                        dismissAction()
                    }
                }
            )
        }
    } else Modifier

    ModalBottomSheet(
        onDismissRequest = {
            if (dismissOnScrim || allowDismiss) {
                onDismissRequest()
            }
        },
        sheetState = actualSheetState,
        shape = shape,
        containerColor = containerColor,
        dragHandle = {
            if (dragHandle != null) {
                Box(modifier = dragModifier) {
                    dragHandle()
                }
            }
        },
        modifier = modifier
    ) {
        val baseColumnModifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .imePadding()

        val finalColumnModifier = if (enableVerticalScroll) {
            baseColumnModifier.verticalScroll(rememberScrollState())
        } else {
            baseColumnModifier
        }.padding(start = contentPadding, end = contentPadding, bottom = contentPadding)
            .then(contentModifier)

        Column(
            modifier = finalColumnModifier
        ) {
            content(dismissAction)
        }
    }
}
