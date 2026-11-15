package com.example.ui.screens.adetsel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdetselKayit
import com.example.ui.components.AppBottomSheetWrapper
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlueBorder
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark
import com.example.util.SafeDateFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdetselSayimDetailModal(
    kayit: AdetselKayit,
    onDismiss: () -> Unit
) {
    val absFark = abs(kayit.farkAdet)
    val (statusTitle, statusBg, statusTextColor, statusBorderColor) = when (kayit.sayimSonucu) {
        "EKSIK" -> Quadruple(
            "Eksik Sayım",
            ExpiredRedContainer,
            ExpiredRedDark,
            ExpiredRedBorder
        )
        "FAZLA" -> Quadruple(
            "Fazla Sayım",
            WarningBlueContainer,
            WarningBlueDark,
            WarningBlueBorder
        )
        else -> Quadruple(
            "Tam Sayım",
            NormalGreenContainer,
            NormalGreenDark,
            NormalGreenBorder
        )
    }

    val farkDisplayText = when (kayit.sayimSonucu) {
        "EKSIK" -> {
            val miktar = if (absFark == 0) 1 else absFark
            "-$miktar Adet Eksik"
        }
        "FAZLA" -> {
            val miktar = if (absFark == 0) 1 else absFark
            "+$miktar Adet Fazla"
        }
        else -> null
    }

    val eklenmeTarihiFormatted = remember(kayit.eklenmeTarihi) {
        if (kayit.eklenmeTarihi <= 0L) "-" else SafeDateFormatter.shortDateTime(kayit.eklenmeTarihi)
    }

    val islemTarihiFormatted = remember(kayit.islemTarihi) {
        if (kayit.islemTarihi <= 0L) "-" else SafeDateFormatter.shortDateTime(kayit.islemTarihi)
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        enableVerticalScroll = true
    ) { dismissSheet ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. ÜST BAŞLIK VE KAPAT (X) BUTONU
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
                        shape = CircleShape,
                        color = TurquoisePrimary.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sayım Detayı",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "Tamamlanan adetsel sayım özeti",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { dismissSheet() }
                        .testTag("close_sayim_detail_icon_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. ÜRÜN ADI VE KODU KARTI
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Slate50
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = kayit.urunAdi.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TurquoisePrimary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Kod: ${kayit.getDisplayCode()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (kayit.barkod.isNotBlank() && kayit.barkod != kayit.urunKodu) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = "Barkod: ${kayit.barkod}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. SAYIM DURUMU KUTUCUĞU
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Slate50
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusBg,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = statusTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sayım Durumu",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusBorderColor)
                    ) {
                        Text(
                            text = statusTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusTextColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 4. RAF ADEDİ KUTUCUĞU
            DetailInfoRowBox(
                icon = Icons.Default.Inventory2,
                iconTint = TurquoiseDark,
                label = "Raf Adedi",
                value = "${kayit.sayilanAdet} Adet",
                valueColor = Slate900
            )

            // 5. FARK MİKTARI (Sadece Eksik veya Fazlaysa)
            if (farkDisplayText != null) {
                DetailInfoRowBox(
                    icon = Icons.Default.SwapVert,
                    iconTint = statusTextColor,
                    label = "Fark Miktarı",
                    value = farkDisplayText,
                    valueColor = statusTextColor
                )
            }

            // 6. SİSTEME EKLENME TARİHİ
            DetailInfoRowBox(
                icon = Icons.Default.CalendarToday,
                iconTint = TurquoiseDark,
                label = "Sisteme Eklenme Tarihi",
                value = eklenmeTarihiFormatted,
                valueColor = Slate900
            )

            // 7. SAYIMIN YAPILDIĞI TARİH
            DetailInfoRowBox(
                icon = Icons.Default.EventAvailable,
                iconTint = TurquoiseDark,
                label = "Sayımın Yapıldığı Tarih",
                value = islemTarihiFormatted,
                valueColor = Slate900
            )

            // 8. NOTLAR (Varsa göster)
            if (kayit.notlar.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Slate50
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sayım Notu",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }
                        Text(
                            text = kayit.notlar,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 9. ALT KAPAT BUTONU
            Button(
                onClick = { dismissSheet() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("close_sayim_detail_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark)
            ) {
                Text(
                    text = "Kapat",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRowBox(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Slate50
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
            }

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
