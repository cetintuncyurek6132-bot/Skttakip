package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.ui.ProductFilter
import com.example.ui.ProductGroupFilter
import com.example.ui.components.CustomBoxedCalendarDialog
import com.example.ui.components.GroupedProductListItemCard
import com.example.ui.components.ProductListItemCard
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProductsScreen(
    products: List<Product>,
    searchQuery: String,
    selectedFilter: ProductFilter,
    selectedGroupFilter: ProductGroupFilter = ProductGroupFilter.ALL,
    startDateFilter: Long? = null,
    endDateFilter: Long? = null,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (ProductFilter) -> Unit,
    onGroupFilterSelect: (ProductGroupFilter) -> Unit = {},
    onDateRangeSelect: (start: Long?, end: Long?) -> Unit = { _, _ -> },
    onClearDateRange: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddProductClick: () -> Unit,
    onOpenQrFixMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDateRangeModalOpen by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

    val hasDateFilter = startDateFilter != null || endDateFilter != null

    if (isDateRangeModalOpen) {
        DateRangePickerModal(
            initialStart = startDateFilter,
            initialEnd = endDateFilter,
            onDismiss = { isDateRangeModalOpen = false },
            onApply = { start, end ->
                onDateRangeSelect(start, end)
                isDateRangeModalOpen = false
            },
            onClear = {
                onClearDateRange()
                isDateRangeModalOpen = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Search and Filters bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Horizontal filter chips & Date Range button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 📅 Tarih Aralığı Filtresi Butonu
                    Surface(
                        onClick = { isDateRangeModalOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasDateFilter) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (hasDateFilter) TurquoiseDark else Color.Transparent),
                        modifier = Modifier.testTag("date_range_filter_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Tarih Filtresi",
                                tint = if (hasDateFilter) Color.White else TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hasDateFilter) {
                                    val startStr = startDateFilter?.let { dateFormat.format(Date(it)) } ?: "..."
                                    val endStr = endDateFilter?.let { dateFormat.format(Date(it)) } ?: "..."
                                    "$startStr - $endStr"
                                } else "SKT Tarih Aralığı",
                                color = if (hasDateFilter) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasDateFilter) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Filtreyi Temizle",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onClearDateRange() }
                                )
                            }
                        }
                    }

                    // 🏷️ QR ile Barkod & Fiyat Düzelt Butonu
                    Surface(
                        onClick = onOpenQrFixMode,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, TurquoisePrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR Düzelt",
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "🏷️ QR Düzelt",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Kategori/Durum Filtre Chip'leri
                    ProductFilter.values().forEach { filter ->
                        FilterChipItem(
                            label = filter.label,
                            isSelected = selectedFilter == filter,
                            onClick = { onFilterSelect(filter) }
                        )
                    }
                }

                // 🧊 Dolap & 🍞 Gıda Sıralama / Filtreleme Butonları (Tümü kategorisi altında)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sol Buton: Dolap
                    Surface(
                        onClick = {
                            onGroupFilterSelect(
                                if (selectedGroupFilter == ProductGroupFilter.DOLAP) ProductGroupFilter.ALL
                                else ProductGroupFilter.DOLAP
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("group_button_dolap"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedGroupFilter == ProductGroupFilter.DOLAP) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selectedGroupFilter == ProductGroupFilter.DOLAP) TurquoiseDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "🧊 Dolap",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedGroupFilter == ProductGroupFilter.DOLAP) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Sağ Buton: Gıda
                    Surface(
                        onClick = {
                            onGroupFilterSelect(
                                if (selectedGroupFilter == ProductGroupFilter.GIDA) ProductGroupFilter.ALL
                                else ProductGroupFilter.GIDA
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("group_button_gida"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedGroupFilter == ProductGroupFilter.GIDA) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selectedGroupFilter == ProductGroupFilter.GIDA) TurquoiseDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "🍞 Gıda",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedGroupFilter == ProductGroupFilter.GIDA) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Active Date Range info banner if set
        if (hasDateFilter) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = TurquoisePrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(text = "🔍", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        val sStr = startDateFilter?.let { dateFormat.format(Date(it)) } ?: "Herhangi"
                        val eStr = endDateFilter?.let { dateFormat.format(Date(it)) } ?: "Herhangi"
                        Text(
                            text = "SKT Tarih Aralığı Filtresi Aktif: $sStr - $eStr (${products.size} ürün)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                    }
                    Text(
                        text = "Temizle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Red,
                        modifier = Modifier
                            .clickable { onClearDateRange() }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Products list or empty state
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "📦",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Eşleşen Ürün Bulunamadı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasDateFilter) "Seçilen SKT tarih aralığında ürün bulunamadı. Filtreyi temizleyebilir veya başka bir tarih aralığı seçebilirsiniz."
                        else "Arama kriterini değiştirebilir, aşağıdaki kamera butonuyla barkod tarayabilir veya elle ekleyebilirsiniz.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (hasDateFilter) {
                        Button(
                            onClick = { onClearDateRange() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("TARIH FILTRESINI TEMIZLE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onAddProductClick,
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("manual_add_product_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Manuel Ürün Ekle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MANUEL YENİ ÜRÜN EKLE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            val groupedProducts = remember(products) {
                products.groupBy {
                    if (it.barkod.isNotBlank()) it.barkod.trim().lowercase() else it.urunAdi.trim().lowercase()
                }.values.toList()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    if (selectedFilter == ProductFilter.IMPORTANT) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, CriticalOrange)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ÖNEMLİ: YÜKSEK STOK + YAKIN SKT (Kategori)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = CriticalOrange
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Stoğu çok (≥10 adet) ve SKT'sine 1 aydan az kalmış (≤30 gün) ürünler. Son 2-3 güne kalmadan acil satış, indirim veya reyon ön sırasına alma gerektirir.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = groupedProducts,
                    key = { group: List<Product> ->
                        val first = group.first()
                        if (first.barkod.isNotBlank()) first.barkod else "${first.urunAdi}_${first.id}"
                    }
                ) { group: List<Product> ->
                    if (group.size == 1) {
                        val product = group.first()
                        ProductListItemCard(
                            product = product,
                            onClick = { onProductClick(product) },
                            onDelete = { onDeleteProduct(product) }
                        )
                    } else {
                        GroupedProductListItemCard(
                            productList = group,
                            onClick = { product -> onProductClick(product) },
                            onDeleteProduct = { product -> onDeleteProduct(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateRangePickerModal(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onApply: (start: Long?, end: Long?) -> Unit,
    onClear: () -> Unit
) {
    var startDateMillis by remember { mutableStateOf(initialStart) }
    var endDateMillis by remember { mutableStateOf(initialEnd) }

    var isSelectingStart by remember { mutableStateOf(false) }
    var isSelectingEnd by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")) }

    if (isSelectingStart) {
        CustomBoxedCalendarDialog(
            initialDateMillis = startDateMillis ?: System.currentTimeMillis(),
            onDismissRequest = { isSelectingStart = false },
            onDateSelected = { millis ->
                startDateMillis = millis
                isSelectingStart = false
            }
        )
    }

    if (isSelectingEnd) {
        CustomBoxedCalendarDialog(
            initialDateMillis = endDateMillis ?: System.currentTimeMillis(),
            onDismissRequest = { isSelectingEnd = false },
            onDateSelected = { millis ->
                endDateMillis = millis
                isSelectingEnd = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, TurquoisePrimary),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Modal Header
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
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Tarih Aralığı",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SKT TARİH ARALIĞI FİLTRESİ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TurquoiseDark
                            )
                            Text(
                                text = "2 tarih seçerek reyon ürünlerini süzün",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Kapat", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection Cards (Start Date & End Date)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Date Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSelectingStart = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (startDateMillis != null) TurquoisePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1. BAŞLANGIÇ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoisePrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = startDateMillis?.let { dateFormat.format(Date(it)) } ?: "Tarih Seçin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (startDateMillis != null) MaterialTheme.colorScheme.onSurface else Slate500
                            )
                        }
                    }

                    // End Date Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSelectingEnd = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (endDateMillis != null) TurquoisePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. BİTİŞ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoisePrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = endDateMillis?.let { dateFormat.format(Date(it)) } ?: "Tarih Seçin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (endDateMillis != null) MaterialTheme.colorScheme.onSurface else Slate500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Presets
                Text(
                    text = "HIZLI TARIH SEÇIMI:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Preset: İlk 7 Gün
                    Surface(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = now
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                            startDateMillis = now
                            endDateMillis = calEnd.timeInMillis
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "İlk 7 Gün",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Preset: Sonraki 7 Gün (Day 7 to Day 14)
                    Surface(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val calStart = Calendar.getInstance().apply {
                                timeInMillis = now
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = now
                                add(Calendar.DAY_OF_YEAR, 14)
                            }
                            startDateMillis = calStart.timeInMillis
                            endDateMillis = calEnd.timeInMillis
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Sonraki 7 Gün",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Preset: Bu Ay
                    Surface(
                        onClick = {
                            val calStart = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            startDateMillis = calStart.timeInMillis
                            endDateMillis = calEnd.timeInMillis
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Bu Ay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Preset: Tarihleri Temizle
                    Surface(
                        onClick = {
                            startDateMillis = null
                            endDateMillis = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Temizle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SIFIRLA", fontWeight = FontWeight.Bold, color = Slate700, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onApply(startDateMillis, endDateMillis) },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .testTag("apply_date_range_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("FİLTREYİ UYGULA", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag("filter_chip_$label")
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
