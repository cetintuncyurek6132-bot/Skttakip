package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.aspectRatio
import com.example.ui.theme.Slate200
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.util.TimeZone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.screens.DateOcrScannerDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SKT",
                    color = TurquoiseDark,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
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
                        text = if (searchQuery.isNotEmpty()) searchQuery else "Ad, kod veya barkod ile ara",
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

            // User Avatar Icon
            IconButton(
                onClick = onAvatarClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Kullanıcı Profili",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
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
                        .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                        .clickable(enabled = false) {}
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                                text = "Ad, kod veya barkod ile ara",
                                                color = Slate500,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange,
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = Slate900,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            cursorBrush = SolidColor(TurquoisePrimary),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
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

                            Spacer(modifier = Modifier.height(10.dp))

                            val queryTrim = searchQuery.trim().lowercase()
                            val matchingProducts = remember(allProducts, queryTrim) {
                                if (queryTrim.isEmpty()) {
                                    allProducts.take(15)
                                } else {
                                    allProducts.filter { prod ->
                                        prod.urunAdi.lowercase().contains(queryTrim) ||
                                        prod.barkod.lowercase().contains(queryTrim) ||
                                        prod.urunKodu.lowercase().contains(queryTrim) ||
                                        prod.kategori.lowercase().contains(queryTrim)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (queryTrim.isEmpty()) "TÜM ÜRÜNLER (${allProducts.size})" else "ARAMA SONUÇLARI (${matchingProducts.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoiseDark
                                )

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
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Eşleşen ürün bulunamadı.",
                                        fontSize = 13.sp,
                                        color = Slate500,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            isSearchExpanded = false
                                            onSearchQueryChange("")
                                            focusManager.clearFocus(force = true)
                                            onAddNewProductClick()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ YENİ ÜRÜN EKLE",
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
                                        .heightIn(max = 380.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(matchingProducts, key = { it.id }) { product ->
                                        val status = if (product.sktTarihi > 0L) product.getExpiryStatus() else ExpiryStatus.NORMAL
                                        val daysLeft = if (product.sktTarihi > 0L) product.getRemainingDays() else 9999L

                                        val statusColor = when (status) {
                                            ExpiryStatus.EXPIRED -> ExpiredRed
                                            ExpiryStatus.CRITICAL -> CriticalOrange
                                            ExpiryStatus.SOON -> SoonYellow
                                            ExpiryStatus.NORMAL -> NormalGreen
                                        }

                                        val statusLabel = if (product.sktTarihi <= 0L) "Tarihsiz"
                                        else when (status) {
                                            ExpiryStatus.EXPIRED -> "DOLDU"
                                            else -> "$daysLeft GÜN"
                                        }

                                        Surface(
                                            onClick = {
                                                isSearchExpanded = false
                                                onSearchQueryChange("")
                                                focusManager.clearFocus(force = true)
                                                onProductClick(product)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Slate50,
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(TurquoisePrimary.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = TurquoiseDark,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = product.urunAdi.uppercase(),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${product.kategori} • ${if (product.barkod.isNotBlank()) "Barkod: ${product.barkod}" else if (product.urunKodu.isNotBlank()) "Kod: ${product.urunKodu}" else "Kod yok"}",
                                                        fontSize = 11.sp,
                                                        color = Slate500,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(statusColor)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = statusLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Stok: ${product.stokAdedi}",
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
    val canAccessReports = userRoleCode == "MS" || userRoleCode == "MSY"
    val canAccessSettings = true

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. ANA SAYFA (ev ikonu)
            BottomNavItem(
                label = "ANA SAYFA",
                icon = Icons.Default.Home,
                selected = currentRoute == "panel",
                onClick = { onNavigate("panel") },
                modifier = Modifier.weight(1f)
            )

            // 2. ÜRÜNLER (liste ikonu)
            BottomNavItem(
                label = "ÜRÜNLER",
                icon = Icons.Default.List,
                selected = currentRoute == "products",
                onClick = { onNavigate("products") },
                modifier = Modifier.weight(1f)
            )

            // 3. ORTADA YÜKSELTİLMİŞ YUVARLAK TURKUAZ FLOATING BUTON (barkod/QR ikonu)
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onScanClick,
                    containerColor = TurquoisePrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .offset(y = (-8).dp)
                        .testTag("scan_floating_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Kamera Barkod Tara",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // 4. RAPORLAR
            BottomNavItem(
                label = "RAPORLAR",
                icon = Icons.Default.BarChart,
                selected = currentRoute == "reports",
                onClick = { onNavigate("reports") },
                modifier = Modifier.weight(1f),
                isRestricted = !canAccessReports
            )

            // 5. AYARLAR & CSV
            BottomNavItem(
                label = "AYARLAR",
                icon = Icons.Default.Settings,
                selected = currentRoute == "csv",
                onClick = { onNavigate("csv") },
                modifier = Modifier.weight(1f),
                isRestricted = !canAccessSettings
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRestricted: Boolean = false
) {
    val alpha = if (isRestricted) 0.5f else 1.0f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) TurquoisePrimary else Color.Gray.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) TurquoisePrimary else Color.Gray.copy(alpha = alpha)
        )
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(TurquoisePrimary, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun ExpiryStatusChip(
    status: ExpiryStatus,
    daysLeft: Long,
    modifier: Modifier = Modifier
) {
    val (bg, textColor, labelText) = when (status) {
        ExpiryStatus.EXPIRED -> Triple(
            ExpiredRedContainer,
            ExpiredRed,
            "SÜRESİ GEÇEN (${daysLeft} GÜN)"
        )

        ExpiryStatus.CRITICAL -> Triple(
            CriticalOrangeContainer,
            CriticalOrange,
            "KRİTİK (${daysLeft} GÜN)"
        )

        ExpiryStatus.SOON -> Triple(
            SoonYellowContainer,
            Color(0xFFB78103),
            "YAKIN (${daysLeft} GÜN)"
        )

        ExpiryStatus.NORMAL -> Triple(
            NormalGreenContainer,
            NormalGreen,
            "NORMAL (${daysLeft} GÜN)"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.5.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = labelText,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProductListItemCard(
    product: Product,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val hasSkt = product.sktTarihi > 0L
    val daysLeft = if (hasSkt) product.getRemainingDays() else 9999L
    val status = if (hasSkt) product.getExpiryStatus() else ExpiryStatus.NORMAL

    val squareBg: Color = if (!hasSkt) {
        Slate500
    } else {
        when (status) {
            ExpiryStatus.EXPIRED -> ExpiredRed
            ExpiryStatus.CRITICAL -> CriticalOrange
            ExpiryStatus.SOON -> SoonYellow
            ExpiryStatus.NORMAL -> NormalGreen
        }
    }

    val squareTextColor: Color = if (!hasSkt) {
        Color.White
    } else {
        when (status) {
            ExpiryStatus.SOON -> Color.Black
            else -> Color.White
        }
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR"))
    val sktString = if (hasSkt) dateFormat.format(Date(product.sktTarihi)) else "SKT Girilmedi"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_item_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Square badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(squareBg),
                contentAlignment = Alignment.Center
            ) {
                if (!hasSkt) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SKT",
                            color = squareTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "YOK",
                            color = squareTextColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = daysLeft.toString(),
                            color = squareTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "GÜN",
                            color = squareTextColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.urunAdi.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${product.kategori} • Stok: ${product.stokAdedi} adet",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    product.getFormattedPrice()?.let { formattedPrice ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0F2FE))
                                .border(0.5.dp, Color(0xFF0284C7), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }
                    if ((hasSkt && daysLeft in 1..30 && product.stokAdedi >= 10) || product.isImportant) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CriticalOrangeContainer)
                                .border(0.5.dp, CriticalOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔥 ÖNEMLİ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = CriticalOrange
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val subInfo = if (product.urunKodu.isNotBlank()) "SKT: $sktString • Kod: ${product.urunKodu}" else "SKT: $sktString • Barkod: ${product.barkod}"
                Text(
                    text = subInfo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right arrow or delete
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = ExpiredRed
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Detay",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GroupedProductListItemCard(
    productList: List<Product>,
    onClick: (Product) -> Unit,
    onDeleteProduct: ((Product) -> Unit)? = null
) {
    if (productList.isEmpty()) return
    val mainProduct = productList.first()
    val sortedList = remember(productList) {
        productList.sortedBy { if (it.sktTarihi > 0L) it.sktTarihi else Long.MAX_VALUE }
    }
    val totalStock = productList.sumOf { it.stokAdedi }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(sortedList.first()) }
            .testTag("grouped_product_item_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Çoklu SKT",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mainProduct.urunAdi.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val groupSubInfo = if (mainProduct.urunKodu.isNotBlank()) "${mainProduct.kategori} • Kod: ${mainProduct.urunKodu}" else "${mainProduct.kategori} • Barkod: ${mainProduct.barkod}"
                    Text(
                        text = groupSubInfo,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(horizontalAlignment = Alignment.End) {
                    mainProduct.getFormattedPrice()?.let { formattedPrice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0F2FE))
                                .border(0.5.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate900)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Toplam: $totalStock Adet",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${sortedList.size} SKT Tarihi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TurquoiseDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-header text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SKT VE ADETLERİ (${sortedList.size}):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate500
                )
                Text(
                    text = "Sağa Kaydır ➔",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal Scrollable Cards/Chips for SKT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedList.forEach { item ->
                    val hasSkt = item.sktTarihi > 0L
                    val daysLeft = if (hasSkt) item.getRemainingDays() else 9999L
                    val status = if (hasSkt) item.getExpiryStatus() else ExpiryStatus.NORMAL

                    val sktBgColor: Color
                    val sktBorderColor: Color
                    val sktTextColor: Color
                    val badgeBgColor: Color
                    val badgeTextColor: Color
                    val statusLabel: String

                    if (!hasSkt) {
                        sktBgColor = Slate50
                        sktBorderColor = Slate300
                        sktTextColor = Slate700
                        badgeBgColor = Slate500
                        badgeTextColor = Color.White
                        statusLabel = "SKT YOK"
                    } else {
                        when (status) {
                            ExpiryStatus.EXPIRED -> {
                                sktBgColor = ExpiredRedContainer
                                sktBorderColor = ExpiredRed
                                sktTextColor = ExpiredRed
                                badgeBgColor = ExpiredRed
                                badgeTextColor = Color.White
                                statusLabel = "DOLDU"
                            }
                            ExpiryStatus.CRITICAL -> {
                                sktBgColor = CriticalOrangeContainer
                                sktBorderColor = CriticalOrange
                                sktTextColor = CriticalOrange
                                badgeBgColor = CriticalOrange
                                badgeTextColor = Color.White
                                statusLabel = "$daysLeft GÜN"
                            }
                            ExpiryStatus.SOON -> {
                                sktBgColor = SoonYellowContainer
                                sktBorderColor = SoonYellow
                                sktTextColor = Color(0xFF8B6B00)
                                badgeBgColor = SoonYellow
                                badgeTextColor = Color.Black
                                statusLabel = "$daysLeft GÜN"
                            }
                            ExpiryStatus.NORMAL -> {
                                sktBgColor = NormalGreenContainer
                                sktBorderColor = NormalGreen
                                sktTextColor = NormalGreen
                                badgeBgColor = NormalGreen
                                badgeTextColor = Color.White
                                statusLabel = "$daysLeft GÜN"
                            }
                        }
                    }

                    Surface(
                        onClick = { onClick(item) },
                        shape = RoundedCornerShape(10.dp),
                        color = sktBgColor,
                        border = BorderStroke(1.dp, sktBorderColor),
                        modifier = Modifier.testTag("skt_chip_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBgColor)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeTextColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                val sktStr = if (hasSkt) dateFormat.format(Date(item.sktTarihi)) else "Tarihsiz"
                                Text(
                                    text = "SKT: $sktStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Adet: ${item.stokAdedi}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = sktTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductModal(
    product: Product?,
    prefilledBarcode: String,
    onDismiss: () -> Unit,
    onSave: (
        barkod: String,
        urunKodu: String,
        urunAdi: String,
        kategori: String,
        sktTarihi: Long,
        stokAdedi: Int,
        isNewSkt: Boolean,
        fiyat: Double?,
        isImportant: Boolean
    ) -> Unit,
    onDelete: ((Product) -> Unit)? = null
) {
    var barkod by remember { mutableStateOf(product?.barkod ?: prefilledBarcode) }
    var urunKodu by remember { mutableStateOf(product?.urunKodu ?: "2500${(1000..9999).random()}") }
    var urunAdi by remember { mutableStateOf(product?.urunAdi ?: "") }
    var kategori by remember { mutableStateOf(product?.kategori ?: "Gıda Ürünleri") }
    var fiyatText by remember {
        mutableStateOf(
            product?.fiyat?.let { f ->
                if (f % 1.0 == 0.0) f.toInt().toString() else f.toString()
            } ?: ""
        )
    }
    var isImportant by remember { mutableStateOf(product?.isImportant ?: false) }

    val categories = listOf(
        "Dolap Ürünleri",
        "Gıda Ürünleri",
        "Süt & Şarküteri",
        "Atıştırmalık",
        "İçecek",
        "Temel Gıda",
        "Temizlik"
    )
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product == null) "YENİ ÜRÜN KARTİ EKLE" else "ÜRÜN BİLGİLERİNİ DÜZENLE",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TurquoiseDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info banner explaining SKT separation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TurquoisePrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ️",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bu ekranda sadece Ana Ürün Bilgileri (Barkod, İsim, Kategori) yönetilir. SKT tarihleri ve miktarları Barkod Okuyucu veya 'Yeni SKT Ekle' butonundan ayrı olarak kaydedilir.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barcode input
                OutlinedTextField(
                    value = barkod,
                    onValueChange = { barkod = it },
                    label = { Text("Barkod No (Örn: 8690526010011)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_barkod"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = TurquoisePrimary,
                        focusedLabelColor = TurquoiseDark,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Product Code
                OutlinedTextField(
                    value = urunKodu,
                    onValueChange = { urunKodu = it },
                    label = { Text("Ürün Kodu (Örn: 25001234)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = TurquoisePrimary,
                        focusedLabelColor = TurquoiseDark,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Product Name
                OutlinedTextField(
                    value = urunAdi,
                    onValueChange = { urunAdi = it },
                    label = { Text("Ürün Adı (Örn: BENİMO KOMBO)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_urun_adi"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = TurquoisePrimary,
                        focusedLabelColor = TurquoiseDark,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = kategori,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = TurquoisePrimary,
                            focusedLabelColor = TurquoiseDark,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    kategori = cat
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price Input (Optional)
                OutlinedTextField(
                    value = fiyatText,
                    onValueChange = { fiyatText = it },
                    label = { Text("Fiyat (₺) (İsteğe Bağlı)") },
                    placeholder = { Text("Örn: 45.50") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fiyat"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = TurquoisePrimary,
                        focusedLabelColor = TurquoiseDark,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Önemli Ürün Switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isImportant) CriticalOrange else Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Önemli Ürün Olarak İşaretle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Her zaman Önemli listesinde gösterilir",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Switch(
                            checked = isImportant,
                            onCheckedChange = { isImportant = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CriticalOrange
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product != null && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(product) },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpiredRed),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SİL", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (barkod.isNotBlank() && urunAdi.isNotBlank()) {
                                val sktTime = product?.sktTarihi ?: 0L
                                val stok = product?.stokAdedi ?: 0
                                val parsedFiyat = fiyatText.trim().replace(',', '.').toDoubleOrNull()
                                onSave(barkod, urunKodu, urunAdi, kategori, sktTime, stok, false, parsedFiyat, isImportant)
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_product_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (product != null) "BİLGİLERİ GÜNCELLE" else "ÜRÜNÜ KAYDET",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailModal(
    product: Product?,
    matchingProducts: List<Product>,
    onDismiss: () -> Unit,
    onEditClick: (Product) -> Unit,
    onAddNewSktClick: (Product) -> Unit,
    onEditSktItem: (product: Product, sktTarihi: Long, stokAdedi: Int) -> Unit = { _, _, _ -> },
    onDeleteSkt: (Product) -> Unit
) {
    if (product == null) return

    var editingSktItem by remember { mutableStateOf<Product?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR")) }

    // Risk Level Quantity Breakdown
    val expiredOrNearCount = remember(matchingProducts) {
        matchingProducts.filter {
            if (it.sktTarihi <= 0L) false else {
                val days = (it.sktTarihi - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                days <= 3
            }
        }.sumOf { it.stokAdedi }
    }
    val criticalCount = remember(matchingProducts) {
        matchingProducts.filter {
            if (it.sktTarihi <= 0L) false else {
                val days = (it.sktTarihi - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                days in 4..15
            }
        }.sumOf { it.stokAdedi }
    }
    val safeCount = remember(matchingProducts) {
        matchingProducts.filter {
            if (it.sktTarihi <= 0L) false else {
                val days = (it.sktTarihi - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                days >= 16
            }
        }.sumOf { it.stokAdedi }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, TurquoisePrimary),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ürün Detay",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = TurquoiseDark
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onEditClick(product) },
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("detail_edit_product_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Düzenle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_detail_modal_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO CARD WITH PRODUCT NAME & ICON
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TurquoiseDark,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = product.urunAdi.take(1).uppercase(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.urunAdi,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TurquoisePrimary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "📁 ${product.kategori}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TurquoiseDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = "🏷️ Kod: ${product.urunKodu}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    val priceText = product.getFormattedPrice()
                                    if (priceText != null) {
                                        Text(
                                            text = priceText,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF16A34A)
                                        )
                                    } else {
                                        Text(
                                            text = "Fiyat Yok",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Barkod: ${product.barkod}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // SKT RISK LEVEL BREAKDOWN (Red, Orange, Green Quantity Counters)
                    Text(
                        text = "📊 SKT MİKTAR & RİSK DAĞILIMI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // RED: EXPIRED / NEAR (0-3 Days)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ExpiredRedContainer,
                            border = BorderStroke(1.5.dp, ExpiredRed.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔴 Geçmiş/Yakın", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ExpiredRed)
                                Text("(0-3 Gün)", fontSize = 9.sp, color = ExpiredRed.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$expiredOrNearCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ExpiredRed)
                                Text("SKT Adedi", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ExpiredRed)
                            }
                        }

                        // ORANGE: CRITICAL (4-15 Days)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CriticalOrangeContainer,
                            border = BorderStroke(1.5.dp, CriticalOrange.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🟠 Kritik", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CriticalOrange)
                                Text("(4-15 Gün)", fontSize = 9.sp, color = CriticalOrange.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$criticalCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = CriticalOrange)
                                Text("SKT Adedi", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CriticalOrange)
                            }
                        }

                        // GREEN: SAFE (16+ Days)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NormalGreenContainer,
                            border = BorderStroke(1.5.dp, NormalGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🟢 Güvende", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NormalGreen)
                                Text("(16+ Gün)", fontSize = 9.sp, color = NormalGreen.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$safeCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NormalGreen)
                                Text("SKT Adedi", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NormalGreen)
                            }
                        }
                    }

                    // REGISTERED SKT DATES LIST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 KAYITLI SKT TARİHLERİ (${matchingProducts.size} TARİH)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matchingProducts.forEach { item ->
                            val hasSkt = item.sktTarihi > 0L
                            val now = System.currentTimeMillis()
                            val diff = if (hasSkt) item.sktTarihi - now else 0L
                            val daysRemaining = diff / (1000 * 60 * 60 * 24)
                            val dateStr = if (hasSkt) dateFormat.format(Date(item.sktTarihi)) else "SKT Girilmedi"

                            val (badgeText, badgeBg, badgeTextColor) = if (!hasSkt) {
                                Triple(
                                    "ℹ️ SKT YOK",
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else when {
                                daysRemaining < 0 -> Triple(
                                    "🚨 GEÇTİ (${kotlin.math.abs(daysRemaining)}g)",
                                    ExpiredRedContainer,
                                    ExpiredRed
                                )
                                daysRemaining in 0..7 -> Triple(
                                    "⚠️ KRİTİK (${daysRemaining}g)",
                                    CriticalOrangeContainer,
                                    CriticalOrange
                                )
                                else -> Triple(
                                    "✅ NORMAL (${daysRemaining}g)",
                                    NormalGreenContainer,
                                    NormalGreen
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TurquoiseDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeBg
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeTextColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "SKT Miktarı: ${item.stokAdedi} adet",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TurquoiseDark
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingSktItem = item },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "SKT/Adet Düzenle",
                                                tint = TurquoiseDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteSkt(item) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "SKT Sil",
                                                tint = ExpiredRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Button (Add SKT)
                Button(
                    onClick = { onAddNewSktClick(product) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("detail_add_new_skt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "SKT ve Adet Ekle",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SKT VE ADET EKLE",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (editingSktItem != null) {
        AddSktModal(
            product = editingSktItem!!,
            isEditMode = true,
            onDismiss = { editingSktItem = null },
            onSaveSkt = { prod, newSkt, newCount ->
                onEditSktItem(prod, newSkt, newCount)
                editingSktItem = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSktModal(
    product: Product,
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onSaveSkt: (product: Product, sktTarihi: Long, stokAdedi: Int) -> Unit
) {
    var stokAdedi by remember(product) {
        mutableStateOf(if (isEditMode && product.stokAdedi > 0) product.stokAdedi.toString() else "1")
    }
    val initialDateMillis = remember(product) {
        if (isEditMode && product.sktTarihi > 0L) product.sktTarihi else System.currentTimeMillis()
    }
    var selectedDateMillis by remember(initialDateMillis) { mutableStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showOcrScanner by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR"))

    if (showOcrScanner) {
        DateOcrScannerDialog(
            onDismiss = { showOcrScanner = false },
            onDateDetected = { dateMillis, _ ->
                selectedDateMillis = dateMillis
                showOcrScanner = false
            }
        )
    }

    if (showDatePicker) {
        CustomBoxedCalendarDialog(
            initialDateMillis = selectedDateMillis,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedMillis ->
                selectedDateMillis = selectedMillis
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, TurquoisePrimary),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "✏️ SKT VE ADET DÜZENLE" else "➕ YENİ SKT TARİHİ EKLE",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TurquoiseDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Header Info Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = product.urunAdi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Barkod: ${product.barkod} | Kategori: ${product.kategori}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker field & OCR Camera button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(selectedDateMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Yeni SKT Tarihi") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Tarih Seç",
                                tint = TurquoiseDark
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                            .testTag("input_new_skt_date"),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = TurquoisePrimary,
                            disabledLabelColor = TurquoiseDark,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTrailingIconColor = TurquoiseDark,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Button(
                        onClick = { showOcrScanner = true },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Kamera ile Tarih Tara",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OCR TARA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SKT Adedi (Label + Stepper + Compact Text Field)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SKT Adedi (Giriş Miktarı):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val current = stokAdedi.toIntOrNull() ?: 0
                                if (current > 1) stokAdedi = (current - 1).toString()
                            },
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedTextField(
                            value = stokAdedi,
                            onValueChange = { stokAdedi = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = TurquoisePrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Button(
                            onClick = {
                                val current = stokAdedi.toIntOrNull() ?: 0
                                stokAdedi = (current + 1).toString()
                            },
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val context = LocalContext.current
                    if (!isEditMode) {
                        Button(
                            onClick = {
                                val count = stokAdedi.toIntOrNull() ?: 1
                                onSaveSkt(product, selectedDateMillis, count)
                                Toast.makeText(context, "✓ SKT eklendi ($count adet)", Toast.LENGTH_SHORT).show()
                                stokAdedi = "1"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("submit_add_skt_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ EKLE",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val count = stokAdedi.toIntOrNull() ?: 1
                            onSaveSkt(product, selectedDateMillis, count)
                            val msg = if (isEditMode) "✓ SKT güncellendi ($count adet)" else "✓ Kaydedildi"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_and_close_skt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isEditMode) TurquoisePrimary else Slate900),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Kaydet", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "DÜZENLEMEYİ KAYDET" else "KAYDET",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBoxedCalendarDialog(
    initialDateMillis: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    var selectedMillis by remember { mutableStateOf(initialDateMillis) }

    var calendarView by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = if (initialDateMillis > 0) initialDateMillis else System.currentTimeMillis()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val currentYear = calendarView.get(Calendar.YEAR)
    val currentMonth = calendarView.get(Calendar.MONTH)

    val trLocale = remember { Locale("tr", "TR") }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", trLocale) }
    val selectedDateFormat = remember { SimpleDateFormat("dd MMMM yyyy, EEEE", trLocale) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = TurquoiseDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "SON KULLANMA TARİHİ SEÇİN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedMillis > 0) selectedDateFormat.format(Date(selectedMillis)) else "Tarih Seçin",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val nextCal = calendarView.clone() as Calendar
                            nextCal.add(Calendar.MONTH, -1)
                            calendarView = nextCal
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Önceki Ay",
                            tint = TurquoisePrimary
                        )
                    }

                    Text(
                        text = monthYearFormat.format(calendarView.time).uppercase(trLocale),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            val nextCal = calendarView.clone() as Calendar
                            nextCal.add(Calendar.MONTH, 1)
                            calendarView = nextCal
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Sonraki Ay",
                            tint = TurquoisePrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysOfWeek = listOf("PZT", "SAL", "ÇAR", "PER", "CUM", "CMT", "PAZ")
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val tempCal = calendarView.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                val emptySlotsBefore = (dayOfWeek + 5) % 7
                val maxDaysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val totalGridCells = emptySlotsBefore + maxDaysInMonth
                val totalRows = (totalGridCells + 6) / 7

                Column(
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    for (rowIndex in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (colIndex in 0 until 7) {
                                val cellIndex = rowIndex * 7 + colIndex
                                val dayNumber = cellIndex - emptySlotsBefore + 1

                                if (dayNumber in 1..maxDaysInMonth) {
                                    val cellCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, currentYear)
                                        set(Calendar.MONTH, currentMonth)
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val cellMillis = cellCal.timeInMillis

                                    val cellMidnight = cellCal.clone() as Calendar
                                    cellMidnight.set(Calendar.HOUR_OF_DAY, 0)
                                    val isPast = cellMidnight.timeInMillis < todayMidnight

                                    val isSelected = isSameDay(selectedMillis, cellMillis)
                                    val isToday = isSameDay(todayMidnight, cellMillis)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.5.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> TurquoisePrimary
                                                    isToday -> TurquoisePrimary.copy(alpha = 0.25f)
                                                    isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    dayNumber % 2 != 0 -> TurquoisePrimary.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .border(
                                                width = if (isSelected || isToday) 2.dp else 1.dp,
                                                color = when {
                                                    isSelected -> TurquoiseDark
                                                    isToday -> TurquoisePrimary
                                                    isPast -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                    dayNumber % 2 != 0 -> TurquoisePrimary.copy(alpha = 0.45f)
                                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = !isPast) {
                                                selectedMillis = cellMillis
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = when {
                                                isSelected -> Color.White
                                                isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                isToday -> TurquoiseDark
                                                dayNumber % 2 != 0 -> TurquoiseDark
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "VAZGEÇ",
                            color = Slate700,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(selectedMillis)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "TAMAM",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    if (millis1 <= 0 || millis2 <= 0) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun TopBarLoadingBar(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = TurquoisePrimary,
            trackColor = TurquoisePrimary.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ProfessionalLoadingOverlay(
    isLoading: Boolean,
    title: String = "Veriler Yükleniyor",
    message: String = "Lütfen bekleyiniz, işlem gerçekleştiriliyor..."
) {
    if (!isLoading) return

    val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.5.dp, TurquoisePrimary.copy(alpha = 0.3f)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = TurquoisePrimary,
                        strokeWidth = 4.5.dp,
                        trackColor = TurquoisePrimary.copy(alpha = 0.15f)
                    )

                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Yükleniyor",
                        tint = TurquoiseDark,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(rotation)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun InlineLoadingCard(
    title: String = "Veriler Yükleniyor...",
    subtitle: String = "Veritabanı ve bulut verileri işleniyor",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "inline_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "inline_rotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = TurquoisePrimary,
                    strokeWidth = 3.5.dp,
                    trackColor = TurquoisePrimary.copy(alpha = 0.15f)
                )
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = TurquoiseDark,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
    }
}
