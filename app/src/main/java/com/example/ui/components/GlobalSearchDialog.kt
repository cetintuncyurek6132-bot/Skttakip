package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.data.getDisplayCode
import com.example.data.getDisplayName
import com.example.data.matchesSearchQuery
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlue
import com.example.ui.theme.WarningBlueContainer
import kotlinx.coroutines.delay

@Composable
fun GlobalSearchDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    allProducts: List<Product>,
    onProductClick: (Product) -> Unit,
    onAddNewProductClick: () -> Unit
) {
    if (!isOpen) return

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Popup(
        onDismissRequest = {
            onDismiss()
            onSearchQueryChange("")
            focusManager.clearFocus(force = true)
        },
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable {
                    onDismiss()
                    onSearchQueryChange("")
                    focusManager.clearFocus(force = true)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(10.dp)
                    .clickable(enabled = false) {}
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Arama Giriş Alanı
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ara",
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Ürün adı, barkod veya kod ara",
                                            color = Slate500,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = Slate900,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        cursorBrush = SolidColor(TurquoisePrimary),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Characters,
                                            imeAction = ImeAction.Search
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                keyboardController?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            onSearchQueryChange("")
                                        } else {
                                            onDismiss()
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kapat",
                                        tint = Slate700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val queryTrim = searchQuery.trim()
                        val matchingProducts = remember(allProducts, queryTrim) {
                            if (queryTrim.isEmpty()) {
                                allProducts.take(50)
                            } else {
                                allProducts.filter { prod ->
                                    prod.matchesSearchQuery(queryTrim)
                                }.take(50)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (queryTrim.isEmpty()) Icons.Default.Inventory2 else Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (queryTrim.isEmpty()) "TÜM ÜRÜNLER (${allProducts.size})" else "ARAMA SONUÇLARI (${matchingProducts.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoiseDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (matchingProducts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aradığınız ürün bulunamadı. Yeni ürün olarak eklemek ister misiniz?",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onSearchQueryChange("")
                                        focusManager.clearFocus(force = true)
                                        onAddNewProductClick()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "YENİ ÜRÜN EKLE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(matchingProducts, key = { it.id }) { product ->
                                    val status = if (product.sktTarihi > 0L) product.getExpiryStatus() else ExpiryStatus.NORMAL
                                    val daysLeft = if (product.sktTarihi > 0L) product.getRemainingDays() else 9999L

                                    val (statusBg, statusText, statusLabel) = when {
                                        product.sktTarihi <= 0L -> Triple(Slate100, Slate700, "TARİHSİZ")
                                        status == ExpiryStatus.EXPIRED -> Triple(ExpiredRedContainer, ExpiredRed, "SÜRESİ GEÇTİ")
                                        status == ExpiryStatus.CRITICAL -> Triple(CriticalOrangeContainer, CriticalOrange, if (daysLeft == 0L) "BUGÜN" else "$daysLeft GÜN")
                                        status == ExpiryStatus.SOON -> Triple(SoonYellowContainer, Color(0xFF8B6B00), "$daysLeft GÜN")
                                        status == ExpiryStatus.WARNING -> Triple(WarningBlueContainer, WarningBlue, "$daysLeft GÜN")
                                        else -> Triple(NormalGreenContainer, NormalGreen, "$daysLeft GÜN")
                                    }

                                    Surface(
                                        onClick = {
                                            onDismiss()
                                            onSearchQueryChange("")
                                            focusManager.clearFocus(force = true)
                                            onProductClick(product)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Slate50,
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val displayName = product.getDisplayName()
                                            val displayCode = product.getDisplayCode()
                                            val firstChar = displayName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"

                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(TurquoisePrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = firstChar,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = TurquoiseDark
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = displayName.uppercase(),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val infoText = buildString {
                                                    append(if (product.kategori.isNotBlank()) product.kategori else "Genel")
                                                    val barcode = product.barkod.trim()
                                                    if (barcode.isNotBlank()) {
                                                        append(" • Barkod: ")
                                                        append(barcode)
                                                    }
                                                    if (displayCode.isNotBlank() && displayCode != barcode && displayCode != displayName) {
                                                        append(" • Kod: ")
                                                        append(displayCode)
                                                    }
                                                }
                                                Text(
                                                    text = infoText,
                                                    fontSize = 11.sp,
                                                    color = Slate500,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(statusBg)
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = statusLabel,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = statusText
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "Adet: ${product.stokAdedi}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate700
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
