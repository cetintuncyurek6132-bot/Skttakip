package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdetselKayit
import com.example.ui.screens.adetsel.AdetselFilterChip
import com.example.ui.screens.adetsel.AdetselSayimDialog
import com.example.ui.screens.adetsel.CompactYapilacakCard
import com.example.ui.screens.adetsel.CompactYapildiCard
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlue
import com.example.ui.theme.WarningBlueBorder
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark

enum class AdetselTab(val label: String) {
    YAPILACAK("Adetsel Yapılacak"),
    YAPILDI("Adetsel Yapıldı")
}

@Composable
fun AdetselScreen(
    yapilacakList: List<AdetselKayit>,
    yapildiList: List<AdetselKayit>,
    onSaveSayim: (kayit: AdetselKayit, sonuc: String, fark: Int, notlar: String) -> Unit,
    onUndoSayim: (AdetselKayit) -> Unit,
    onDeleteKayit: (Int) -> Unit,
    onClearCompleted: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onBackClick: () -> Unit = onNavigateToProducts
) {
    var selectedTab by remember { mutableStateOf(AdetselTab.YAPILACAK) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedResultFilter by remember { mutableStateOf("ALL") } // ALL, EKSIK, FAZLA, TAM

    var countingKayit by remember { mutableStateOf<AdetselKayit?>(null) }
    var initialModeForDialog by remember { mutableStateOf("TAM") } // "TAM", "EKSIK", "FAZLA"
    var itemToDelete by remember { mutableStateOf<AdetselKayit?>(null) }
    var showClearCompletedConfirm by remember { mutableStateOf(false) }

    val distinctYapilacakList = remember(yapilacakList) {
        yapilacakList.distinctBy { item ->
            when {
                item.barkod.isNotBlank() -> "B:${item.barkod.trim()}"
                item.urunKodu.isNotBlank() -> "K:${item.urunKodu.trim()}"
                item.productId > 0 -> "P:${item.productId}"
                else -> "N:${item.urunAdi.trim().lowercase()}"
            }
        }
    }

    val filteredYapilacak = remember(distinctYapilacakList, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) distinctYapilacakList else {
            distinctYapilacakList.filter {
                it.urunAdi.lowercase().contains(q) ||
                it.urunKodu.lowercase().contains(q) ||
                it.barkod.lowercase().contains(q)
            }
        }
    }

    val filteredYapildi = remember(yapildiList, searchQuery, selectedResultFilter) {
        val q = searchQuery.trim().lowercase()
        yapildiList.filter { item ->
            val matchQuery = q.isEmpty() ||
                item.urunAdi.lowercase().contains(q) ||
                item.urunKodu.lowercase().contains(q) ||
                item.barkod.lowercase().contains(q)

            val matchFilter = when (selectedResultFilter) {
                "EKSIK" -> item.sayimSonucu == "EKSIK"
                "FAZLA" -> item.sayimSonucu == "FAZLA"
                "TAM" -> item.sayimSonucu == "TAM"
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    // SAYIM DIALOGU
    countingKayit?.let { kayit ->
        AdetselSayimDialog(
            kayit = kayit,
            initialMode = initialModeForDialog,
            onDismiss = { countingKayit = null },
            onSave = { sonuc, fark, notlar ->
                onSaveSayim(kayit, sonuc, fark, notlar)
                countingKayit = null
            }
        )
    }

    // SİLME ONAY DIALOGU
    itemToDelete?.let { kayit ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpiredRed)
            },
            title = {
                Text(text = "Adetsel Kaydını Sil", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(
                    text = "\"${kayit.urunAdi}\" kaydını listeden silmek istediğinize emin misiniz?",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteKayit(kayit.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Vazgeç", color = Slate700)
                }
            }
        )
    }

    // TAMAMLANANLARI TEMİZLE ONAY DIALOGU
    if (showClearCompletedConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCompletedConfirm = false },
            icon = {
                Icon(imageVector = Icons.Default.ClearAll, contentDescription = null, tint = ExpiredRed)
            },
            title = {
                Text(text = "Yapılan Sayımları Temizle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(
                    text = "Tamamlanmış tüm adetsel sayım geçmişi silinecektir.",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCompleted()
                        showClearCompletedConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Temizle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedConfirm = false }) {
                    Text("Vazgeç", color = Slate700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER - KOMPAKT VE ŞIK
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adetsel Sayım",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }

                    if (selectedTab == AdetselTab.YAPILDI && yapildiList.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearCompletedConfirm = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpiredRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Temizle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // TAB ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TAB 1: ADETSEL YAPILACAK
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = AdetselTab.YAPILACAK },
                        color = if (selectedTab == AdetselTab.YAPILACAK) TurquoiseDark else Slate100,
                        shape = RoundedCornerShape(8.dp),
                        border = if (selectedTab == AdetselTab.YAPILACAK) null else BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YAPILACAK",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == AdetselTab.YAPILACAK) Color.White else Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTab == AdetselTab.YAPILACAK) Color.White.copy(alpha = 0.25f) else TurquoisePrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${distinctYapilacakList.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedTab == AdetselTab.YAPILACAK) Color.White else TurquoiseDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // TAB 2: ADETSEL YAPILDI
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = AdetselTab.YAPILDI },
                        color = if (selectedTab == AdetselTab.YAPILDI) TurquoiseDark else Slate100,
                        shape = RoundedCornerShape(8.dp),
                        border = if (selectedTab == AdetselTab.YAPILDI) null else BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YAPILDI",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == AdetselTab.YAPILDI) Color.White else Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTab == AdetselTab.YAPILDI) Color.White.copy(alpha = 0.25f) else NormalGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${yapildiList.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedTab == AdetselTab.YAPILDI) Color.White else NormalGreenDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SEARCH & FILTER BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // High Visibility Search Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Ürün adı veya barkod ara...",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(TurquoiseDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adetsel_search_input")
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Temizle",
                                tint = Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // YAPILDI TAB FILTER PILLS (TÜMÜ, EKSİK, FAZLA, TAM)
            if (selectedTab == AdetselTab.YAPILDI && yapildiList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val eksikCount = remember(yapildiList) { yapildiList.count { it.sayimSonucu == "EKSIK" } }
                    val fazlaCount = remember(yapildiList) { yapildiList.count { it.sayimSonucu == "FAZLA" } }
                    val tamCount = remember(yapildiList) { yapildiList.count { it.sayimSonucu == "TAM" } }

                    AdetselFilterChip(
                        label = "Tümü (${yapildiList.size})",
                        isSelected = selectedResultFilter == "ALL",
                        onClick = { selectedResultFilter = "ALL" },
                        color = TurquoiseDark
                    )
                    AdetselFilterChip(
                        label = "Eksik ($eksikCount)",
                        isSelected = selectedResultFilter == "EKSIK",
                        onClick = { selectedResultFilter = "EKSIK" },
                        color = ExpiredRedDark
                    )
                    AdetselFilterChip(
                        label = "Fazla ($fazlaCount)",
                        isSelected = selectedResultFilter == "FAZLA",
                        onClick = { selectedResultFilter = "FAZLA" },
                        color = WarningBlueDark
                    )
                    AdetselFilterChip(
                        label = "Tam ($tamCount)",
                        isSelected = selectedResultFilter == "TAM",
                        onClick = { selectedResultFilter = "TAM" },
                        color = NormalGreenDark
                    )
                }
            }
        }

        // CONTENT BODY
        when (selectedTab) {
            AdetselTab.YAPILACAK -> {
                if (filteredYapilacak.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Ürün bulunamadı" else "Sayım Listesi Boş",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Arama terimini kontrol edin." else "Ürün detayından 'Sayıma Ekle' butonuna dokunarak listeye ürün aktarabilirsiniz.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            if (searchQuery.isEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onNavigateToProducts,
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ürünler Sayfasına Git", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                    ) {
                        items(filteredYapilacak, key = { it.id }) { kayit ->
                            CompactYapilacakCard(
                                kayit = kayit,
                                onClick = {
                                    initialModeForDialog = "TAM"
                                    countingKayit = kayit
                                },
                                onDelete = { itemToDelete = kayit }
                            )
                        }
                    }
                }
            }

            AdetselTab.YAPILDI -> {
                if (filteredYapildi.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NormalGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NormalGreenDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedResultFilter != "ALL") "Filtreye uygun kayıt yok" else "Henüz Sayılan Ürün Yok",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tamamlanan adetsel sayımlar burada listelenir.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                    ) {
                        items(filteredYapildi, key = { it.id }) { kayit ->
                            CompactYapildiCard(
                                kayit = kayit,
                                onUndo = { onUndoSayim(kayit) },
                                onDelete = { itemToDelete = kayit }
                            )
                        }
                    }
                }
            }
        }
    }
}
