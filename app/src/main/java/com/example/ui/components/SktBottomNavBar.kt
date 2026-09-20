package com.example.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate500
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary

@Composable
fun SktBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onScanClick: () -> Unit,
    userRoleCode: String = "MS"
) {
    val isReportsRestricted = userRoleCode !in listOf("MS", "MSY")

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
                        .fillMaxHeight(),
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
                    isRestricted = isReportsRestricted,
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
    modifier: Modifier = Modifier,
    isRestricted: Boolean = false
) {
    val alpha = if (isRestricted) 0.45f else 1.0f
    val itemColor = if (selected) TurquoisePrimary else Slate500.copy(alpha = alpha)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                enabled = !isRestricted,
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
                .background(if (selected) TurquoisePrimary.copy(alpha = 0.12f) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = itemColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
