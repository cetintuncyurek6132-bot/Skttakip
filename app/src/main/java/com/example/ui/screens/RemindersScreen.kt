package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.TurquoisePrimary

@Composable
fun RemindersScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember {
        val sp = context.getSharedPreferences("skt_settings_prefs", Context.MODE_PRIVATE)
        val legacy = context.getSharedPreferences("a101_settings_prefs", Context.MODE_PRIVATE)
        if (sp.all.isEmpty() && legacy.all.isNotEmpty()) {
            val ed = sp.edit()
            legacy.all.forEach { (k, v) ->
                if (v is String) ed.putString(k, v)
            }
            ed.apply()
        }
        sp
    }

    val defaultText = "• Dolap derece kontrollerini saat 10:00 ve 16:00'da kaydetmeyi unutma.\n• Son günü gelen ürünlerde %50 indirim etiketini yapıştır.\n• Sayım sonrası fire tutanaklarını sisteme gir.\n• Reyon düzeni ve SKT öncelik (FIFO) kuralını kontrol et.\n• Akşam kasa kapanışında fiş rulosu ve poşet stoğunu tamamla."

    var reminderNotes by remember {
        mutableStateOf(prefs.getString("store_reminders_notes", defaultText) ?: defaultText)
    }
    var showResetDialog by remember { mutableStateOf(false) }

    // Otomatik gecikmeli kaydetme (her harfte diske yazarak arayüzü kitlemeyi önler)
    LaunchedEffect(reminderNotes) {
        kotlinx.coroutines.delay(400)
        prefs.edit().putString("store_reminders_notes", reminderNotes).apply()
    }

    val quickSnippets = listOf(
        "❄️ Dolap sıcaklıklarını çizelgeye işle",
        "🏷️ %50 İndirim etiketlerini raflara as",
        "📋 Akşam kasa mutabakatını tamamla",
        "🔄 FIFO kuralı ile reyonları öne çek",
        "📦 Günü dolan ürünlerin fire kaydını gir"
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("Varsayılana Sıfırla", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Hatırlatıcı notları varsayılan mağaza şablonuna sıfırlamak istiyor musunuz?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderNotes = defaultText
                        prefs.edit().putString("store_reminders_notes", defaultText).apply()
                        showResetDialog = false
                        Toast.makeText(context, "Hatırlatıcılar varsayılana sıfırlandı", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text("Sıfırla", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CriticalOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CriticalOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HATIRLATICILAR & NOTLAR",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "MAĞAZA UNUTULMAYACAKLAR DEFTERİ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(reminderNotes))
                                Toast.makeText(context, "Notlar panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Kopyala",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bilgi Banner'ı
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CriticalOrange.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CriticalOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mağaza Hatırlatıcı Not Defteri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Yazdığınız notlar otomatik olarak cihazınızda saklanır. Vardiya devirlerinde veya gün içi takiplerde kullanabilirsiniz.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Hızlı Şablonlar / Hazır Maddeler
            item {
                Column {
                    Text(
                        text = "HIZLI NOT EKLE (+)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickSnippets) { snippet ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        val newText = if (reminderNotes.isBlank()) "• $snippet" else "$reminderNotes\n• $snippet"
                                        reminderNotes = newText
                                        Toast.makeText(context, "Madde eklendi", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Text(
                                    text = snippet,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Ana Not Düzenleme Alanı
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝 Not İçeriği",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Button(
                                onClick = {
                                    prefs.edit().putString("store_reminders_notes", reminderNotes.trim()).apply()
                                    Toast.makeText(context, "✅ Notlar başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kaydet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = reminderNotes,
                            onValueChange = {
                                reminderNotes = it
                            },
                            placeholder = { Text("Buraya mağazada yapılması gerekenleri veya hatırlatıcıları yazın...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showResetDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Varsayılan Notlar", fontSize = 11.sp)
                            }

                            TextButton(
                                onClick = {
                                    reminderNotes = ""
                                    prefs.edit().putString("store_reminders_notes", "").apply()
                                    Toast.makeText(context, "Notlar temizlendi", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Temizle", fontSize = 12.sp, color = ExpiredRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
