package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TurquoisePrimary

val LocalTriggerGlobalLoading = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun SktPageLoadingOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skt_wave_loading")

    val offsetS by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 0)
        ),
        label = "wave_offset_s"
    )
    val offsetK by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 150)
        ),
        label = "wave_offset_k"
    )
    val offsetT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 300)
        ),
        label = "wave_offset_t"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .testTag("skt_page_loading_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "S",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = TurquoisePrimary,
                modifier = Modifier.offset(y = offsetS.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "K",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = TurquoisePrimary,
                modifier = Modifier.offset(y = offsetK.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "T",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = TurquoisePrimary,
                modifier = Modifier.offset(y = offsetT.dp)
            )
        }
    }
}
