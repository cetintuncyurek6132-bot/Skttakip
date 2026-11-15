package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
import com.example.ui.components.AddInventoryAuditModal
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private data class AuditCategorySpec(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val pastelBg: Color,
    val iconTint: Color
)

@Composable
fun InventoryTrackingScreen(
    audits: List<InventoryAuditEntity>,
    onRecordAudit: (
        category: String,
        auditDateMillis: Long,
        revenue: Double,
        fireCost: Double,
        targetRatio: Double,
        notes: String
    ) -> Unit,
    onDeleteAudit: (id: Int) -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val todayMidnight by com.example.util.rememberTodayMidnight()
    var activeCategoryForModal by remember { mutableStateOf<String?>(null) }

    val categories = remember {
        listOf(
            AuditCategorySpec(
                key = "GIDA",
                title = "GIDA",
                icon = Icons.Default.Fastfood,
                pastelBg = Color(0xFFF0FDFA),
                iconTint = TurquoiseDark
            ),
            AuditCategorySpec(
                key = "DOLAP",
                title = "DOLAP",
                icon = Icons.Default.AcUnit,
                pastelBg = Color(0xFFEFF6FF),
                iconTint = Color(0xFF1D4ED8)
            ),
            AuditCategorySpec(
                key = "GIDA DIŞI",
                title = "GIDA DIŞI",
                icon = Icons.Default.Category,
                pastelBg = Color(0xFFF5F3FF),
                iconTint = Color(0xFF6D28D9)
            )
        )
    }

    val fullDateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
    }

    val moneyFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0.##", symbols)
    }

    val ratioFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
        }
        DecimalFormat("0.##", symbols)
    }

    // Kategori kartına veya sağ üst "Ekle" butonuna tıklandığında açılan ModalBottomSheet
    activeCategoryForModal?.let { initialCategory ->
        AddInventoryAuditModal(
            initialCategory = initialCategory,
            onDismiss = { activeCategoryForModal = null },
            onSave = { chosenCategory, pickedDateMillis, revenue, fireCost, targetRatio, notes ->
                onRecordAudit(chosenCategory, pickedDateMillis, revenue, fireCost, targetRatio, notes)
                activeCategoryForModal = null
                Toast.makeText(
                    context,
                    "$chosenCategory envanteri kaydedildi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .testTag("inventory_tracking_screen")
    ) {
        // ÜST BAR: "Envanter Takibi" başlığı + Yeni Ekle butonu
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Slate50,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onBackClick() }
                            .testTag("inventory_tracking_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Slate900,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Envanter Takibi",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "90 günlük periyot & ciro/fire oranı analizi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { activeCategoryForModal = "GIDA" },
                    shape = RoundedCornerShape(12.dp),
                    color = TurquoisePrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("inventory_tracking_add_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Envanter Ekle",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ekle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark
                        )
                    }
                }
            }
        }

        // ANA İÇERİK: YAN YANA 3 KOMPAKT KART + GEÇMİŞ ENVANTER KAYITLARI LİSTESİ
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. KATEGORİ KARTLARI (GIDA, DOLAP, GIDA DIŞI - Row + weight(1f))
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { spec ->
                        val latestAudit = remember(audits, spec.key) {
                            audits.filter { it.category.equals(spec.key, ignoreCase = true) }
                                .maxByOrNull { it.auditDateMillis }
                        }

                        val daysElapsed = remember(latestAudit, todayMidnight) {
                            if (latestAudit == null || latestAudit.auditDateMillis <= 0L) {
                                null
                            } else {
                                calculateDaysSince(latestAudit.auditDateMillis, todayMidnight)
                            }
                        }

                        val remainingDays = remember(daysElapsed) {
                            if (daysElapsed == null) null else 90L - daysElapsed
                        }

                        // Envanter Oranı: (Fire / Ciro) * 100
                        val ratioPercentText = remember(latestAudit) {
                            if (latestAudit == null) {
                                "%-"
                            } else if (latestAudit.revenue > 0.0) {
                                val r = (latestAudit.fireCost / latestAudit.revenue) * 100.0
                                "%${ratioFormatter.format(r)}"
                            } else {
                                "%0"
                            }
                        }

                        val elapsedText = if (daysElapsed == null) {
                            "Geçen: -"
                        } else {
                            "Geçen: $daysElapsed Gün"
                        }

                        // Renk Kodu: Kalan gün > 30 Yeşil, 0..30 Sarı/Turuncu, < 0 Kırmızı ("X Gün Gecikti")
                        val (remainingLabel, statusColor, statusBgColor, statusBorderColor) = when {
                            remainingDays == null -> QuadrupleStatus(
                                "Kayıt Yok",
                                ExpiredRedDark,
                                ExpiredRedContainer,
                                ExpiredRedBorder
                            )
                            remainingDays < 0L -> QuadrupleStatus(
                                "${abs(remainingDays)} Gün Gecikti",
                                ExpiredRedDark,
                                ExpiredRedContainer,
                                ExpiredRedBorder
                            )
                            remainingDays in 0L..30L -> QuadrupleStatus(
                                "Kalan: $remainingDays Gün",
                                CriticalOrangeDark,
                                CriticalOrangeContainer,
                                CriticalOrangeBorder
                            )
                            else -> QuadrupleStatus(
                                "Kalan: $remainingDays Gün",
                                NormalGreenDark,
                                NormalGreenContainer,
                                NormalGreenBorder
                            )
                        }

                        Card(
                            onClick = { activeCategoryForModal = spec.key },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_card_${spec.key}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = statusBgColor.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.2.dp, statusBorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // Kategori İkonu + Başlığı
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = BorderStroke(1.dp, statusBorderColor),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = spec.icon,
                                            contentDescription = spec.title,
                                            tint = statusColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = spec.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                // Envanter Oranı: Ortada Büyük ve Tok "%X"
                                Text(
                                    text = ratioPercentText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                // Geçen Gün
                                Text(
                                    text = elapsedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                // Kalan Gün / Gecikme Rozeti
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusBgColor,
                                    border = BorderStroke(0.8.dp, statusBorderColor)
                                ) {
                                    Text(
                                        text = remainingLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. GEÇMİŞ ENVANTER KAYITLARI BAŞLIĞI
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Geçmiş Envanter Kayıtları",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TurquoisePrimary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${audits.size} Kayıt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. GEÇMİŞ ENVANTER LİSTESİ (Sol: Kategori & Tarih, Sağ: Ciro, Fire, Oran ve Not)
            if (audits.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Henüz envanter kaydı bulunmuyor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Yukarıdaki GIDA, DOLAP veya GIDA DIŞI kartlarından birine dokunarak ilk finansal envanter kaydını ekleyebilirsiniz.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(audits, key = { it.id }) { audit ->
                    val auditDaysAgo = calculateDaysSince(audit.auditDateMillis, todayMidnight)
                    val remaining = 90L - auditDaysAgo
                    val envanterTarihiStr = fullDateFormatter.format(Date(audit.auditDateMillis))

                    val actualRatio = if (audit.revenue > 0.0) {
                        (audit.fireCost / audit.revenue) * 100.0
                    } else {
                        0.0
                    }
                    val isExceedingTarget = audit.targetRatio > 0.0 && actualRatio > audit.targetRatio

                    val (badgeBg, badgeText) = when (audit.category.uppercase(Locale.forLanguageTag("tr-TR"))) {
                        "DOLAP" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
                        "GIDA DIŞI" -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
                        else -> Pair(TurquoisePrimary.copy(alpha = 0.16f), TurquoiseDark)
                    }

                    val ratioBadgeBg = if (isExceedingTarget) ExpiredRedContainer else NormalGreenContainer
                    val ratioBadgeText = if (isExceedingTarget) ExpiredRedDark else NormalGreenDark

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // SOL TARAF: Kategori Adı ve Tarih
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeBg
                                        ) {
                                            Text(
                                                text = audit.category,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeText,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ratioBadgeBg
                                        ) {
                                            Text(
                                                text = "Oran: %${ratioFormatter.format(actualRatio)}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ratioBadgeText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Slate700,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = envanterTarihiStr,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (remaining >= 0) "• Kalan: $remaining Gün" else "• ${abs(remaining)} Gün Gecikti",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (remaining < 0) ExpiredRedDark else Slate700
                                        )
                                    }
                                }

                                // SAĞ TARAF: Ciro, Fire, Hedef Oran + Sil Butonu
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "Ciro: ${moneyFormatter.format(audit.revenue)} ₺",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Fire: ${moneyFormatter.format(audit.fireCost)} ₺",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpiredRedDark
                                        )
                                        if (audit.targetRatio > 0.0) {
                                            Text(
                                                text = "Hedef: %${ratioFormatter.format(audit.targetRatio)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate700
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteAudit(audit.id) },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .testTag("delete_audit_${audit.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Kaydı Sil",
                                            tint = Slate500,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            // Varsa Not Bilgisi
                            if (audit.notes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(0.8.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Not: ${audit.notes}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate800,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun calculateDaysSince(auditDateMillis: Long, todayMidnight: Long): Long {
    val auditCal = Calendar.getInstance().apply {
        timeInMillis = auditDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMillis = maxOf(0L, todayMidnight - auditCal.timeInMillis)
    return TimeUnit.MILLISECONDS.toDays(diffMillis)
}

private data class QuadrupleStatus(
    val label: String,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)
