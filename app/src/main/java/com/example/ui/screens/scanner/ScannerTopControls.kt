package com.example.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.TurquoisePrimary

@Composable
fun ScannerTopControls(
    isFixQrMode: Boolean,
    isFlashOn: Boolean,
    onCloseClick: () -> Unit,
    onModeChange: (Boolean) -> Unit,
    onFlashToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Close Button (X)
        Surface(
            onClick = onCloseClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.50f),
            modifier = Modifier
                .size(40.dp)
                .testTag("scanner_close_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Segmented Pill Mode Selector: [ 🔍 Arama ] [ 🏷️ QR Düzelt ]
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ARAMA / TARA MODU
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isFixQrMode) TurquoisePrimary else Color.Transparent)
                        .clickable { onModeChange(false) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Barkod Ara",
                        color = if (!isFixQrMode) Color.White else Color.White.copy(alpha = 0.70f),
                        fontWeight = if (!isFixQrMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // QR DÜZELTME MODU
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFixQrMode) TurquoisePrimary else Color.Transparent)
                        .clickable { onModeChange(true) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Etiket Düzelt",
                        color = if (isFixQrMode) Color.White else Color.White.copy(alpha = 0.70f),
                        fontWeight = if (isFixQrMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Flash Toggle Button (Right side)
        Surface(
            onClick = onFlashToggle,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.50f),
            modifier = Modifier
                .size(40.dp)
                .testTag("flash_toggle_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flaş",
                    tint = if (isFlashOn) SoonYellow else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
