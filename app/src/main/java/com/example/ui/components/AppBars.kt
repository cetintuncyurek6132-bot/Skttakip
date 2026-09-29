package com.example.ui.components

import com.example.R
import com.example.data.getDisplayCode
import com.example.data.getDisplayName
import com.example.data.matchesSearchQuery
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.ripple
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlue
import com.example.ui.theme.WarningBlueContainer
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SktTopAppBar(
    onBellClick: () -> Unit,
    unreadCount: Int,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    allProducts: List<Product> = emptyList(),
    onProductClick: (Product) -> Unit = {},
    onAddNewProductClick: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onRemindersClick: () -> Unit = onAvatarClick,
    onSettingsClick: () -> Unit = {},
    showHomeButton: Boolean = false,
    onHomeClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    var isSearchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearchExpanded = true
        }
    }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && searchQuery.isEmpty()) {
            focusManager.clearFocus(force = true)
        }
    }

    // 2 dakikada bir kendi ekseni etrafında dönen (3D Flip) mikro-animasyon
    val rotationY = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120_000L) // 2 dakika (120 saniye) bekle
            rotationY.animateTo(
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 900,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
            rotationY.snapTo(0f) // Tur bitince açıyı sıfırla, bir sonraki 2 dakikayı bekle
        }
    }

    val isDark = MaterialTheme.colorScheme.background == Slate900 || MaterialTheme.colorScheme.surface == Slate800

    Surface(
        color = if (isDark) Color(0xFF0F766E) else TurquoisePrimary,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SKT Logo Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SKT",
                    color = if (isDark) Color(0xFF2DD4BF) else TurquoiseDark,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.graphicsLayer {
                        this.rotationY = rotationY.value
                        cameraDistance = 12f * density
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Manuel Arama Yeri (Search Bar - Açılır / Kapanır)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clickable {
                        isSearchExpanded = true
                    }
                    .testTag("top_bar_search_field"),
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Manuel Ara",
                        tint = if (isDark) Color(0xFF94A3B8) else Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) searchQuery else "Ürün adı, barkod veya kod ara",
                        color = if (searchQuery.isNotEmpty()) {
                            if (isDark) Color(0xFFF1F5F9) else Slate900
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Slate500.copy(alpha = 0.65f)
                        },
                        fontSize = 12.sp,
                        fontWeight = if (searchQuery.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            onOpenScanner()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Barkod Tara",
                            tint = if (isDark) Color(0xFF2DD4BF) else TurquoisePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bell icon with modern notification badge
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                        Badge(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                IconButton(
                    onClick = onBellClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("bell_icon")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Bildirimler",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Settings Icon (Moved to top-bar)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayarlar & Veri Aktarımı",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    GlobalSearchDialog(
        isOpen = isSearchExpanded,
        onDismiss = { isSearchExpanded = false },
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        allProducts = allProducts,
        onProductClick = onProductClick,
        onAddNewProductClick = onAddNewProductClick
    )
}
