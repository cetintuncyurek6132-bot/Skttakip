package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.getDisplayCode
import com.example.data.getDisplayName
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

// Aydınlık / Kurumsal Beyaz Tema Renkleri
private val LightBg = Color(0xFFF8FAFC)
private val LightSurface = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE2E8F0)
private val BorderStrong = Color(0xFFCBD5E1)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF64748B)
private val TextSubtle = Color(0xFF94A3B8)

private val TealPrimary = Color(0xFF0D9488)
private val TealLightBg = Color(0xFFF0FDFA)
private val RedCritical = Color(0xFFDC2626)
private val RedLightBg = Color(0xFFFEF2F2)
private val OrangeWarning = Color(0xFFEA580C)
private val OrangeLightBg = Color(0xFFFFF7ED)
private val YellowAlert = Color(0xFFCA8A04)
private val YellowLightBg = Color(0xFFFEFCE8)
private val GreenSafe = Color(0xFF16A34A)
private val GreenLightBg = Color(0xFFF0FDF4)
private val BlueDate = Color(0xFF2563EB)
private val BlueLightBg = Color(0xFFEFF6FF)

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

    // İstatistik Hesaplamaları: Kullanıcının istediği gibi "Toplam stok değil KRİTİK STOK ADETİ"
    val selectedCount = currentProducts.size
    val criticalProducts = currentProducts.filter { it.getRemainingDays() <= 3L }
    val criticalStockCount = criticalProducts.sumOf { it.stokAdedi }
    val criticalVariantCount = criticalProducts.size

    fun buildWhatsAppMessage(): String {
        val sb = StringBuilder()
        sb.append("📋 *A101 SKT TAKİP & STOK - KONTROL RAPORU*\n")
        sb.append("📅 *Tarih:* $dateStr $timeStr\n")
        sb.append("📦 *Seçilen Ürün:* $selectedCount Çeşit\n")
        sb.append("⚠️ *Kritik Stok Adeti:* $criticalStockCount Adet ($criticalVariantCount Çeşit)\n\n")

        if (noteText.isNotBlank()) {
            sb.append("📝 *Not:* $noteText\n\n")
        }

        if (currentProducts.isNotEmpty()) {
            sb.append("*Takip Edilecek Ürünler:*\n")
            currentProducts.take(20).forEachIndexed { i, p ->
                val days = p.getRemainingDays()
                val statusEmoji = when {
                    days <= 0L -> "🔴 [SÜRESİ GEÇTİ]"
                    days <= 3L -> "🟠 [YAKLAŞIYOR - ${days} GÜN]"
                    days <= 15L -> "🟡 [${days} GÜN]"
                    else -> "🟢 [GÜVENDE - ${days} GÜN]"
                }
                sb.append("${i + 1}. *${p.getDisplayName()}* | *Stok: ${p.stokAdedi} Adet* | SKT: ${p.getFormattedSkt()} $statusEmoji\n")
            }
            if (currentProducts.size > 20) {
                sb.append("...ve ${currentProducts.size - 20} ürün daha.\n")
            }
            sb.append("\n_Detaylı görsel rapor ektedir._")
        }
        return sb.toString()
    }

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
                // ==========================================
                // 1. ÜST BAR (BEYAZ TEMA & TEMİZ BAŞLIK)
                // ==========================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LightSurface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Sol Üstte Kapatma İkonu (X)
                        IconButton(
                            onClick = {
                                if (isCropping) {
                                    isCropping = false
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("preview_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Ortada Başlık ve Açıklayıcı Alt Metin
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isCropping) "Görseli Kırp & Düzenle" else title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isCropping) "Alanı seçip Uygula'ya basınız" else subtitle,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Sağ Üstte Bilgi İkonu (i)
                        IconButton(
                            onClick = { showInfoModal = true },
                            modifier = Modifier.testTag("preview_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Bilgi",
                                tint = TealPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

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
                    // ==========================================
                    // ANA İÇERİK (DASHBOARD + LİSTE + NOT)
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
                    ) {
                        // ==========================================
                        // 2. ÖZET KARTI (DASHBOARD) - 4 KUTU (YAZI KAYMASI ÖNLENMİŞ & KRİTİK STOK ODAKLI)
                        // ==========================================
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. SEÇİLEN ÜRÜN
                                CleanSummaryMiniCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Inventory2,
                                    iconBg = TealLightBg,
                                    iconTint = TealPrimary,
                                    label = "SEÇİLEN",
                                    value = "$selectedCount",
                                    unit = "ÜRÜN",
                                    valueColor = TealPrimary
                                )

                                // 2. KRİTİK STOK ADETİ (Kullanıcı talebi: Toplam stok değil Kritik Stok Adeti)
                                CleanSummaryMiniCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Layers,
                                    iconBg = RedLightBg,
                                    iconTint = RedCritical,
                                    label = "KRİTİK STOK",
                                    value = "$criticalStockCount",
                                    unit = "ADET",
                                    valueColor = RedCritical
                                )

                                // 3. KRİTİK ÇEŞİT
                                CleanSummaryMiniCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Warning,
                                    iconBg = OrangeLightBg,
                                    iconTint = OrangeWarning,
                                    label = "KRİTİK ÇEŞİT",
                                    value = "$criticalVariantCount",
                                    unit = "ÜRÜN",
                                    valueColor = if (criticalVariantCount > 0) OrangeWarning else TextPrimary
                                )

                                // 4. TAKİP TARİHİ
                                CleanSummaryMiniCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Event,
                                    iconBg = BlueLightBg,
                                    iconTint = BlueDate,
                                    label = "TARİH",
                                    value = dateStr,
                                    unit = timeStr,
                                    valueColor = TextPrimary,
                                    isDate = true
                                )
                            }
                        }

                        // ==========================================
                        // 3. ÜRÜN LİSTESİ BAŞLIĞI
                        // ==========================================
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

                        // ==========================================
                        // 4. ALT BİLGİ / NOT ALANI
                        // ==========================================
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
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = TealPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Ekip Notu (İsteğe Bağlı)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    OutlinedTextField(
                                        value = noteText,
                                        onValueChange = {
                                            if (it.length <= 120) {
                                                noteText = it
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                text = "Bu rapor hakkında mağaza ekibine kısa bir not ekleyin...",
                                                color = TextSubtle,
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        supportingText = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = "${noteText.length}/120",
                                                    color = TextSubtle,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        singleLine = false,
                                        maxLines = 3,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = Color(0xFFFAFAFA),
                                            unfocusedContainerColor = Color(0xFFFAFAFA),
                                            focusedBorderColor = TealPrimary,
                                            unfocusedBorderColor = BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("preview_note_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = TextSubtle,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Oluşturulma: $dateStr $timeStr  •  SKT & Stok Takip Sistemi",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 5. ALT EYLEM ÇUBUĞU (ACTION BAR - HER EKRANA TAM SIĞAN KOMPAKT DÜZEN)
                    // ==========================================
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = LightSurface,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. SOL: KIRP
                            OutlinedButton(
                                onClick = {
                                    activeBitmap = ProductImageGenerator.createSharePreviewReportBitmap(
                                        title = title,
                                        note = noteText,
                                        productList = currentProducts
                                    )
                                    isCropping = true
                                },
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("btn_preview_crop"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, BorderStrong)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = "Kırp",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "KIRP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = TextPrimary
                                )
                            }

                            // 2. ORTA: WHATSAPP'TA PAYLAŞ
                            Button(
                                onClick = {
                                    val finalBmp = if (isCropping) activeBitmap else {
                                        ProductImageGenerator.createSharePreviewReportBitmap(
                                            title = title,
                                            note = noteText,
                                            productList = currentProducts
                                        )
                                    }
                                    val textMsg = buildWhatsAppMessage()
                                    ProductImageGenerator.shareBitmapWithText(
                                        context = context,
                                        bitmap = finalBmp,
                                        textMessage = textMsg,
                                        fileNamePrefix = "skt_rapor"
                                    )
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_preview_whatsapp_share"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "WhatsApp",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "WhatsApp Paylaş",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White
                                    )
                                }
                            }

                            // 3. SAĞ: İNDİR (Cihaza görsel olarak kaydet)
                            Surface(
                                onClick = {
                                    val finalBmp = if (isCropping) activeBitmap else {
                                        ProductImageGenerator.createSharePreviewReportBitmap(
                                            title = title,
                                            note = noteText,
                                            productList = currentProducts
                                        )
                                    }
                                    ProductImageGenerator.saveBitmapToGallery(
                                        context = context,
                                        bitmap = finalBmp,
                                        fileNamePrefix = "skt_rapor"
                                    )
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("btn_preview_download"),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "İndir",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // BİLGİ MODALI (i) - AYDINLIK TEMA
    // ==========================================
    if (showInfoModal) {
        AlertDialog(
            onDismissRequest = { showInfoModal = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = LightSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(TealLightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Paylaşım Önizlemesi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bu ekranda mağaza ekibinizle paylaşılacak ürünleri ve stok adetlerini kontrol edebilirsiniz:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "• İstemediğiniz ürünlerin yanındaki çöp kutusu ikonuna basarak listeden çıkarabilirsiniz.\n• Her ürünün stok adeti ve SKT kalan gün sayısı açıkça listelenir.\n• İsteğe bağlı ekip notu ekleyebilirsiniz.\n• 'WhatsApp'ta Paylaş' butonu ile görsel rapor ve metin özeti aynı anda paylaşılır.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoModal = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Anladım", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Canlı, belirgin ve orantılı renkli mini özet kartı
 */
@Composable
private fun CleanSummaryMiniCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    isDate: Boolean = false
) {
    Surface(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, iconTint.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Üst ince renkli vurgu çizgisi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(iconTint)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // İkon Kutucuğu
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg)
                        .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Başlık (Tek satır, asla kaymaz)
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = iconTint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Değer ve Birim
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = value,
                        fontSize = if (isDate) 10.5.sp else 15.sp,
                        fontWeight = FontWeight.Black,
                        color = valueColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = unit,
                        fontSize = if (isDate) 9.sp else 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Belirgin, renkli ve kart şeklinde tasarlanmış ürün önizleme kartı
 */
@Composable
private fun CleanProductPreviewCard(
    index: Int,
    product: Product,
    onRemove: () -> Unit
) {
    val days = product.getRemainingDays()
    val (statusLabel, statusBg, statusColor, daysBadgeText) = when {
        days <= 0L -> Quadruple("SÜRESİ GEÇTİ", RedLightBg, RedCritical, "GEÇTİ")
        days <= 3L -> Quadruple("YAKLAŞIYOR", OrangeLightBg, OrangeWarning, "${days} GÜN")
        days <= 15L -> Quadruple("4-15 GÜN", YellowLightBg, YellowAlert, "${days} GÜN")
        else -> Quadruple("GÜVENDE", GreenLightBg, GreenSafe, "${days} GÜN")
    }

    // Kategoriye özel renkler
    val (catBg, catText) = when (product.kategori.lowercase()) {
        "süt & kahvaltılık", "şarküteri" -> Pair(Color(0xFFE0F2FE), Color(0xFF0284C7))
        "et & tavuk" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
        "unlu mamül", "ekmek" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
        "meyve & sebze" -> Pair(Color(0xFFDCFCE7), Color(0xFF16A34A))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preview_product_card_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SOL: Kalan Gün / SKT Durum Rozeti (Büyük Renkli Kare)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (days <= 0L) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GEÇTİ",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        } else {
                            Text(
                                text = "$days",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "GÜN",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // ORTA: Ürün Detayları
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Satır: Sıra No & Ürün Adı
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TealLightBg)
                                .border(0.5.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "#$index",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TealPrimary
                            )
                        }

                        Text(
                            text = product.getDisplayName().uppercase(),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Satır: Kategori Rozeti + Kod
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Kategori Rozeti (Renkli)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(catBg)
                                .border(0.5.dp, catText.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = product.kategori.ifBlank { "Genel" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = catText
                            )
                        }

                        // Kod Rozeti
                        val codeText = if (product.urunKodu.isNotBlank()) product.urunKodu else product.id.toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Kod: $codeText",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                    }

                    // 3. Satır: Vurgulu STOK ADETİ ve SKT TARİHİ
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 📦 VURGULU STOK ROZETİ (Canlı Amber / Turuncu Vurgulu)
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Stok: ${product.stokAdedi} Adet",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }

                        // 📅 SKT TARİH ROZETİ
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = statusBg,
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = product.getFormattedSkt(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // SAĞ: Silme İkonu & Durum Rozeti
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(78.dp)
                ) {
                    // Kırmızı Arka Planlı Şık Çöp Kutusu Butonu
                    Surface(
                        onClick = onRemove,
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(0.5.dp, Color(0xFFFECACA)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Listeden Çıkar",
                                tint = RedCritical,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Durum Rozeti
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Kart Altında Canlı Risk Seviyesi Çizgisi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(statusColor)
            )
        }
    }
}

// 4 değerli yardımcı veri sınıfı
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun ImageCropperCanvas(
    bitmap: Bitmap,
    onCropApplied: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var cropLeftFrac by remember { mutableFloatStateOf(0.03f) }
    var cropTopFrac by remember { mutableFloatStateOf(0.03f) }
    var cropRightFrac by remember { mutableFloatStateOf(0.97f) }
    var cropBottomFrac by remember { mutableFloatStateOf(0.97f) }

    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = constraints.maxWidth.toFloat()
            val containerHeightPx = constraints.maxHeight.toFloat()

            val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val containerAspect = containerWidthPx / containerHeightPx

            val (imgWidthPx, imgHeightPx, imgLeftPx, imgTopPx) = if (containerAspect > imgAspect) {
                val h = containerHeightPx
                val w = h * imgAspect
                val l = (containerWidthPx - w) / 2f
                val t = 0f
                listOf(w, h, l, t)
            } else {
                val w = containerWidthPx
                val h = w / imgAspect
                val l = 0f
                val t = (containerHeightPx - h) / 2f
                listOf(w, h, l, t)
            }

            val boxLeft = imgLeftPx + cropLeftFrac * imgWidthPx
            val boxTop = imgTopPx + cropTopFrac * imgHeightPx
            val boxRight = imgLeftPx + cropRightFrac * imgWidthPx
            val boxBottom = imgTopPx + cropBottomFrac * imgHeightPx

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val touchThreshold = 60.dp.toPx()
                                val x = offset.x
                                val y = offset.y

                                val distTL = hypot(x - boxLeft, y - boxTop)
                                val distTR = hypot(x - boxRight, y - boxTop)
                                val distBL = hypot(x - boxLeft, y - boxBottom)
                                val distBR = hypot(x - boxRight, y - boxBottom)

                                activeHandle = when {
                                    distTL < touchThreshold -> CropHandle.TOP_LEFT
                                    distTR < touchThreshold -> CropHandle.TOP_RIGHT
                                    distBL < touchThreshold -> CropHandle.BOTTOM_LEFT
                                    distBR < touchThreshold -> CropHandle.BOTTOM_RIGHT
                                    x in boxLeft..boxRight && y in boxTop..boxBottom -> CropHandle.CENTER
                                    else -> null
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val handle = activeHandle ?: return@detectDragGestures

                                val dxFrac = dragAmount.x / imgWidthPx
                                val dyFrac = dragAmount.y / imgHeightPx

                                val minSize = 0.08f

                                when (handle) {
                                    CropHandle.TOP_LEFT -> {
                                        cropLeftFrac = (cropLeftFrac + dxFrac).coerceIn(0f, cropRightFrac - minSize)
                                        cropTopFrac = (cropTopFrac + dyFrac).coerceIn(0f, cropBottomFrac - minSize)
                                    }
                                    CropHandle.TOP_RIGHT -> {
                                        cropRightFrac = (cropRightFrac + dxFrac).coerceIn(cropLeftFrac + minSize, 1f)
                                        cropTopFrac = (cropTopFrac + dyFrac).coerceIn(0f, cropBottomFrac - minSize)
                                    }
                                    CropHandle.BOTTOM_LEFT -> {
                                        cropLeftFrac = (cropLeftFrac + dxFrac).coerceIn(0f, cropRightFrac - minSize)
                                        cropBottomFrac = (cropBottomFrac + dyFrac).coerceIn(cropTopFrac + minSize, 1f)
                                    }
                                    CropHandle.BOTTOM_RIGHT -> {
                                        cropRightFrac = (cropRightFrac + dxFrac).coerceIn(cropLeftFrac + minSize, 1f)
                                        cropBottomFrac = (cropBottomFrac + dyFrac).coerceIn(cropTopFrac + minSize, 1f)
                                    }
                                    CropHandle.CENTER -> {
                                        val widthFrac = cropRightFrac - cropLeftFrac
                                        val heightFrac = cropBottomFrac - cropTopFrac

                                        var newLeft = cropLeftFrac + dxFrac
                                        var newTop = cropTopFrac + dyFrac

                                        if (newLeft < 0f) newLeft = 0f
                                        if (newLeft + widthFrac > 1f) newLeft = 1f - widthFrac

                                        if (newTop < 0f) newTop = 0f
                                        if (newTop + heightFrac > 1f) newTop = 1f - heightFrac

                                        cropLeftFrac = newLeft
                                        cropRightFrac = newLeft + widthFrac
                                        cropTopFrac = newTop
                                        cropBottomFrac = newTop + heightFrac
                                    }
                                }
                            },
                            onDragEnd = { activeHandle = null },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            ) {
                // Görseli çiz
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(imgLeftPx.toInt(), imgTopPx.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(imgWidthPx.toInt(), imgHeightPx.toInt())
                )

                // Karartma katmanları
                val darkOverlay = Color(0x99000000)
                drawRect(darkOverlay, Offset.Zero, Size(size.width, boxTop))
                drawRect(darkOverlay, Offset(0f, boxBottom), Size(size.width, size.height - boxBottom))
                drawRect(darkOverlay, Offset(0f, boxTop), Size(boxLeft, boxBottom - boxTop))
                drawRect(darkOverlay, Offset(boxRight, boxTop), Size(size.width - boxRight, boxBottom - boxTop))

                // Kırpma çerçevesi
                drawRect(
                    color = Color.White,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxRight - boxLeft, boxBottom - boxTop),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Izgara çizgileri (Üçte bir kuralı)
                val w = boxRight - boxLeft
                val h = boxBottom - boxTop
                val gridColor = Color.White.copy(alpha = 0.35f)
                drawLine(gridColor, Offset(boxLeft + w / 3f, boxTop), Offset(boxLeft + w / 3f, boxBottom), 1.dp.toPx())
                drawLine(gridColor, Offset(boxLeft + 2 * w / 3f, boxTop), Offset(boxLeft + 2 * w / 3f, boxBottom), 1.dp.toPx())
                drawLine(gridColor, Offset(boxLeft, boxTop + h / 3f), Offset(boxRight, boxTop + h / 3f), 1.dp.toPx())
                drawLine(gridColor, Offset(boxLeft, boxTop + 2 * h / 3f), Offset(boxRight, boxTop + 2 * h / 3f), 1.dp.toPx())

                // Köşe tutamaçları
                val handleRadius = 8.dp.toPx()
                val handleColor = Color(0xFF00C2AB)
                drawCircle(handleColor, handleRadius, Offset(boxLeft, boxTop))
                drawCircle(handleColor, handleRadius, Offset(boxRight, boxTop))
                drawCircle(handleColor, handleRadius, Offset(boxLeft, boxBottom))
                drawCircle(handleColor, handleRadius, Offset(boxRight, boxBottom))
            }
        }

        // Kırpma Butonları
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("İptal", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try {
                            val cropX = (cropLeftFrac * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                            val cropY = (cropTopFrac * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            val cropW = ((cropRightFrac - cropLeftFrac) * bitmap.width).toInt().coerceIn(10, bitmap.width - cropX)
                            val cropH = ((cropBottomFrac - cropTopFrac) * bitmap.height).toInt().coerceIn(10, bitmap.height - cropY)

                            val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
                            onCropApplied(cropped)
                        } catch (e: Exception) {
                            onCancel()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uygula", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}
