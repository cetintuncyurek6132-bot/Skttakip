package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Para birimi (₺), yüzde (%) ve dar kutulardaki metrik etiketlerinin
 * kutu dışına taşmadan, satır kırmadan ve okunaklı biçimde otomatik küçülerek sığmasını sağlar.
 */
@Composable
fun AutoSizeMetricText(
    text: String,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 8.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null
) {
    var currentFontSize by remember(text, maxFontSize, minFontSize) {
        mutableStateOf(maxFontSize)
    }
    var readyToDraw by remember(text, maxFontSize, minFontSize) {
        mutableStateOf(false)
    }

    Text(
        text = text,
        fontSize = currentFontSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        onTextLayout = { result ->
            val overflows = result.didOverflowWidth || result.hasVisualOverflow
            if (overflows && currentFontSize.value > minFontSize.value && result.size.width > 0) {
                val shrunk = (currentFontSize.value * 0.9f).coerceAtLeast(minFontSize.value)
                if (shrunk < currentFontSize.value - 0.05f) {
                    currentFontSize = shrunk.sp
                } else {
                    currentFontSize = minFontSize
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}
