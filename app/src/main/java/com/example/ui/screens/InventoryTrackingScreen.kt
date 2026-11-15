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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class AuditCategorySpec(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val pastelBg: Color,
    val iconTint: Color
)

@Composable
fun InventoryTrackingScreen(
    audits: List<InventoryAuditEntity>,
    onRecordAudit: (category: String, notes: String) -> Unit,
    onDeleteAudit: (id: Int) -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val todayMidnight by com.example.util.rememberTodayMidnight()
    val noteInputs = remember { mutableStateMapOf<String, String>() }

    val categories = remember {
        listOf(
            AuditCategorySpec(
                key = "GIDA",
                title = "GIDA",
                subtitle = "Kuru gıda, bakliyat, atıştırmalık ve içecek reyonu",
                icon = Icons.Default.Fastfood,
                pastelBg = Color(0xFFF0FDFA),
                iconTint = TurquoiseDark
            ),
            AuditCategorySpec(
                key = "DOLAP",
                title = "DOLAP",
                subtitle = "Süt ürünleri, şarküteri, soğuk dolap ve dondurulmuş ürünler",
                icon = Icons.Default.AcUnit,
                pastelBg = Color(0xFFEFF6FF),
                iconTint = Color(0xFF1D4ED8)
            ),
            AuditCategorySpec(
                key = "GIDA DIŞI",
                title = "GIDA DIŞI",
                subtitle = "Temizlik, kişisel bakım, kağıt ürünleri ve ev gereçleri",
                icon = Icons.Default.Category,
                pastelBg = Color(0xFFF5F3FF),
                iconTint = Color(0xFF6D28D9)
            )
        )
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))
    }
    val dateTimeFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .testTag("inventory_tracking_screen")
    ) {
        // ÜST BAŞLIK ÇUBUĞU
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
                            text = "Mağaza Envanter Takibi",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Gıda, Dolap ve Gıda Dışı periyodik envanter kontrolü",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ANA İÇERİK (3 BÜYÜK KART + ENVANTER GEÇMİŞİ LİSTESİ)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3 BÜYÜK KATEGORİ KARTI (GIDA, DOLAP, GIDA DIŞI)
            items(categories, key = { it.key }) { spec ->
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

                val (daysText, statusColor, statusBgColor, statusBorderColor) = when {
                    daysElapsed == null -> QuadrupleStatus(
                        "Henüz Yapılmadı",
                        ExpiredRedDark,
                        ExpiredRedContainer,
                        ExpiredRedBorder
                    )
                    daysElapsed > 7L -> QuadrupleStatus(
                        "$daysElapsed Gün Önce",
                        ExpiredRedDark,
                        ExpiredRedContainer,
                        ExpiredRedBorder
                    )
                    daysElapsed in 3L..7L -> QuadrupleStatus(
                        "$daysElapsed Gün Önce",
                        CriticalOrangeDark,
                        CriticalOrangeContainer,
                        CriticalOrangeBorder
                    )
                    daysElapsed == 0L -> QuadrupleStatus(
                        "Bugün Yapıldı",
                        NormalGreenDark,
                        NormalGreenContainer,
                        NormalGreenBorder
                    )
                    else -> QuadrupleStatus(
                        "$daysElapsed Gün Önce",
                        NormalGreenDark,
                        NormalGreenContainer,
                        NormalGreenBorder
                    )
                }

                val lastDateFormatted = remember(latestAudit) {
                    if (latestAudit == null || latestAudit.auditDateMillis <= 0L) {
                        "Kayıt Yok"
                    } else {
                        dateFormatter.format(Date(latestAudit.auditDateMillis))
                    }
                }

                val currentNote = noteInputs[spec.key].orEmpty()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_card_${spec.key}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate50),
                    border = BorderStroke(1.2.dp, statusBorderColor.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Üst Satır: Kategori İkonu + Adı + Geçen Gün Rozeti
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = spec.pastelBg,
                                    border = BorderStroke(1.dp, spec.iconTint.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = spec.icon,
                                            contentDescription = spec.title,
                                            tint = spec.iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = spec.title,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = spec.subtitle,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate700,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Üzerinden geçen gün sayısı rozeti (>7 kırmızı, 3-7 turuncu, <3 yeşil)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = statusBgColor,
                                border = BorderStroke(1.dp, statusBorderColor)
                            ) {
                                Text(
                                    text = daysText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Orta Satır: Son Envanter Tarihi Kutusu
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Son Envanter Tarihi:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                }
                                Text(
                                    text = lastDateFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                            }
                        }

                        // Opsiyonel Not Girişi
                        OutlinedTextField(
                            value = currentNote,
                            onValueChange = { noteInputs[spec.key] = it },
                            placeholder = {
                                Text(
                                    text = "Envanter notu ekle (opsiyonel)...",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate500
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // "Envanter Yapıldı" Butonu
                        Button(
                            onClick = {
                                val noteToSave = noteInputs[spec.key].orEmpty().trim()
                                onRecordAudit(spec.key, noteToSave)
                                noteInputs[spec.key] = ""
                                Toast.makeText(
                                    context,
                                    "${spec.title} envanteri kaydedildi",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("inventory_done_button_${spec.key}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Envanter Yapıldı",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ENVANTER GEÇMİŞİ BAŞLIĞI
            item {
                Spacer(modifier = Modifier.height(6.dp))
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
                            text = "Envanter Geçmişi",
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

            // ENVANTER GEÇMİŞİ LİSTESİ
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
                                text = "Yukarıdaki kategorilerden \"Envanter Yapıldı\" butonuna basarak ilk kaydı oluşturabilirsiniz.",
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
                    val daysAgoLabel = when (auditDaysAgo) {
                        0L -> "Bugün"
                        1L -> "Dün"
                        else -> "$auditDaysAgo Gün Önce"
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TurquoisePrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = audit.category,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TurquoiseDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = dateTimeFormatter.format(Date(audit.auditDateMillis)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "• $daysAgoLabel",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                }

                                if (audit.notes.isNotBlank()) {
                                    Text(
                                        text = "Not: ${audit.notes}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate800
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteAudit(audit.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Kaydı Sil",
                                    tint = Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
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
