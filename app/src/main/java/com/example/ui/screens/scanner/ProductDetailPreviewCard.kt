package com.example.ui.screens.scanner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.components.CustomBoxedCalendarDialog
import com.example.ui.screens.DateOcrScannerDialog
import java.util.Calendar
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private fun getDaysRemaining(sktTarihi: Long): Long {
    if (sktTarihi <= 0L) return 9999L
    val calSkt = java.util.Calendar.getInstance().apply {
        timeInMillis = sktTarihi
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val calNow = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val diffMillis = calSkt.timeInMillis - calNow.timeInMillis
    return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailPreviewCard(
    product: Product,
    matchingProducts: List<Product> = listOf(product),
    initialSktMillis: Long? = null,
    onAddSkt: (Product, Long, Int) -> Unit = { _, _, _ -> },
    onDeductStock: (Product, Int, String) -> Unit = { _, _, _ -> },
    onSelectProduct: () -> Unit,
    onClearDetail: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

    // Only products with a valid SKT date and stock > 0
    val validSktProducts = remember(matchingProducts) {
        matchingProducts.filter { it.sktTarihi > 0L && it.stokAdedi > 0 }
    }

    val effectiveProduct = remember(product, validSktProducts) {
        if (product.sktTarihi > 0L) {
            product
        } else {
            validSktProducts.minByOrNull { it.sktTarihi } ?: product
        }
    }

    val hasSkt = effectiveProduct.sktTarihi > 0L
    val daysRemaining = if (hasSkt) effectiveProduct.getRemainingDays() else 9999L

    var selectedSktMillis by remember(product.barkod, initialSktMillis) {
        mutableStateOf(initialSktMillis ?: (if (product.sktTarihi > 0L) product.sktTarihi else System.currentTimeMillis()))
    }
    var sktAdediStr by remember(product.barkod) { mutableStateOf("1") }
    var isOcrScannerOpen by remember { mutableStateOf(false) }
    var isDatePickerOpen by remember { mutableStateOf(false) }
    var selectedRiskFilter by remember(product.barkod) { mutableStateOf<String?>(null) }

    if (isDatePickerOpen) {
        CustomBoxedCalendarDialog(
            initialDateMillis = selectedSktMillis,
            onDismissRequest = { isDatePickerOpen = false },
            onDateSelected = { selectedMillis ->
                selectedSktMillis = selectedMillis
            }
        )
    }

    // Risk Level Breakdown calculated only on valid SKT products
    val expiredOrNearCount = remember(validSktProducts) {
        validSktProducts.filter { getDaysRemaining(it.sktTarihi) <= 3 }.sumOf { it.stokAdedi }
    }
    val criticalCount = remember(validSktProducts) {
        validSktProducts.filter { getDaysRemaining(it.sktTarihi) in 4..15 }.sumOf { it.stokAdedi }
    }
    val safeCount = remember(validSktProducts) {
        validSktProducts.filter { getDaysRemaining(it.sktTarihi) >= 16 }.sumOf { it.stokAdedi }
    }

    val filteredValidSktProducts = remember(validSktProducts, selectedRiskFilter) {
        val filtered = when (selectedRiskFilter) {
            "EXPIRED" -> validSktProducts.filter { getDaysRemaining(it.sktTarihi) <= 3 }
            "CRITICAL" -> validSktProducts.filter { getDaysRemaining(it.sktTarihi) in 4..15 }
            "SAFE" -> validSktProducts.filter { getDaysRemaining(it.sktTarihi) >= 16 }
            else -> validSktProducts
        }
        filtered.sortedBy { it.sktTarihi }
    }

    val (badgeText, badgeBg, badgeTextColor) = when {
        !hasSkt -> Triple(
            "ℹ️ SKT GİRİLMEDİ",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        daysRemaining < 0 -> Triple(
            "🚨 SÜRESİ GEÇTİ (${abs(daysRemaining)} gün)",
            ExpiredRedContainer,
            ExpiredRed
        )
        daysRemaining in 0..7 -> Triple(
            "⚠️ KRİTİK ($daysRemaining Gün)",
            CriticalOrangeContainer,
            CriticalOrange
        )
        else -> Triple(
            "✅ NORMAL ($daysRemaining Gün)",
            NormalGreenContainer,
            NormalGreen
        )
    }

    if (isOcrScannerOpen) {
        DateOcrScannerDialog(
            onDismiss = { isOcrScannerOpen = false },
            onDateDetected = { dateMillis, _ ->
                selectedSktMillis = dateMillis
                isOcrScannerOpen = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, TurquoisePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top info: Full Product Name & Clear Button (Row 1), Badges & Category/Barcode (Row 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = product.urunAdi,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )

                IconButton(
                    onClick = onClearDetail,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Temizle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Sub-row: Barcode/Code (left) + Price & SKT Badges (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val codeDisplay = if (product.urunKodu.isNotBlank()) "Kod: ${product.urunKodu}" else "Barkod: ${product.barkod}"
                Text(
                    text = codeDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    product.getFormattedPrice()?.let { formattedPrice ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(0.5.dp, Color(0xFF0284C7))
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeTextColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }

            // COLORED RISK BREAKDOWN BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isExpiredSelected = selectedRiskFilter == "EXPIRED"
                val isCriticalSelected = selectedRiskFilter == "CRITICAL"
                val isSafeSelected = selectedRiskFilter == "SAFE"

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isExpiredSelected) ExpiredRedContainer else if (selectedRiskFilter != null) ExpiredRedContainer.copy(alpha = 0.5f) else ExpiredRedContainer,
                    border = BorderStroke(if (isExpiredSelected) 1.8.dp else 1.dp, if (isExpiredSelected) ExpiredRed else ExpiredRed.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedRiskFilter = if (selectedRiskFilter == "EXPIRED") null else "EXPIRED"
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isExpiredSelected) "🚨✓ " else "🚨 ", fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = ExpiredRed)
                        Text("$expiredOrNearCount ad.", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ExpiredRed)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCriticalSelected) CriticalOrangeContainer else if (selectedRiskFilter != null) CriticalOrangeContainer.copy(alpha = 0.5f) else CriticalOrangeContainer,
                    border = BorderStroke(if (isCriticalSelected) 1.8.dp else 1.dp, if (isCriticalSelected) CriticalOrange else CriticalOrange.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedRiskFilter = if (selectedRiskFilter == "CRITICAL") null else "CRITICAL"
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isCriticalSelected) "⚠️✓ " else "⚠️ ", fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = CriticalOrange)
                        Text("$criticalCount ad.", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = CriticalOrange)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSafeSelected) NormalGreenContainer else if (selectedRiskFilter != null) NormalGreenContainer.copy(alpha = 0.5f) else NormalGreenContainer,
                    border = BorderStroke(if (isSafeSelected) 1.8.dp else 1.dp, if (isSafeSelected) NormalGreen else NormalGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedRiskFilter = if (selectedRiskFilter == "SAFE") null else "SAFE"
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSafeSelected) "✅✓ " else "✅ ", fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = NormalGreen)
                        Text("$safeCount ad.", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = NormalGreen)
                    }
                }
            }

            // Registered SKT list (Horizontal Row)
            if (filteredValidSktProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedRiskFilter != null) "Sıralanan (${filteredValidSktProducts.size}):" else "Kayıtlar (${validSktProducts.size}):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )
                    filteredValidSktProducts.forEach { item ->
                        val dateStr = dateFormat.format(Date(item.sktTarihi))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "📅 $dateStr (${item.stokAdedi} ad.)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (selectedRiskFilter != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { selectedRiskFilter = null }
                        ) {
                            Text(
                                text = "Tümü ✕",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // HIZLI DÜŞME BAR (HIZLI SATILDI / HIZLI FİRE) - MODAL AÇMADAN TEK DOKUNUŞLA
            if (validSktProducts.isNotEmpty()) {
                val targetBatch = effectiveProduct
                val canDeduct = targetBatch.stokAdedi > 0
                val batchDateStr = if (targetBatch.sktTarihi > 0L) dateFormat.format(Date(targetBatch.sktTarihi)) else "Tarihsiz"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HIZLI İŞLEM (${batchDateStr}):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate700
                            )
                            Text(
                                text = "Mevcut: ${targetBatch.stokAdedi} Adet",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (canDeduct) TurquoiseDark else ExpiredRed
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // -1 SATILDI
                            Button(
                                onClick = {
                                    if (canDeduct) {
                                        onDeductStock(targetBatch, 1, "SATILDI")
                                        Toast.makeText(context, "✅ 1 adet Satıldı olarak düşüldü (${batchDateStr})", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "⚠️ Bu partide stok kalmadı!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = canDeduct,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "-1 Satıldı",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            // -1 FİRE
                            Button(
                                onClick = {
                                    if (canDeduct) {
                                        onDeductStock(targetBatch, 1, "FİRE")
                                        Toast.makeText(context, "🗑️ 1 adet Fire olarak düşüldü (${batchDateStr})", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "⚠️ Bu partide stok kalmadı!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = canDeduct,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "-1 Fire",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // SKT & ADEDİ EKLEME ALANI (COMPACT BOX)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Date Picker & OCR Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isDatePickerOpen = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("skt_date_picker_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, TurquoisePrimary),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Tarih",
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateFormat.format(Date(selectedSktMillis)),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = { isOcrScannerOpen = true },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("open_skt_ocr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "SKT Tara",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OCR",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                // Row 1.5: Hızlı Tarih Kısayolları (Ayaküstü tek tıkla SKT belirleme)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(
                        "+15G" to {
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, 15)
                            c.timeInMillis
                        },
                        "+1 Ay" to {
                            val c = Calendar.getInstance()
                            c.add(Calendar.MONTH, 1)
                            c.timeInMillis
                        },
                        "+2 Ay" to {
                            val c = Calendar.getInstance()
                            c.add(Calendar.MONTH, 2)
                            c.timeInMillis
                        },
                        "+3 Ay" to {
                            val c = Calendar.getInstance()
                            c.add(Calendar.MONTH, 3)
                            c.timeInMillis
                        },
                        "+6 Ay" to {
                            val c = Calendar.getInstance()
                            c.add(Calendar.MONTH, 6)
                            c.timeInMillis
                        },
                        "+1 Yıl" to {
                            val c = Calendar.getInstance()
                            c.add(Calendar.YEAR, 1)
                            c.timeInMillis
                        },
                        "Yıl Sonu" to {
                            val c = Calendar.getInstance()
                            c.set(Calendar.MONTH, Calendar.DECEMBER)
                            c.set(Calendar.DAY_OF_MONTH, 31)
                            c.timeInMillis
                        }
                    )
                    presets.forEach { (label, dateCalc) ->
                        Surface(
                            onClick = {
                                selectedSktMillis = dateCalc()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.45f)),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }
                }

                // Row 2: Quantity Stepper & Optional "+ Başka SKT Ekle"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stepper: - [qty] +
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val current = sktAdediStr.toIntOrNull() ?: 0
                                if (current > 1) sktAdediStr = (current - 1).toString()
                            },
                            modifier = Modifier.size(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(34.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = sktAdediStr,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                        sktAdediStr = newValue
                                    }
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(TurquoisePrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val current = sktAdediStr.toIntOrNull() ?: 0
                                sktAdediStr = (current + 1).toString()
                            },
                            modifier = Modifier.size(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val count = sktAdediStr.toIntOrNull() ?: 1
                            onAddSkt(product, selectedSktMillis, count)
                            Toast.makeText(context, "✓ SKT eklendi ($count adet), yeni SKT girebilirsiniz", Toast.LENGTH_SHORT).show()
                            sktAdediStr = "1"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("save_skt_button"),
                        border = BorderStroke(1.dp, TurquoisePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Başka SKT Ekle",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Başka SKT Ekle",
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Row 3: Tek Adımlı Ana Buton: "KAYDET VE SIRADAKİNİ TARA"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val count = sktAdediStr.toIntOrNull() ?: 1
                            onAddSkt(product, selectedSktMillis, count)
                            Toast.makeText(context, "✓ Kaydedildi ($count adet). Sıradaki taranıyor...", Toast.LENGTH_SHORT).show()
                            onClearDetail()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("save_finished_skt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Kaydet ve Sıradakine Geç",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KAYDET & SIRADAKİNİ TARA",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 12.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onClearDetail,
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("close_preview_detail_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Kapat",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
