package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.Product
import com.example.data.getTodayMidnightMillis
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductImageGenerator

enum class ShareCategory {
    TODAY_AND_OVERDUE_1,
    LAST_7_DAYS,
    IMPORTANT
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppShareModal(
    allProducts: List<Product>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayMidnight = remember { getTodayMidnightMillis() }

    var selectedCategory by remember { mutableStateOf(ShareCategory.TODAY_AND_OVERDUE_1) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var generatedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Filter lists
    val todayAndOverdue1Products = remember(allProducts, todayMidnight) {
        allProducts.filter { prod ->
            prod.sktTarihi > 0L && prod.stokAdedi > 0 && prod.getRemainingDays(todayMidnight) in -1L..0L
        }.sortedBy { it.sktTarihi }
    }

    val last7DaysProducts = remember(allProducts, todayMidnight) {
        allProducts.filter { prod ->
            prod.sktTarihi > 0L && prod.stokAdedi > 0 && prod.getRemainingDays(todayMidnight) in 0L..7L
        }.sortedBy { it.sktTarihi }
    }

    val importantProducts = remember(allProducts, todayMidnight) {
        allProducts.filter { prod ->
            prod.isImportant
        }.sortedBy { it.sktTarihi }
    }

    val currentCategoryProducts = when (selectedCategory) {
        ShareCategory.TODAY_AND_OVERDUE_1 -> todayAndOverdue1Products
        ShareCategory.LAST_7_DAYS -> last7DaysProducts
        ShareCategory.IMPORTANT -> importantProducts
    }

    var selectedProductIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Auto-select all products when category changes or initial load
    LaunchedEffect(selectedCategory, currentCategoryProducts) {
        selectedProductIds = currentCategoryProducts.map { it.id }.toSet()
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss
    ) { dismissSheet ->
        // =============================================================
        // 1. MODAL HEADER
        // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Görsel Önizleme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPreviewMode) {
                                if (generatedBitmaps.size > 1) "Toplam ${generatedBitmaps.size} sayfa görsel üretildi" else "1 sayfa görsel hazırlandı"
                            } else {
                                "WhatsApp'ta paylaşılacak ürünleri seçin"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_share_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // =============================================================
                // 2. MAIN BODY (SELECTION OR PREVIEW)
                // =============================================================
                if (!isPreviewMode) {
                    // CATEGORY TABS [ SON GÜN / -1 GÜN ] [ SKT SON 7 GÜN ] [ ÖNEMLİ ]
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tab 1 (ÖNCELİKLİ / BAŞTA): SON GÜN / -1 GÜN GEÇMİŞ
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selectedCategory == ShareCategory.TODAY_AND_OVERDUE_1) TurquoisePrimary else Color.Transparent
                                    )
                                    .clickable { selectedCategory = ShareCategory.TODAY_AND_OVERDUE_1 }
                                    .padding(horizontal = 12.dp)
                                    .testTag("share_tab_today_and_overdue_1"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (selectedCategory == ShareCategory.TODAY_AND_OVERDUE_1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Son Gün / -1 Gün (${todayAndOverdue1Products.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedCategory == ShareCategory.TODAY_AND_OVERDUE_1) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (selectedCategory == ShareCategory.TODAY_AND_OVERDUE_1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Tab 2: SKT SON 7 GÜN
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selectedCategory == ShareCategory.LAST_7_DAYS) TurquoisePrimary else Color.Transparent
                                    )
                                    .clickable { selectedCategory = ShareCategory.LAST_7_DAYS }
                                    .padding(horizontal = 12.dp)
                                    .testTag("share_tab_last_7_days"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (selectedCategory == ShareCategory.LAST_7_DAYS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Son 7 Gün (${last7DaysProducts.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedCategory == ShareCategory.LAST_7_DAYS) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (selectedCategory == ShareCategory.LAST_7_DAYS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Tab 3: ÖNEMLİ
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selectedCategory == ShareCategory.IMPORTANT) TurquoisePrimary else Color.Transparent
                                    )
                                    .clickable { selectedCategory = ShareCategory.IMPORTANT }
                                    .padding(horizontal = 12.dp)
                                    .testTag("share_tab_important"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (selectedCategory == ShareCategory.IMPORTANT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Önemli (${importantProducts.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedCategory == ShareCategory.IMPORTANT) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (selectedCategory == ShareCategory.IMPORTANT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ACTION BAR: Info + "Tümünü Seç" / "Seçimi Temizle"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentCategoryProducts.isNotEmpty()) {
                                "${currentCategoryProducts.size} ürün listeleniyor • Seçili: ${selectedProductIds.size}"
                            } else {
                                "0 ürün listeleniyor"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedProductIds = currentCategoryProducts.map { it.id }.toSet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, TurquoisePrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tümünü Seç",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedProductIds = emptySet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Deselect,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Temizle",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // PRODUCT LIST
                    if (currentCategoryProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Listelenecek ürün bulunamadı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (selectedCategory) {
                                        ShareCategory.TODAY_AND_OVERDUE_1 -> "Son gün veya 1 gün geçmiş süresi dolan ürün kaydı bulunmuyor."
                                        ShareCategory.LAST_7_DAYS -> "Son 7 gün içinde süresi dolacak ürün kaydı bulunmuyor."
                                        ShareCategory.IMPORTANT -> "Önemli olarak işaretlenmiş ürün bulunmuyor."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(currentCategoryProducts, key = { it.id }) { product ->
                                val isSelected = selectedProductIds.contains(product.id)
                                val days = product.getRemainingDays(todayMidnight)
                                val daysText = when {
                                    days < 0L -> "Süresi Geçti"
                                    days == 0L -> "Bugün Doluyor"
                                    days == 1L -> "1 Gün Kaldı"
                                    else -> "$days Gün Kaldı"
                                }

                                Surface(
                                    onClick = {
                                        selectedProductIds = if (isSelected) {
                                            selectedProductIds - product.id
                                        } else {
                                            selectedProductIds + product.id
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) TurquoisePrimary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedProductIds = if (checked) {
                                                    selectedProductIds + product.id
                                                } else {
                                                    selectedProductIds - product.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = TurquoisePrimary,
                                                uncheckedColor = MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.urunAdi,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // SKT Date
                                                Text(
                                                    text = "SKT: ${product.getFormattedSkt()} ($daysText)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = when {
                                                        days in 0L..1L -> Color(0xFFDC2626)
                                                        days in 2L..7L -> Color(0xFFD97706)
                                                        else -> TurquoiseDark
                                                    }
                                                )

                                                // Stok Adedi
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = "${product.stokAdedi} Adet",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BOTTOM ACTION: "Önizle"
                    Button(
                        onClick = {
                            val chosenProducts = currentCategoryProducts.filter { selectedProductIds.contains(it.id) }
                            if (chosenProducts.isNotEmpty()) {
                                generatedBitmaps = ProductImageGenerator.createCleanWhatsAppShareBitmaps(
                                    productList = chosenProducts,
                                    todayMidnight = todayMidnight
                                )
                                isPreviewMode = true
                            }
                        },
                        enabled = selectedProductIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("preview_share_images_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurquoisePrimary,
                            disabledContainerColor = TurquoisePrimary.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Önizle (${selectedProductIds.size} Ürün)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    // =============================================================
                    // 3. PREVIEW & WHATSAPP SHARE STATE
                    // =============================================================
                    if (generatedBitmaps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Görsel oluşturulamadı.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val pagerState = rememberPagerState(pageCount = { generatedBitmaps.size })

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Multiple Page Indicator (e.g. "Sayfa 1 / 2")
                            if (generatedBitmaps.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = TurquoisePrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "Sayfa ${pagerState.currentPage + 1} / ${generatedBitmaps.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TurquoiseDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Card with image container (wraps exact image height without empty void)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                if (generatedBitmaps.size > 1) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) { page ->
                                        val bmp = generatedBitmaps[page]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 550.dp)
                                                .verticalScroll(rememberScrollState()),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Paylaşım Görseli Sayfa ${page + 1}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                contentScale = ContentScale.FillWidth
                                            )
                                        }
                                    }
                                } else {
                                    val bmp = generatedBitmaps.first()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 550.dp)
                                            .verticalScroll(rememberScrollState()),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Paylaşım Görseli",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ACTIONS: [ Geri Dön ] [ Paylaş ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { isPreviewMode = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_back_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, TurquoisePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Geri Dön",
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                ProductImageGenerator.shareBitmapsToWhatsApp(
                                    context = context,
                                    bitmaps = generatedBitmaps
                                )
                                onDismiss()
                            },
                            enabled = generatedBitmaps.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("share_to_whatsapp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paylaş",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
    }
}
