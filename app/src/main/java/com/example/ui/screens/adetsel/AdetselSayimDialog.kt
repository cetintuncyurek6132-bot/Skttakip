package com.example.ui.screens.adetsel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
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
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlueBorder
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark

/**
 * ADETSEL SAYIM DIALOG
 * Direct difference entry: Tam (0) / Eksik (-X) / Fazla (+X)
 */
@Composable
fun AdetselSayimDialog(
    kayit: AdetselKayit,
    initialMode: String = "TAM",
    onDismiss: () -> Unit,
    onSave: (sonuc: String, fark: Int, notlar: String) -> Unit
) {
    var selectedSonuc by remember { mutableStateOf(initialMode) } // "TAM", "EKSIK", "FAZLA"
    var discrepancyText by remember { mutableStateOf(if (initialMode == "TAM") "0" else "1") }
    var notlar by remember { mutableStateOf("") }

    val discrepancyNumber = remember(discrepancyText) {
        discrepancyText.trim().toIntOrNull() ?: 0
    }

    val calculatedFark = remember(selectedSonuc, discrepancyNumber) {
        when (selectedSonuc) {
            "TAM" -> 0
            "EKSIK" -> -discrepancyNumber
            "FAZLA" -> discrepancyNumber
            else -> 0
        }
    }

    fun adjustDiscrepancy(delta: Int) {
        val current = discrepancyNumber
        val next = maxOf(1, current + delta)
        discrepancyText = next.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adetsel Sayım",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TurquoiseDark
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Product Info Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = kayit.urunAdi.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kod: ${kayit.getDisplayCode()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DURUM SEÇİMİ (TAM / EKSİK / FAZLA)
                Text(
                    text = "DURUM SEÇİN",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. TAM (FARK: 0)
                    StatusSelectionPill(
                        label = "TAM (0)",
                        subLabel = "Fark Yok",
                        isSelected = selectedSonuc == "TAM",
                        activeBg = NormalGreenDark,
                        onClick = {
                            selectedSonuc = "TAM"
                            discrepancyText = "0"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. EKSİK
                    StatusSelectionPill(
                        label = "EKSİK (-)",
                        subLabel = "Eksik Var",
                        isSelected = selectedSonuc == "EKSIK",
                        activeBg = ExpiredRedDark,
                        onClick = {
                            selectedSonuc = "EKSIK"
                            if (discrepancyNumber == 0) discrepancyText = "1"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 3. FAZLA
                    StatusSelectionPill(
                        label = "FAZLA (+)",
                        subLabel = "Fazla Var",
                        isSelected = selectedSonuc == "FAZLA",
                        activeBg = WarningBlueDark,
                        onClick = {
                            selectedSonuc = "FAZLA"
                            if (discrepancyNumber == 0) discrepancyText = "1"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DİNAMİK FARK GİRİŞİ ALANI
                when (selectedSonuc) {
                    "TAM" -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NormalGreenContainer,
                            border = BorderStroke(1.dp, NormalGreenBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NormalGreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Tam (0 Fark)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = NormalGreenDark
                                    )
                                    Text(
                                        text = "Stokta eksik veya fazla adet bulunmuyor.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }

                    "EKSIK" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "KAÇ ADET EKSİK?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = ExpiredRedDark,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Minus Button (-1)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Slate100,
                                    border = BorderStroke(1.2.dp, Slate300),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { adjustDiscrepancy(-1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Azalt",
                                            tint = Slate700,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Sayı Giriş Alanı - Geniş, ferah ve tam görünür
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ExpiredRedContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.5.dp, ExpiredRedBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = discrepancyText,
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }
                                                discrepancyText = digits
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = TextStyle(
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                textAlign = TextAlign.Center,
                                                color = ExpiredRedDark
                                            ),
                                            cursorBrush = SolidColor(ExpiredRedDark),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (discrepancyText.isEmpty()) {
                                            Text(
                                                text = "0",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ExpiredRedDark.copy(alpha = 0.4f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Plus Button (+1)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ExpiredRedDark,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { adjustDiscrepancy(1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Artır",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "FAZLA" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "KAÇ ADET FAZLA?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = WarningBlueDark,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Minus Button (-1)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Slate100,
                                    border = BorderStroke(1.2.dp, Slate300),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { adjustDiscrepancy(-1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Azalt",
                                            tint = Slate700,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Sayı Giriş Alanı - Geniş, ferah ve tam görünür
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WarningBlueContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.5.dp, WarningBlueBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = discrepancyText,
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }
                                                discrepancyText = digits
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = TextStyle(
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                textAlign = TextAlign.Center,
                                                color = WarningBlueDark
                                            ),
                                            cursorBrush = SolidColor(WarningBlueDark),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (discrepancyText.isEmpty()) {
                                            Text(
                                                text = "0",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarningBlueDark.copy(alpha = 0.4f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Plus Button (+1)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WarningBlueDark,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { adjustDiscrepancy(1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Artır",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Not Alanı (İsteğe Bağlı)
                OutlinedTextField(
                    value = notlar,
                    onValueChange = { notlar = it },
                    placeholder = { Text("Açıklama / Not (isteğe bağlı)...", fontSize = 11.5.sp, color = Slate500) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoiseDark,
                        unfocusedBorderColor = Slate300
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons (Vazgeç & Kaydet)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Vazgeç", color = Slate700, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            val finalFark = when (selectedSonuc) {
                                "TAM" -> 0
                                "EKSIK" -> -maxOf(1, discrepancyNumber)
                                "FAZLA" -> maxOf(1, discrepancyNumber)
                                else -> 0
                            }
                            onSave(selectedSonuc, finalFark, notlar)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedSonuc) {
                                "EKSIK" -> ExpiredRedDark
                                "FAZLA" -> WarningBlueDark
                                else -> NormalGreenDark
                            }
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (selectedSonuc) {
                                "EKSIK" -> "Eksik Kaydet"
                                "FAZLA" -> "Fazla Kaydet"
                                else -> "Tam Kaydet"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusSelectionPill(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    activeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected) activeBg else Slate100,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else Slate900
            )
            Text(
                text = subLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate500
            )
        }
    }
}
