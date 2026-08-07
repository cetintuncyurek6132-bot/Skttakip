package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.platform.LocalContext
import com.example.util.ProductImageGenerator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.GameNavyCard
import com.example.ui.theme.GameNavyCardAccent
import com.example.ui.theme.GameNavyDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameScreen(
    targetCategory: String,
    onCategoryChange: (String) -> Unit,
    tourActive: Boolean,
    tourFinished: Boolean,
    tourQueue: List<Product>,
    currentQueueIndex: Int,
    tourLogs: List<TurKontrolKaydi>,
    lastSavedReport: TurRaporu?,
    onStartTour: () -> Unit,
    onRecordSold: (Product, Int) -> Unit,
    onRecordFire: (Product, Int) -> Unit,
    onRecordNotr: (Product) -> Unit,
    onUndoLastAction: () -> Unit,
    onCancelTour: () -> Unit,
    onResetTour: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    val categories = listOf(
        "Dolap Ürünleri",
        "Gıda Ürünleri",
        "Tüm Reyonlar"
    )

    // State for Dialogs
    var showSoldDialog by remember { mutableStateOf(false) }
    var showFireDialog by remember { mutableStateOf(false) }
    var soldQuantityText by remember { mutableStateOf("") }
    var fireQuantityText by remember { mutableStateOf("") }
    var soldErrorText by remember { mutableStateOf<String?>(null) }
    var fireErrorText by remember { mutableStateOf<String?>(null) }

    val currentProduct = if (tourQueue.isNotEmpty() && currentQueueIndex in tourQueue.indices) {
        tourQueue[currentQueueIndex]
    } else null

    Scaffold(
        containerColor = GameNavyDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (tourActive) {
                            onCancelTour()
                        } else {
                            onBackClick()
                        }
                    },
                    modifier = Modifier.testTag("cancel_game_link")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B3B2B),
                    border = BorderStroke(1.dp, NormalGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NormalGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🌅 SABAH KONTROL TURU",
                            color = NormalGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                if (tourActive && tourQueue.isNotEmpty()) {
                    IconButton(
                        onClick = onUndoLastAction,
                        enabled = tourLogs.isNotEmpty() && currentQueueIndex > 0,
                        modifier = Modifier.testTag("undo_tour_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Geri Al",
                            tint = if (tourLogs.isNotEmpty() && currentQueueIndex > 0) TurquoisePrimary else Color.White.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. REYON SELECTOR CAROUSEL (Show when not active or finished)
            if (!tourActive && !tourFinished) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        val isSelected = targetCategory == cat
                        val iconText = when (cat) {
                            "Dolap Ürünleri" -> "❄️"
                            "Gıda Ürünleri" -> "🍞"
                            else -> "🏪"
                        }
                        Surface(
                            onClick = { onCategoryChange(cat) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) TurquoisePrimary else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) TurquoisePrimary else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = iconText, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. MAIN CONTENT CONTAINER
            if (tourFinished) {
                // TOUR SUMMARY SCREEN
                TourSummaryView(
                    report = lastSavedReport,
                    logs = tourLogs,
                    targetCategory = targetCategory,
                    onNewTourClick = onResetTour
                )
            } else if (!tourActive) {
                // WELCOME & START TOUR BANNER
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameNavyCard),
                    border = BorderStroke(1.dp, GameNavyCardAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🎯 SIRA (KUYRUK) BAZLI KONTROL SİSTEMİ",
                                color = TurquoisePrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Seçili Reyon: ${targetCategory.uppercase()}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Bu turda sadece SKT'si Kritik (0-7 gün), Yaklaşan Riskli (8-30 gün) ve Önemli ürünler sırayla gösterilecektir. Süresi çok uzun normal ürünler tura dahil edilmez.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // START BUTTON
                        Button(
                            onClick = onStartTour,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_game_circular_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NormalGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Başla")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SABAH KONTROL TURUNU BAŞLAT",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // INFO SUMMARY CARDS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔴 Kritik (0-7 Gün)", color = ExpiredRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Öncelikli Sıra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🟠 Yakın (8-30 Gün)", color = CriticalOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Risk Takibi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // ACTIVE TOUR QUEUE SCREEN
                if (tourQueue.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = GameNavyCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tamam",
                                tint = NormalGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "KONTROL EDİLECEK ÜRÜN BULUNAMADI!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Seçilen '${targetCategory}' reyonunda SKT'si 30 günün altında olan veya riskli ürün yok. Tüm ürünler güvende!",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onResetTour,
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                Text("TAMAM / BAŞKA REYON SEÇ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (currentProduct != null) {
                    val remainingDays = currentProduct.getRemainingDays()
                    val totalQueue = tourQueue.size
                    val currentDisplayIndex = currentQueueIndex + 1
                    val progressRatio = currentDisplayIndex.toFloat() / totalQueue.toFloat()

                    // QUEUE PROGRESS BAR & HEADER
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GameNavyCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ÜRÜN $currentDisplayIndex / $totalQueue",
                                    color = TurquoisePrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = currentProduct.kategori,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = TurquoisePrimary,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PRODUCT CONTROL CARD (Ürün Kontrol Kartı)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("product_control_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                remainingDays <= 0 -> Color(0xFF381414)
                                remainingDays <= 7 -> Color(0xFF3B2314)
                                remainingDays <= 30 -> Color(0xFF3B3314)
                                else -> GameNavyCard
                            }
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                remainingDays <= 7 -> ExpiredRed
                                remainingDays <= 30 -> CriticalOrange
                                else -> TurquoisePrimary
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Info
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            remainingDays <= 0 -> ExpiredRed
                                            remainingDays <= 7 -> ExpiredRed
                                            remainingDays <= 30 -> CriticalOrange
                                            else -> NormalGreen
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                remainingDays <= 0 -> "🔴 SKT GEÇMİŞ ($remainingDays Gün)"
                                                remainingDays <= 7 -> "🔴 KRİTİK SKT ($remainingDays Gün Kaldı)"
                                                remainingDays <= 30 -> "🟠 YAKIN SKT ($remainingDays Gün Kaldı)"
                                                else -> "🟢 NORMAL / ÖNEMLİ"
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    Text(
                                        text = "Barkod: ${currentProduct.barkod}",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = currentProduct.urunAdi,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    lineHeight = 26.sp
                                )

                                if (currentProduct.urunKodu.isNotBlank()) {
                                    Text(
                                        text = "Ürün Kodu: ${currentProduct.urunKodu}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Date & System Stock Card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("SKT TARİHİ", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
                                        Text(
                                            text = if (currentProduct.sktTarihi > 0L) sdf.format(Date(currentProduct.sktTarihi)) else "SKT Girilmedi",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("SİSTEM MEVCUT STOK", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${currentProduct.stokAdedi} ADET",
                                            color = TurquoisePrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }

                            // Bottom 3 Big Action Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "BU ÜRÜN İÇİN AKSİYON SEÇİN:",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // A) SATILDI
                                Button(
                                    onClick = {
                                        soldQuantityText = currentProduct.stokAdedi.toString()
                                        soldErrorText = null
                                        showSoldDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("action_sold_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NormalGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = "Satıldı")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A) SATILDI (STOK DÜŞ)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }

                                // B) KALDIRILDI / FIRE
                                Button(
                                    onClick = {
                                        fireQuantityText = currentProduct.stokAdedi.toString()
                                        fireErrorText = null
                                        showFireDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("action_fire_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.RemoveCircle, contentDescription = "Fire")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("B) KALDIRILDI / FİRE (RAFTAN ÇIKAR)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }

                                // C) NOTR (Single click)
                                OutlinedButton(
                                    onClick = {
                                        onRecordNotr(currentProduct)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("action_notr_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Nötr")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("C) NÖTR (DEĞİŞİKLİK YOK - TEK TIK DEVAM)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG A: SATILDI ADET GIRISI
    if (showSoldDialog && currentProduct != null) {
        AlertDialog(
            onDismissRequest = { showSoldDialog = false },
            title = {
                Text("Kaç Adet Satıldı?", fontWeight = FontWeight.Bold, color = Slate900)
            },
            text = {
                Column {
                    Text(
                        text = "${currentProduct.urunAdi}\nMevcut Stok: ${currentProduct.stokAdedi} adet",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = soldQuantityText,
                        onValueChange = {
                            soldQuantityText = it
                            soldErrorText = null
                        },
                        label = { Text("Satılan Adet") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = soldErrorText != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (soldErrorText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = soldErrorText!!,
                            color = ExpiredRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = soldQuantityText.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            soldErrorText = "Geçerli bir adet giriniz"
                        } else if (qty > currentProduct.stokAdedi) {
                            soldErrorText = "Satılan adet mevcut stoktan (${currentProduct.stokAdedi}) fazla olamaz!"
                        } else {
                            showSoldDialog = false
                            onRecordSold(currentProduct, qty)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NormalGreen)
                ) {
                    Text("ONAYLA VE DEVAM ET", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoldDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // DIALOG B: FIRE ADET GIRISI
    if (showFireDialog && currentProduct != null) {
        AlertDialog(
            onDismissRequest = { showFireDialog = false },
            title = {
                Text("Kaç Adet Kaldırıldı / Fire Oldu?", fontWeight = FontWeight.Bold, color = Slate900)
            },
            text = {
                Column {
                    Text(
                        text = "${currentProduct.urunAdi}\nFiziksel raftan kaldırılan fire adedini girin. Otomatik fire sebebi: SKT.",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fireQuantityText,
                        onValueChange = {
                            fireQuantityText = it
                            fireErrorText = null
                        },
                        label = { Text("Kaldırılan / Fire Adedi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = fireErrorText != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (fireErrorText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fireErrorText!!,
                            color = ExpiredRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = fireQuantityText.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            fireErrorText = "Geçerli bir adet giriniz"
                        } else if (qty > currentProduct.stokAdedi) {
                            fireErrorText = "Fire adedi mevcut stoktan (${currentProduct.stokAdedi}) fazla olamaz!"
                        } else {
                            showFireDialog = false
                            onRecordFire(currentProduct, qty)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("ONAYLA VE DEVAM ET", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFireDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun TourSummaryView(
    report: TurRaporu?,
    logs: List<TurKontrolKaydi>,
    targetCategory: String,
    onNewTourClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tour_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GameNavyCard),
        border = BorderStroke(1.dp, GameNavyCardAccent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Rapor",
                tint = TurquoisePrimary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (report?.tamamlandiMi == false) "⚠️ YARIM KALAN KONTROL TURU RAPORU" else "🎉 KONTROL TURU TAMAMLANDI",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Reyon: ${targetCategory} | Toplam ${logs.size} Ürün Denetlendi",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // STATS 3-COLUMN GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val satilanLogs = logs.filter { it.durum == "SATILDI" }
                val fireLogs = logs.filter { it.durum == "FIRE" }
                val notrCount = logs.count { it.durum == "NOTR" }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, NormalGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SATILDI", color = NormalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${satilanLogs.size} Ürün", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${satilanLogs.sumOf { it.islemAdedi }} Adet", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, ExpiredRed)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("FİRE", color = ExpiredRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${fireLogs.size} Ürün", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${fireLogs.sumOf { it.islemAdedi }} Adet", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("NÖTR", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$notrCount Ürün", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Değişiklik Yok", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "TURDA YAPILAN İŞLEM DETAYLARI:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.urunAdiSnapshot,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (log.barkodSnapshot.isNotBlank()) {
                                Text(
                                    text = "Barkod: ${log.barkodSnapshot}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (log.durum) {
                                "SATILDI" -> NormalGreen
                                "FIRE" -> ExpiredRed
                                else -> Color.White.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when (log.durum) {
                                    "SATILDI" -> "SATILDI (-${log.islemAdedi})"
                                    "FIRE" -> "FİRE (-${log.islemAdedi})"
                                    else -> "NÖTR"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (report != null) {
                    Button(
                        onClick = {
                            ProductImageGenerator.shareTourReportAsImage(
                                context = context,
                                rapor = report,
                                logs = logs
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_tour_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }

                Button(
                    onClick = onNewTourClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("reset_tour_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("YENİ TUR", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
