package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate500
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import kotlin.math.roundToInt

@Composable
fun SktBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onScanClick: () -> Unit,
    userRoleCode: String = "MS"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Alt barı ekranın altına tam oturan, shadowElevation = 8.dp olan Surface
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sekme: "Ana Sayfa"
                BottomNavItem(
                    label = "Ana Sayfa",
                    activeIcon = Icons.Filled.SpaceDashboard,
                    inactiveIcon = Icons.Outlined.SpaceDashboard,
                    selected = currentRoute == "panel",
                    onClick = {
                        if (currentRoute == "panel") return@BottomNavItem
                        onNavigate("panel")
                    },
                    modifier = Modifier.weight(1f)
                )

                // 2. Sekme: "Ürünler"
                BottomNavItem(
                    label = "Ürünler",
                    activeIcon = Icons.Filled.Inventory2,
                    inactiveIcon = Icons.Outlined.Inventory2,
                    selected = currentRoute == "products",
                    onClick = {
                        if (currentRoute == "products") return@BottomNavItem
                        onNavigate("products")
                    },
                    modifier = Modifier.weight(1f)
                )

                // 3. Sekme (ORTA): "Tara" -> Yer tutucu ve tıklama alanı
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onScanClick
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "Tara",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TurquoisePrimary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // 4. Sekme: "İade Takip"
                BottomNavItem(
                    label = "İade Takip",
                    activeIcon = Icons.Filled.AssignmentTurnedIn,
                    inactiveIcon = Icons.Outlined.AssignmentTurnedIn,
                    selected = currentRoute == "takip" || currentRoute == "reports",
                    onClick = {
                        if (currentRoute == "takip" || currentRoute == "reports") return@BottomNavItem
                        onNavigate("takip")
                    },
                    modifier = Modifier.weight(1f)
                )

                // 5. Sekme: "Sayım"
                BottomNavItem(
                    label = "Sayım",
                    activeIcon = Icons.Filled.Checklist,
                    inactiveIcon = Icons.Outlined.Checklist,
                    selected = currentRoute == "adetsel",
                    onClick = {
                        if (currentRoute == "adetsel") return@BottomNavItem
                        onNavigate("adetsel")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Sekme (ORTA): "Tara" -> Yükseltilmiş, 52x52 dp boyutunda yuvarlak, turkuaz gradyanlı sabit buton
        Surface(
            onClick = onScanClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(2.dp, Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
                .size(52.dp)
                .testTag("scan_floating_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                TurquoiseLight,
                                TurquoisePrimary,
                                TurquoiseDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Kamera Barkod Tara",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor by animateColorAsState(
        targetValue = if (selected) TurquoisePrimary else Slate500,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_item_color"
    )

    val iconSize by animateDpAsState(
        targetValue = if (selected) 26.dp else 22.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_icon_size"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_icon_scale"
    )

    val capsuleColor by animateColorAsState(
        targetValue = if (selected) TurquoisePrimary.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_capsule_color"
    )

    val capsuleHorizontalPadding by animateDpAsState(
        targetValue = if (selected) 14.dp else 8.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_capsule_padding"
    )

    val fontWeightValue by animateFloatAsState(
        targetValue = if (selected) 700f else 500f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_font_weight"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(capsuleColor)
                .padding(horizontal = capsuleHorizontalPadding, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight(fontWeightValue.roundToInt().coerceIn(400, 800)),
            color = itemColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

