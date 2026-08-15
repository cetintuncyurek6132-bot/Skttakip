package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import com.example.data.getDisplayName
import com.example.data.getDisplayCode
import com.example.data.isDolapProduct
import com.example.ui.components.ReportPreviewDialog
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductImageGenerator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameScreen(
    targetCategory: String,
    onCategoryChange: (String) -> Unit,
    tourActive: Boolean,
    tourFinished: Boolean,
    isPaused: Boolean = false,
    tourQueue: List<Product>,
    currentQueueIndex: Int,
    tourLogs: List<TurKontrolKaydi>,
    tourScore: Int = 0,
    tourStreak: Int = 0,
    lastActionMessage: String? = null,
    lastSavedReport: TurRaporu?,
    onStartTour: () -> Unit,
    onPauseTour: () -> Unit = {},
    onResumeTour: () -> Unit = {},
    onClearLastActionMessage: () -> Unit = {},
    onRecordSold: (Product, Int) -> Unit,
    onRecordFire: (Product, Int) -> Unit,
    onRecordNotr: (Product) -> Unit,
    onUndoLastAction: () -> Unit,
    onCancelTour: () -> Unit,
    onResetTour: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    // Action Modals State
    var showSoldDialog by remember { mutableStateOf(false) }
    var showFireDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }

    var soldQuantityText by remember { mutableStateOf("1") }
    var fireQuantityText by remember { mutableStateOf("1") }
    var soldErrorText by remember { mutableStateOf<String?>(null) }
    var fireErrorText by remember { mutableStateOf<String?>(null) }

    // Floating action toast auto-hide
    LaunchedEffect(lastActionMessage) {
        if (lastActionMessage != null) {
            delay(2200)
            onClearLastActionMessage()
        }
    }

    val currentProduct = if (tourQueue.isNotEmpty() && currentQueueIndex in tourQueue.indices) {
        tourQueue[currentQueueIndex]
    } else null

    Scaffold(
        containerColor = Color(0xFF0B1120) // Deep Dark Modern Canvas
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP NAVIGATION / APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (tourActive) {
                            showCancelDialog = true
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
                    color = Color(0xFF0F2E33),
                    border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (tourActive) NormalGreen else TurquoisePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🌅 SABAH KONTROL TURU",
                            color = Color(0xFF5EEAD4),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
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
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Geri Al",
                            tint = if (tourLogs.isNotEmpty() && currentQueueIndex > 0) TurquoisePrimary else Color(0xFF334155)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // MAIN CONTENT ROUTER
            if (tourFinished) {
                // 1. TOUR FINISHED / SUMMARY SCREEN
                TourSummaryView(
                    report = lastSavedReport,
                    logs = tourLogs,
                    targetCategory = targetCategory,
                    onNewTourClick = onResetTour,
                    onBackToHome = onBackClick
                )
            } else if (!tourActive) {
                // 2. WELCOME / TOUR INTRO SCREEN
                TourWelcomeScreen(
                    queueSize = tourQueue.size,
                    currentIndex = currentQueueIndex,
                    onStartTour = onStartTour,
                    onResumeTour = onResumeTour
                )
            } else {
                // 3. ACTIVE SINGLE-PRODUCT TOUR VIEW
                if (tourQueue.isEmpty()) {
                    TourEmptyState(onResetTour = onResetTour)
                } else if (currentProduct != null) {
                    TourActiveProductCard(
                        product = currentProduct,
                        currentIndex = currentQueueIndex,
                        totalCount = tourQueue.size,
                        tourScore = tourScore,
                        tourStreak = tourStreak,
                        lastActionMessage = lastActionMessage,
                        onOpenSold = {
                            soldQuantityText = "1"
                            soldErrorText = null
                            showSoldDialog = true
                        },
                        onOpenFire = {
                            fireQuantityText = "1"
                            fireErrorText = null
                            showFireDialog = true
                        },
                        onRecordNotr = {
                            onRecordNotr(currentProduct)
                        },
                        onPauseClick = {
                            onPauseTour()
                            showPauseDialog = true
                        }
                    )
                }
            }
        }
    }

    // MODAL 1: SATILDI (QUANTITY SELECTION)
    if (showSoldDialog && currentProduct != null) {
        val maxStock = maxOf(1, currentProduct.stokAdedi)
        val currentQty = soldQuantityText.toIntOrNull() ?: 1

        AlertDialog(
            onDismissRequest = { showSoldDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NormalGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = NormalGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("🟢 Ürün Satıldı", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentProduct.getDisplayName(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mevcut Sistem Stoğu: ${currentProduct.stokAdedi} Adet",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Select Quantity Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5, 10, currentProduct.stokAdedi).distinct().filter { it in 1..currentProduct.stokAdedi }.forEach { qtyOption ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentQty == qtyOption) NormalGreen else Color(0xFF334155),
                                modifier = Modifier.clickable {
                                    soldQuantityText = qtyOption.toString()
                                    soldErrorText = null
                                }
                            ) {
                                Text(
                                    text = if (qtyOption == currentProduct.stokAdedi) "Tümü ($qtyOption)" else "+$qtyOption",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stepper Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val newQty = maxOf(1, currentQty - 1)
                                soldQuantityText = newQty.toString()
                                soldErrorText = null
                            },
                            enabled = currentQty > 1,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (currentQty > 1) Color(0xFF334155) else Color(0xFF1E293B))
                        ) {
                            Text("-", color = if (currentQty > 1) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = soldQuantityText,
                            onValueChange = {
                                soldQuantityText = it.filter { char -> char.isDigit() }
                                soldErrorText = null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.width(90.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = {
                                val newQty = minOf(maxStock, currentQty + 1)
                                soldQuantityText = newQty.toString()
                                soldErrorText = null
                            },
                            enabled = currentQty < maxStock,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (currentQty < maxStock) NormalGreen else Color(0xFF334155))
                        ) {
                            Text("+", color = if (currentQty < maxStock) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                    }

                    // Remaining stock live calculation
                    Spacer(modifier = Modifier.height(10.dp))
                    val previewNewStock = maxOf(0, currentProduct.stokAdedi - currentQty)
                    Text(
                        text = "İşlem Sonrası Kalan Stok: $previewNewStock Adet",
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (soldErrorText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = soldErrorText!!,
                            color = ExpiredRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = soldQuantityText.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            soldErrorText = "Lütfen geçerli bir adet giriniz"
                        } else if (qty > currentProduct.stokAdedi) {
                            soldErrorText = "Satılan adet stoktan (${currentProduct.stokAdedi}) fazla olamaz!"
                        } else {
                            showSoldDialog = false
                            onRecordSold(currentProduct, qty)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NormalGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_sold_button")
                ) {
                    Text("✓ Satışı Onayla", fontWeight = FontWeight.Black, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoldDialog = false }) {
                    Text("Vazgeç", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // MODAL 2: FİRE / KALDIRILDI (QUANTITY SELECTION)
    if (showFireDialog && currentProduct != null) {
        val maxStock = maxOf(1, currentProduct.stokAdedi)
        val currentQty = fireQuantityText.toIntOrNull() ?: 1

        AlertDialog(
            onDismissRequest = { showFireDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ExpiredRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpiredRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("🔴 Fireye Ayrıldı", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentProduct.getDisplayName(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mevcut Sistem Stoğu: ${currentProduct.stokAdedi} Adet",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Select Quantity Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5, 10, currentProduct.stokAdedi).distinct().filter { it in 1..currentProduct.stokAdedi }.forEach { qtyOption ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentQty == qtyOption) ExpiredRed else Color(0xFF334155),
                                modifier = Modifier.clickable {
                                    fireQuantityText = qtyOption.toString()
                                    fireErrorText = null
                                }
                            ) {
                                Text(
                                    text = if (qtyOption == currentProduct.stokAdedi) "Tümü ($qtyOption)" else "+$qtyOption",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stepper Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val newQty = maxOf(1, currentQty - 1)
                                fireQuantityText = newQty.toString()
                                fireErrorText = null
                            },
                            enabled = currentQty > 1,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (currentQty > 1) Color(0xFF334155) else Color(0xFF1E293B))
                        ) {
                            Text("-", color = if (currentQty > 1) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = fireQuantityText,
                            onValueChange = {
                                fireQuantityText = it.filter { char -> char.isDigit() }
                                fireErrorText = null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.width(90.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = {
                                val newQty = minOf(maxStock, currentQty + 1)
                                fireQuantityText = newQty.toString()
                                fireErrorText = null
                            },
                            enabled = currentQty < maxStock,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (currentQty < maxStock) ExpiredRed else Color(0xFF334155))
                        ) {
                            Text("+", color = if (currentQty < maxStock) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                    }

                    // Remaining stock live calculation
                    Spacer(modifier = Modifier.height(10.dp))
                    val previewNewStock = maxOf(0, currentProduct.stokAdedi - currentQty)
                    Text(
                        text = "İşlem Sonrası Kalan Stok: $previewNewStock Adet",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (fireErrorText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fireErrorText!!,
                            color = ExpiredRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = fireQuantityText.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            fireErrorText = "Lütfen geçerli bir adet giriniz"
                        } else if (qty > currentProduct.stokAdedi) {
                            fireErrorText = "Fire adedi stoktan (${currentProduct.stokAdedi}) fazla olamaz!"
                        } else {
                            showFireDialog = false
                            onRecordFire(currentProduct, qty)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_fire_button")
                ) {
                    Text("🗑 Fireyi Onayla", fontWeight = FontWeight.Black, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFireDialog = false }) {
                    Text("Vazgeç", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // MODAL 3: PAUSE DIALOG
    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = {
                showPauseDialog = false
                onResumeTour()
            },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("⏸ Tur Duraklatıldı", fontWeight = FontWeight.Black, color = Color.White)
            },
            text = {
                Text(
                    text = "Kontrol turu duraklatıldı. Şimdi çıkabilir ve daha sonra kaldığınız yerden devam edebilirsiniz.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPauseDialog = false
                        onResumeTour()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text("▶ Devam Et", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPauseDialog = false
                        onCancelTour()
                    }
                ) {
                    Text("Turu Bitir ve Kaydet", color = ExpiredRed)
                }
            }
        )
    }

    // MODAL 4: CANCEL CONFIRMATION DIALOG
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Turu Sonlandır", fontWeight = FontWeight.Black, color = Color.White)
            },
            text = {
                Text(
                    text = if (tourLogs.isNotEmpty()) {
                        "Kontrol turundan çıkmak istiyor musunuz? Şimdiye kadar yaptığınız ${tourLogs.size} kontrol kaydedilecek ve rapor oluşturulacaktır."
                    } else {
                        "Kontrol turundan çıkmak istiyor musunuz?"
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelTour()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Evet, Çık", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("İptal", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SUB-SCREEN 1: WELCOME & START BANNER
// -------------------------------------------------------------
@Composable
private fun TourWelcomeScreen(
    queueSize: Int,
    currentIndex: Int,
    onStartTour: () -> Unit,
    onResumeTour: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F766E),
                Color(0xFF134E4A),
                Color(0xFF0F172A)
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tour_welcome_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(gradientBrush)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🎮 OYUN MODUNDA HIZLI DENETİM",
                            color = Color(0xFFCCFBF1),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Sabah Kontrol Turu",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Mağazadaki SKT tarihi 20 gün ve altındaki ürünleri tek tek kontrol edin. Satılanları düşürün, fireleri ayırın, reyonları güvende tutun.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big Action Button
                    if (queueSize > 0 && currentIndex > 0 && currentIndex < queueSize) {
                        Button(
                            onClick = onResumeTour,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("resume_tour_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NormalGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KALDIĞIM YERDEN DEVAM ET (${currentIndex + 1}/$queueSize)",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onStartTour,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("restart_tour_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sıfırdan Yeniden Başlat", fontSize = 12.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onStartTour,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_tour_main_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KONTROL TURUNU BAŞLAT",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Tour Rules & Step Overview
        Text(
            text = "TUR KONTROL AKIŞI",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF0E7490).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("❄️ 1. ADIM", color = TurquoisePrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dolap Ürünleri", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Süt, peynir, şarküteri, et ve yoğurtlar", color = Color(0xFF94A3B8), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFFC2410C).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📦 2. ADIM", color = CriticalOrange, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Gıda Ürünleri", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Bisküvi, bakliyat, konserve ve diğerleri", color = Color(0xFF94A3B8), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }

        // Action Keys Description
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔘 3 BASİT BUTONLA YÖNETİN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NormalGreen))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🟢 SATILDI:", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ürün rafta yoksa adedi seçin, stoktan düşsün.", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ExpiredRed))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔴 FİREYE AYRILDI:", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SKT geçtiyse veya bozulduysa fireye atın.", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚪ NÖTR:", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ürün rafta sağlam duruyorsa direkt sonrakine geçin.", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREEN 2: ACTIVE SINGLE-PRODUCT CARD WITH HUD
// -------------------------------------------------------------
@Composable
private fun TourActiveProductCard(
    product: Product,
    currentIndex: Int,
    totalCount: Int,
    tourScore: Int,
    tourStreak: Int,
    lastActionMessage: String?,
    onOpenSold: () -> Unit,
    onOpenFire: () -> Unit,
    onRecordNotr: () -> Unit,
    onPauseClick: () -> Unit
) {
    val remainingDays = product.getRemainingDays()
    val isDolap = product.isDolapProduct()
    val progressRatio = ((currentIndex + 1).toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD: PROGRESS & SCORE
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ÜRÜN ${currentIndex + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = " / $totalCount",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Gamification Score & Streak Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tourStreak > 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEA580C).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFEA580C))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${tourStreak}x", color = Color(0xFFFB923C), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$tourScore P", color = Color(0xFFFDE68A), fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Duraklat", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Smooth Progress Bar
            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isDolap) TurquoisePrimary else CriticalOrange,
                trackColor = Color(0xFF1E293B)
            )

            // Feedback Message Toast Pill (if any)
            AnimatedVisibility(
                visible = lastActionMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F766E).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = lastActionMessage ?: "",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CENTER: SINGLE-PRODUCT HERO CARD (ANIMATED)
        AnimatedContent(
            targetState = product,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "ProductCardTransition"
        ) { targetProduct ->
            val targetDays = targetProduct.getRemainingDays()
            val targetIsDolap = targetProduct.isDolapProduct()

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("active_product_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(
                    1.5.dp,
                    when {
                        targetDays <= 0 -> ExpiredRed
                        targetDays <= 3 -> ExpiredRed
                        targetDays <= 7 -> CriticalOrange
                        else -> Color(0xFF334155)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Pill (Dolap vs Gıda)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (targetIsDolap) Color(0xFF0E7490).copy(alpha = 0.3f) else Color(0xFFC2410C).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (targetIsDolap) Color(0xFF06B6D4) else Color(0xFFF97316))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (targetIsDolap) "❄️ DOLAP ÜRÜNÜ" else "📦 GIDA ÜRÜNÜ",
                                    color = if (targetIsDolap) Color(0xFF67E8F9) else Color(0xFFFDBA74),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Remaining Days Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                targetDays <= 0 -> Color(0xFF991B1B)
                                targetDays <= 3 -> Color(0xFF991B1B)
                                targetDays <= 7 -> Color(0xFF9A3412)
                                else -> Color(0xFF065F46)
                            }
                        ) {
                            Text(
                                text = when {
                                    targetDays <= 0 -> "🔴 SÜRESİ GEÇMİŞ"
                                    targetDays <= 3 -> "🔴 $targetDays GÜN KALDI"
                                    targetDays <= 7 -> "🟠 $targetDays GÜN KALDI"
                                    else -> "🟡 $targetDays GÜN KALDI"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Product Avatar Icon & Name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // Emoji Avatar Box
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(2.dp, if (targetIsDolap) Color(0xFF0891B2) else Color(0xFFEA580C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getProductCategoryEmoji(targetProduct),
                                fontSize = 36.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = targetProduct.getDisplayName(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Category & Code Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = targetProduct.kategori,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (targetProduct.barkod.isNotBlank()) {
                                Text("•", color = Color(0xFF64748B))
                                Text(
                                    text = targetProduct.barkod,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Stock & SKT Metrics Dual Panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stock Info
                        Column {
                            Text("MEVCUT STOK", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(
                                text = "${targetProduct.stokAdedi} ADET",
                                color = if (targetProduct.stokAdedi > 0) TurquoisePrimary else ExpiredRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        // SKT Date Info
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SKT TARİHİ", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
                            Text(
                                text = if (targetProduct.sktTarihi > 0L) sdf.format(Date(targetProduct.sktTarihi)) else "Girilmedi",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // BOTTOM: 3 LARGE ERGONOMIC ACTION BUTTONS
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. SATILDI (GREEN BUTTON)
                Button(
                    onClick = onOpenSold,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("action_sold_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NormalGreen),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SATILDI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        }
                        Text("Stoktan Düş", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                // 2. FİREYE ATILDI (RED BUTTON)
                Button(
                    onClick = onOpenFire,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("action_fire_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FİREYE AT", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        }
                        Text("Fireye Ayır", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            // 3. NÖTR / DEĞİŞİKLİK YOK (SLATE BUTTON)
            OutlinedButton(
                onClick = onRecordNotr,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("action_notr_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("NÖTR (Rafta Duruyor / Değişiklik Yok)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREEN 3: EMPTY STATE SCREEN
// -------------------------------------------------------------
@Composable
private fun TourEmptyState(onResetTour: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NormalGreen,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "KONTROL EDİLECEK ÜRÜN YOK!",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sistemde SKT tarihi 20 gün ve altında olan kritik veya riskli ürün bulunmuyor. Reyonlarınız güvende!",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onResetTour,
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("ANA SAYFAYA DÖN", fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREEN 4: TOUR SUMMARY & RESULT REPORT
// -------------------------------------------------------------
@Composable
private fun TourSummaryView(
    report: TurRaporu?,
    logs: List<TurKontrolKaydi>,
    targetCategory: String,
    onNewTourClick: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val currentPreview = previewBitmap
    if (currentPreview != null) {
        ReportPreviewDialog(
            bitmap = currentPreview,
            onDismiss = { previewBitmap = null }
        )
    }

    val satilanLogs = logs.filter { it.durum == "SATILDI" }
    val fireLogs = logs.filter { it.durum == "FIRE" }
    val notrLogs = logs.filter { it.durum == "NOTR" }

    val satilanAdet = satilanLogs.sumOf { it.islemAdedi }
    val fireAdet = fireLogs.sumOf { it.islemAdedi }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tour_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Trophy & Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD97706).copy(alpha = 0.2f))
                        .border(2.dp, Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (report?.tamamlandiMi == false) "⚠️ KONTROL TURU TAMAMLANDI" else "🎉 KONTROL TURU TAMAMLANDI!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Toplam ${logs.size} ürün başarıyla denetlendi.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Score & Duration Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KAZANILAN PUAN", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${report?.toplamPuan ?: 0} P", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFF334155)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TUR SÜRESİ", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val saniye = report?.turSuresiSaniye ?: 0L
                        val mins = saniye / 60
                        val secs = saniye % 60
                        Text(String.format(Locale.US, "%02d:%02d", mins, secs), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFF334155)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PERSONEL", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(report?.personelAdi?.take(10) ?: "Görevli", color = Color(0xFF5EEAD4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // 3-Card Summary Breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Satıldı
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF065F46).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SATILDI", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("${satilanLogs.size} Çeşit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$satilanAdet Adet", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }

                    // Fire
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF991B1B).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("FİRE", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("${fireLogs.size} Çeşit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$fireAdet Adet", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }

                    // Nötr
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("NÖTR", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("${notrLogs.size} Çeşit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Değişmedi", color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Detailed Logs Section Header
            item {
                Text(
                    text = "İŞLEM GÖREN ÜRÜN LİSTESİ (${logs.size})",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Log Items
            items(logs) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.urunAdiSnapshot,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (log.barkodSnapshot.isNotBlank()) {
                            Text(
                                text = "Barkod: ${log.barkodSnapshot}",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (log.durum) {
                            "SATILDI" -> NormalGreen
                            "FIRE" -> ExpiredRed
                            else -> Color(0xFF334155)
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
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Share & Navigation Buttons
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (report != null) {
                        Button(
                            onClick = {
                                previewBitmap = ProductImageGenerator.createTourReportBitmap(
                                    rapor = report,
                                    logs = logs
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_tour_whatsapp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp Paylaş", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onNewTourClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reset_tour_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YENİ TUR", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER: PRODUCT CATEGORY EMOJI
// -------------------------------------------------------------
private fun getProductCategoryEmoji(product: Product): String {
    val name = product.urunAdi.lowercase(Locale.forLanguageTag("tr-TR"))
    val cat = product.kategori.lowercase(Locale.forLanguageTag("tr-TR"))
    return when {
        name.contains("süt") || name.contains("sut") || name.contains("ayran") || name.contains("kefir") -> "🥛"
        name.contains("peynir") || name.contains("kaşar") || name.contains("kasar") || name.contains("lor") -> "🧀"
        name.contains("yoğurt") || name.contains("yogurt") || name.contains("kaymak") -> "🥣"
        name.contains("et") || name.contains("kıyma") || name.contains("kiyma") || name.contains("sucuk") || name.contains("sosis") || name.contains("salam") -> "🥩"
        name.contains("tavuk") || name.contains("piliç") || name.contains("pilic") || name.contains("hindi") -> "🍗"
        name.contains("yumurta") -> "🥚"
        name.contains("ekmek") || name.contains("lavaş") || name.contains("lavas") || name.contains("un") || name.contains("pasta") || name.contains("kek") -> "🍞"
        name.contains("sebze") || name.contains("meyve") || name.contains("domates") || name.contains("salata") || name.contains("yeşillik") -> "🥬"
        name.contains("su") || name.contains("kola") || name.contains("içecek") || name.contains("icecek") || name.contains("meyve suyu") || name.contains("gazoz") -> "🥤"
        name.contains("çikolata") || name.contains("cikolata") || name.contains("bisküvi") || name.contains("biskuvi") || name.contains("gofret") -> "🍫"
        name.contains("dondurma") -> "🍦"
        cat.contains("dolap") || cat.contains("soğuk") || cat.contains("soguk") -> "❄️"
        else -> "📦"
    }
}
