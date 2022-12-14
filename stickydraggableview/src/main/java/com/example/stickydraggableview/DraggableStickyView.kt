package com.example.stickydraggableview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DraggableStickyView(
    modifier: Modifier = Modifier,
    state: DraggableViewState,
    checkIntersection: (DraggableViewState) -> Boolean,
    rememberState: (DraggableViewState) -> Unit,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var position by remember { mutableStateOf(Offset.Zero) }
    val animatedOffset by remember {
        mutableStateOf(Animatable(state.startOffset, Offset.VectorConverter))
    }

    LaunchedEffect(Unit) {
        rememberState(state)
    }

    if (state.visibility.value) {
        Layout(
            content = content,
            measurePolicy = measurePolicy(),
            modifier = modifier
                .offset {
                    IntOffset(
                        animatedOffset.value.x.roundToInt(),
                        animatedOffset.value.y.roundToInt(),
                    )
                }
                .zIndex(2f)
                .onGloballyPositioned { coords ->
                    position = coords.positionInRoot()
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            state.currentOffset = position
                            val isCardArea = checkIntersection(state)
                            if (isCardArea) {
                                coroutineScope.launch {
                                    animatedOffset.snapTo(targetValue = state.startOffset)
                                }
                            } else {
                                coroutineScope.launch {
                                    animatedOffset.animateTo(
                                        targetValue = state.startOffset,
                                        animationSpec = tween(
                                            durationMillis = 1000,
                                            delayMillis = 0
                                        )
                                    )
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        coroutineScope.launch {
                            animatedOffset.snapTo(
                                Offset(
                                    animatedOffset.value.x + dragAmount.x,
                                    animatedOffset.value.y + dragAmount.y,
                                )
                            )
                        }
                    }
                }
        )
    }
}

private fun measurePolicy() =
    MeasurePolicy { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable ->
            measurable.measure(looseConstraints)
        }

        val layoutWidth = placeables.maxOfOrNull { it.width } ?: 0
        val layoutHeight = placeables.maxOfOrNull { it.height } ?: 0

        layout(layoutWidth, layoutHeight) {
            var xPosition: Int
            var yPosition: Int

            placeables.forEach { placeable ->
                xPosition = layoutWidth/2 - placeable.width/2
                yPosition = layoutHeight/2 - placeable.height/2

                placeable.place(x = xPosition, y = yPosition)
            }
        }
    }
