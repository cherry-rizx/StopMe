package com.hanyz.stopme.ui.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.theme.OrangeAccent
import com.hanyz.stopme.ui.theme.SlidePillOrange
import com.hanyz.stopme.ui.theme.TextWhite
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SlideToDismissBar(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val knobSize = 54.dp
    val barHeight = 62.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(31.dp))
            .background(SlidePillOrange)
            .testTag("slide_to_dismiss_bar"),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val maxOffsetPx = with(density) { (maxWidth - knobSize - 8.dp).toPx() }
        val offsetX = remember { Animatable(0f) }

        // Teks "Slide to Dismiss" di tengah pill
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.slide_to_dismiss_pill),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }

        // Knob putih berisi ic_slide_dismiss
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(knobSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(maxOffsetPx) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value >= maxOffsetPx * 0.8f) {
                                    offsetX.animateTo(maxOffsetPx, tween(150))
                                    onDismissed()
                                } else {
                                    offsetX.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f, tween(200)) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount.x).coerceIn(0f, maxOffsetPx)
                            coroutineScope.launch {
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
                .testTag("slide_to_dismiss_knob"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_slide_dismiss),
                contentDescription = stringResource(id = R.string.slide_to_dismiss_instruction),
                tint = OrangeAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
