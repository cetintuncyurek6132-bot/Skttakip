package com.example.ui.components

import com.example.R
import com.example.data.getDisplayCode
import com.example.data.getDisplayName
import com.example.data.matchesSearchQuery
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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

    Surface(
        color = TurquoisePrimary,
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
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_skt_shield_logo),
                    contentDescription = "SKT Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SKT",
                    color = TurquoiseDark,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp
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
                color = Color.White,
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
                        tint = Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) searchQuery else "Ürün adı, barkod veya kod ara",
                        color = if (searchQuery.isNotEmpty()) Slate900 else Slate500.copy(alpha = 0.65f),
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
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bell icon with red number badge
            IconButton(
                onClick = onBellClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("bell_icon")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = ExpiredRed,
                                contentColor = Color.White
                            ) {
                                Text(text = unreadCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Bildirimler",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
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

    if (isSearchExpanded) {
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        Popup(
            onDismissRequest = {
                isSearchExpanded = false
                onSearchQueryChange("")
                focusManager.clearFocus(force = true)
            },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        isSearchExpanded = false
                        onSearchQueryChange("")
                        focusManager.clearFocus(force = true)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .clickable(enabled = false) {}
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Active Search Input Field inside Popup
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Ara",
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Ürün adı, barkod veya kod ara",
                                                color = Slate500,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { onSearchQueryChange(it.uppercase(java.util.Locale.forLanguageTag("tr-TR"))) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = Slate900,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            cursorBrush = SolidColor(TurquoisePrimary),
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Characters,
                                                imeAction = ImeAction.Search
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    keyboardController?.hide()
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (searchQuery.isNotEmpty()) {
                                                onSearchQueryChange("")
                                            } else {
                                                isSearchExpanded = false
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Kapat",
                                            tint = Slate700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val queryTrim = searchQuery.trim()
                            val matchingProducts = remember(allProducts, queryTrim) {
                                if (queryTrim.isEmpty()) {
                                    allProducts
                                } else {
                                    allProducts.filter { prod ->
                                        prod.matchesSearchQuery(queryTrim)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (queryTrim.isEmpty()) Icons.Default.Inventory2 else Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (queryTrim.isEmpty()) "TÜM ÜRÜNLER (${allProducts.size})" else "ARAMA SONUÇLARI (${matchingProducts.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TurquoiseDark
                                    )
                                }

                                Text(
                                    text = "Yazdıkça filtrelenir",
                                    fontSize = 10.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (matchingProducts.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Slate100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = Slate500,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Aramanızla eşleşen ürün bulunamadı",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"$searchQuery\" araması için kayıtlı ürün bulunmuyor. Dilerseniz yeni ürün ekleyebilirsiniz.",
                                        fontSize = 12.sp,
                                        color = Slate500,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            isSearchExpanded = false
                                            onSearchQueryChange("")
                                            focusManager.clearFocus(force = true)
                                            onAddNewProductClick()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "YENİ ÜRÜN EKLE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp)
                                        .weight(1f, fill = false),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(matchingProducts, key = { it.id }) { product ->
                                        val status = if (product.sktTarihi > 0L) product.getExpiryStatus() else ExpiryStatus.NORMAL
                                        val daysLeft = if (product.sktTarihi > 0L) product.getRemainingDays() else 9999L

                                        val (statusBg, statusText, statusLabel) = when {
                                            product.sktTarihi <= 0L -> Triple(Slate100, Slate700, "TARİHSİZ")
                                            status == ExpiryStatus.EXPIRED -> Triple(ExpiredRedContainer, ExpiredRed, "SÜRESİ GEÇTİ")
                                            status == ExpiryStatus.CRITICAL -> Triple(CriticalOrangeContainer, CriticalOrange, if (daysLeft == 0L) "BUGÜN" else "$daysLeft GÜN")
                                            status == ExpiryStatus.SOON -> Triple(SoonYellowContainer, Color(0xFF8B6B00), "$daysLeft GÜN")
                                            status == ExpiryStatus.WARNING -> Triple(WarningBlueContainer, WarningBlue, "$daysLeft GÜN")
                                            else -> Triple(NormalGreenContainer, NormalGreen, "$daysLeft GÜN")
                                        }

                                        Surface(
                                            onClick = {
                                                isSearchExpanded = false
                                                onSearchQueryChange("")
                                                focusManager.clearFocus(force = true)
                                                onProductClick(product)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Slate50,
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val displayName = product.getDisplayName()
                                                val displayCode = product.getDisplayCode()
                                                val firstChar = displayName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = firstChar,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = TurquoiseDark
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = displayName.uppercase(),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val infoText = buildString {
                                                        append(if (product.kategori.isNotBlank()) product.kategori else "Genel")
                                                        val barcode = product.barkod.trim()
                                                        if (barcode.isNotBlank()) {
                                                            append(" • Barkod: ")
                                                            append(barcode)
                                                        }
                                                        if (displayCode.isNotBlank() && displayCode != barcode && displayCode != displayName) {
                                                            append(" • Kod: ")
                                                            append(displayCode)
                                                        }
                                                    }
                                                    Text(
                                                        text = infoText,
                                                        fontSize = 11.sp,
                                                        color = Slate500,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(statusBg)
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = statusLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = statusText
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    Text(
                                                        text = "Adet: ${product.stokAdedi}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate700
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.5.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. ANA SAYFA (Modern Dashboard)
                    BottomNavItem(
                        label = "Ana Sayfa",
                        activeIcon = Icons.Filled.SpaceDashboard,
                        inactiveIcon = Icons.Outlined.SpaceDashboard,
                        selected = currentRoute == "panel",
                        onClick = { onNavigate("panel") },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. ÜRÜNLER (Modern Envanter / Reyon)
                    BottomNavItem(
                        label = "Ürünler",
                        activeIcon = Icons.Filled.Inventory2,
                        inactiveIcon = Icons.Outlined.Inventory2,
                        selected = currentRoute == "products",
                        onClick = { onNavigate("products") },
                        modifier = Modifier.weight(1f)
                    )

                    // 3. ORTA ALAN: TARA ETİKETİ VE YERLEŞİM (Yükseltilmiş Butonun Altı)
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onScanClick() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Tara",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoisePrimary,
                            letterSpacing = 0.3.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // 4. TAKİP (İade & Depo Süreç Takibi)
                    val isReportsRestricted = userRoleCode !in listOf("MS", "MSY")
                    BottomNavItem(
                        label = "İade Takip",
                        activeIcon = Icons.Filled.AssignmentTurnedIn,
                        inactiveIcon = Icons.Outlined.AssignmentTurnedIn,
                        selected = currentRoute == "takip" || currentRoute == "reports",
                        onClick = { onNavigate("takip") },
                        isRestricted = isReportsRestricted,
                        modifier = Modifier.weight(1f)
                    )

                    // 5. ADETSEL SAYIM (Doğrulama & Liste Kontrolü)
                    BottomNavItem(
                        label = "Sayım",
                        activeIcon = Icons.Filled.Checklist,
                        inactiveIcon = Icons.Outlined.Checklist,
                        selected = currentRoute == "adetsel",
                        onClick = { onNavigate("adetsel") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // YÜKSELTİLMİŞ (ELEVATED / FLOATING DOCK) BARKOD & KAMERA LOGOSU
        Surface(
            onClick = onScanClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(56.dp)
                .testTag("scan_floating_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                TurquoiseLight,
                                TurquoisePrimary,
                                TurquoiseDark
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.45f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Kamera Barkod Tara",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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
    val iconColor by animateColorAsState(
        targetValue = if (selected) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f * alpha),
        label = "bottom_nav_icon_color"
    )
    val pillBgColor by animateColorAsState(
        targetValue = if (selected) TurquoisePrimary.copy(alpha = 0.14f) else Color.Transparent,
        label = "bottom_nav_pill_bg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isRestricted) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Modern Pill Container
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(pillBgColor)
                .padding(horizontal = 14.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = iconColor,
            letterSpacing = 0.2.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Active Glowing Line Indicator
        Box(
            modifier = Modifier
                .width(if (selected) 16.dp else 0.dp)
                .height(2.5.dp)
                .background(
                    if (selected) TurquoisePrimary else Color.Transparent,
                    RoundedCornerShape(1.5.dp)
                )
        )
    }
}
