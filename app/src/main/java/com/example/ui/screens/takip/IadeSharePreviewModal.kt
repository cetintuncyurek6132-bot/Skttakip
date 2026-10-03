package com.example.ui.screens.takip

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DepoIadeKaydi
import com.example.data.TakipStats
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.util.image.BitmapSharingHelper
import com.example.util.image.IadeImageGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IadeSharePreviewModal(
    records: List<DepoIadeKaydi>,
    stats: TakipStats? = null,
    formattedTextMessage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Görsel Formatı, 1: Metin Formatı
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingImage by remember { mutableStateOf(true) }

    LaunchedEffect(records) {
        isGeneratingImage = true
        generatedBitmap = withContext(Dispatchers.Default) {
            IadeImageGenerator.createIadeListBitmap(records, stats)
        }
        isGeneratingImage = false
    }

    fun shareImage() {
        val bitmap = generatedBitmap
        if (bitmap != null) {
            BitmapSharingHelper.shareBitmap(context, bitmap, "iade_takip_raporu")
            onDismiss()
        } else {
            Toast.makeText(context, "Görsel hazırlanıyor, lütfen bekleyin.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, formattedTextMessage)
            `package` = "com.whatsapp"
        }
        try {
            context.startActivity(sendIntent)
            onDismiss()
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formattedTextMessage)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Takip Raporunu Paylaş"))
                onDismiss()
            } catch (ex: Exception) {
                Toast.makeText(context, "Paylaşım uygulaması açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // BAŞLIK BARI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "WhatsApp Paylaşım Önizleme",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${records.size} kayıt paylaşılacak",
                                fontSize = 11.5.sp,
                                color = Slate500
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // FORMAT SEÇİM SEKMELERİ (TabRow)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = TurquoiseDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Görsel Formatı", fontSize = 12.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Metin Formatı", fontSize = 12.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ÖNİZLEME ALANI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == 0) {
                        // Görsel Formatı Önizleme
                        if (isGeneratingImage) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = TurquoisePrimary, modifier = Modifier.size(28.dp))
                                Text("Görsel tablo hazırlanıyor...", fontSize = 12.sp, color = Slate500)
                            }
                        } else if (generatedBitmap != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = generatedBitmap!!.asImageBitmap(),
                                    contentDescription = "İade Listesi Görseli",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        } else {
                            Text("Görsel oluşturulamadı.", fontSize = 12.sp, color = Slate500)
                        }
                    } else {
                        // Metin Formatı Önizleme
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = formattedTextMessage,
                                fontSize = 12.sp,
                                color = Slate700,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ALT AKSİYON BUTONLARI (2 Seçenek)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. "Metin Olarak Paylaş"
                    OutlinedButton(
                        onClick = { shareText() },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Metin Paylaş",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    }

                    // 2. "Görsel Olarak Paylaş"
                    Button(
                        onClick = { shareImage() },
                        enabled = !isGeneratingImage && generatedBitmap != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Görsel Olarak Paylaş",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
