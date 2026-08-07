package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.util.ProductImageGenerator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InspectionReport
import com.example.data.TurRaporu
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    reports: List<InspectionReport>,
    turRaporlari: List<TurRaporu> = emptyList(),
    onBackClick: () -> Unit = {},
    onDeleteReport: (Int) -> Unit = {},
    onDeleteTurRaporu: (Int) -> Unit = {},
    onClearAllReports: () -> Unit = {}
) {
    var showClearAllDialog by remember { mutableStateOf(false) }
    var reportToDeleteId by remember { mutableStateOf<Int?>(null) }
    var turToDeleteId by remember { mutableStateOf<Int?>(null) }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "Tüm Raporlar Silinsin Mi?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Kayıtlı tüm sayım ve kontrol turu raporları silinecektir. Bu işlem geri alınamaz.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllReports()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("TÜMÜNÜ SİL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (turToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { turToDeleteId = null },
            title = {
                Text(
                    text = "Kontrol Tur Raporu Silinsin Mi?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Seçilen kontrol turu raporu ve detayları silinecektir.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        turToDeleteId?.let { onDeleteTurRaporu(it) }
                        turToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("SİL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { turToDeleteId = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (reportToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { reportToDeleteId = null },
            title = {
                Text(
                    text = "Rapor Silinsin Mi?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Seçilen kontrol turu raporu kalıcı olarak silinecektir.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportToDeleteId?.let { onDeleteReport(it) }
                        reportToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("SİL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDeleteId = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TUR RAPOR GEÇMİŞİ",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FİRE VE İSTATİSTİK RAPORLARI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (reports.isNotEmpty()) {
                        Button(
                            onClick = { showClearAllDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("clear_all_reports_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Tüm Raporları Sil",
                                tint = ExpiredRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tümünü Sil",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpiredRed
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (reports.isEmpty() && turRaporlari.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Rapor Yok",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Henüz tamamlanmış kontrol turu raporu bulunmuyor.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ana panolden 'Sabah Kontrol Turu' başlattığınızda sayım sonuçları ve fire tutarları burada listelenecektir.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (turRaporlari.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SABAH KONTROL TURU RAPORLARI (${turRaporlari.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(turRaporlari, key = { "tur_${it.id}" }) { turRapor ->
                        TurRaporuCard(
                            rapor = turRapor,
                            onDeleteClick = { turToDeleteId = turRapor.id }
                        )
                    }
                }

                if (reports.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "GENEL SAYIM VE DENETİM RAPORLARI (${reports.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(reports, key = { "rep_${it.id}" }) { report ->
                        InspectionReportCard(
                            report = report,
                            onDeleteClick = { reportToDeleteId = report.id }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun TurRaporuCard(
    rapor: TurRaporu,
    onDeleteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale("tr", "TR"))
    val dateString = dateFormat.format(Date(rapor.turTarihi))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tur_report_item_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Tarih",
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateString,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = {
                            ProductImageGenerator.shareTourReportAsImage(
                                context = context,
                                rapor = rapor,
                                logs = emptyList()
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366),
                        modifier = Modifier.testTag("share_tur_report_whatsapp_${rapor.id}")
                    ) {
                        Text(
                            text = "WhatsApp",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Raporu Sil",
                            tint = ExpiredRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoisePrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Reyon: ${rapor.hedefReyon}",
                        color = TurquoisePrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (rapor.tamamlandiMi) NormalGreen.copy(alpha = 0.15f) else CriticalOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (rapor.tamamlandiMi) "TAMAMLANDI" else "YARIM KALDI",
                        color = if (rapor.tamamlandiMi) NormalGreen else CriticalOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOPLAM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${rapor.toplamUrunSayisi} Ürün", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SATILDI", color = NormalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${rapor.satilanUrunSayisi} Ürün (${rapor.toplamSatilanAdet} Adet)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FİRE", color = ExpiredRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${rapor.fireUrunSayisi} Ürün (${rapor.toplamFireAdet} Adet)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun InspectionReportCard(
    report: InspectionReport,
    onDeleteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale("tr", "TR"))
    val dateString = dateFormat.format(Date(report.tarih))
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    val fireString = currencyFormat.format(report.fireTutari)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_item_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Date and Reyon + Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Tarih",
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateString,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = report.reyonAdi,
                            color = TurquoisePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        onClick = {
                            val dummyTur = com.example.data.TurRaporu(
                                id = report.id,
                                turTarihi = report.tarih,
                                hedefReyon = report.reyonAdi,
                                toplamUrunSayisi = report.tarananUrunSayisi,
                                satilanUrunSayisi = (report.tarananUrunSayisi - report.suresiGecenSayisi).coerceAtLeast(0),
                                toplamSatilanAdet = (report.tarananUrunSayisi - report.suresiGecenSayisi).coerceAtLeast(0),
                                fireUrunSayisi = report.suresiGecenSayisi,
                                toplamFireAdet = report.suresiGecenSayisi,
                                notrUrunSayisi = report.kritikUrunSayisi,
                                tamamlandiMi = true
                            )
                            ProductImageGenerator.shareTourReportAsImage(
                                context = context,
                                rapor = dummyTur,
                                logs = emptyList()
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366),
                        modifier = Modifier.testTag("share_inspection_report_whatsapp_${report.id}")
                    ) {
                        Text(
                            text = "WhatsApp",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_report_${report.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Raporu Sil",
                            tint = ExpiredRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Stats Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Slate100.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Total Scanned
                StatColumnItem(
                    title = "Taranan",
                    value = "${report.tarananUrunSayisi}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Expired
                StatColumnItem(
                    title = "Süresi Geçen",
                    value = "${report.suresiGecenSayisi}",
                    color = if (report.suresiGecenSayisi > 0) ExpiredRed else NormalGreen
                )
                // Critical
                StatColumnItem(
                    title = "Kritik / Yakın",
                    value = "${report.kritikUrunSayisi}",
                    color = if (report.kritikUrunSayisi > 0) CriticalOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Financial Loss / Fire footer
            if (report.fireTutari > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tahmini Fire Tutarı:",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Text(
                        text = fireString,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = ExpiredRed
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumnItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
