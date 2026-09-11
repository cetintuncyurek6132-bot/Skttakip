package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.components.report.CleanProductPreviewCard
import com.example.ui.components.report.ImageCropperCanvas
import com.example.ui.components.report.ReportBottomActionBar
import com.example.ui.components.report.ReportInfoModal
import com.example.ui.components.report.ReportSummaryDashboard
import com.example.ui.components.report.ReportTeamNoteCard
import com.example.ui.components.report.ReportTopBar
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Aydınlık / Kurumsal Beyaz Tema Renkleri
private val LightBg = Color(0xFFF8FAFC)
private val LightSurface = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE2E8F0)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF64748B)
private val TealPrimary = Color(0xFF0D9488)
private val GreenSafe = Color(0xFF16A34A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewDialog(
    bitmap: Bitmap? = null,
    products: List<Product> = emptyList(),
    title: String = "Paylaşım Önizlemesi",
    subtitle: String = "Seçilen ürünler WhatsApp üzerinden gönderilecek",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Düzenlenebilir Ürün Listesi
    var currentProducts by remember(products) { mutableStateOf(products) }
    var noteText by remember { mutableStateOf("") }
    var showInfoModal by remember { mutableStateOf(false) }
    var isCropping by remember { mutableStateOf(false) }

    // Tarih ve Saat Bilgisi
    val now = remember { Date() }
    val dateStr = remember(now) { SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR")).format(now) }
    val timeStr = remember(now) { SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(now) }

    // Ürün listesinden otomatik veya harici bitmap üretimi
    val generatedBitmap = remember(currentProducts, noteText, bitmap) {
        if (currentProducts.isNotEmpty()) {
            ProductImageGenerator.createSharePreviewReportBitmap(
                title = title,
                note = noteText,
                productList = currentProducts
            )
        } else {
            bitmap ?: ProductImageGenerator.createSharePreviewReportBitmap(
                title = title,
                note = noteText,
                productList = emptyList()
            )
        }
    }

    var activeBitmap by remember(generatedBitmap) { mutableStateOf(generatedBitmap) }

    // İstatistik Hesaplamaları: "KRİTİK STOK ADETİ" ve "SEÇİLEN ÜRÜN"
    val selectedCount = currentProducts.size
    val criticalProducts = currentProducts.filter { it.getRemainingDays() <= 3L }
    val criticalStockCount = criticalProducts.sumOf { it.stokAdedi }
    val criticalVariantCount = criticalProducts.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LightBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                // 1. ÜST BAR
                ReportTopBar(
                    isCropping = isCropping,
                    title = title,
                    subtitle = subtitle,
                    onCloseClick = {
                        if (isCropping) {
                            isCropping = false
                        } else {
                            onDismiss()
                        }
                    },
                    onInfoClick = { showInfoModal = true }
                )

                // EĞER KIRPMA MODUNDAYSA
                if (isCropping) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ImageCropperCanvas(
                            bitmap = activeBitmap,
                            onCropApplied = { cropped ->
                                activeBitmap = cropped
                                isCropping = false
                                Toast.makeText(context, "Kırpma uygulandı", Toast.LENGTH_SHORT).show()
                            },
                            onCancel = { isCropping = false }
                        )
                    }
                } else {
                    // ANA İÇERİK (DASHBOARD + LİSTE + NOT)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
                    ) {
                        // 2. ÖZET KARTI (DASHBOARD)
                        item {
                            ReportSummaryDashboard(
                                selectedCount = selectedCount,
                                criticalStockCount = criticalStockCount,
                                criticalVariantCount = criticalVariantCount,
                                dateStr = dateStr,
                                timeStr = timeStr
                            )
                        }

                        // 3. ÜRÜN LİSTESİ BAŞLIĞI
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(TealPrimary)
                                    )
                                    Text(
                                        text = "Takip Edilecek Ürünler",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Text(
                                        text = "$selectedCount Çeşit",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // ÜRÜN KARTLARI
                        if (currentProducts.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = LightSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = GreenSafe,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Paylaşılacak ürün kalmadı.",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Listeden tüm ürünler çıkarıldı.",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(currentProducts, key = { _, p -> p.id }) { index, product ->
                                CleanProductPreviewCard(
                                    index = index + 1,
                                    product = product,
                                    onRemove = {
                                        currentProducts = currentProducts.filter { it.id != product.id }
                                        Toast.makeText(
                                            context,
                                            "${product.getDisplayName()} çıkarıldı",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }

                        // 4. ALT BİLGİ / NOT ALANI
                        item {
                            ReportTeamNoteCard(
                                noteText = noteText,
                                onNoteChange = { noteText = it },
                                dateStr = dateStr,
                                timeStr = timeStr,
                                focusManager = focusManager
                            )
                        }
                    }

                    // 5. ALT EYLEM ÇUBUĞU
                    ReportBottomActionBar(
                        context = context,
                        title = title,
                        noteText = noteText,
                        currentProducts = currentProducts,
                        activeBitmap = activeBitmap,
                        isCropping = isCropping,
                        onStartCrop = {
                            activeBitmap = ProductImageGenerator.createSharePreviewReportBitmap(
                                title = title,
                                note = noteText,
                                productList = currentProducts
                            )
                            isCropping = true
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }

    // BİLGİ MODALI
    if (showInfoModal) {
        ReportInfoModal(onDismiss = { showInfoModal = false })
    }
}
