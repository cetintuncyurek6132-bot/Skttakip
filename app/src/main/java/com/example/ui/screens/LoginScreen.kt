package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.auth.UserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Teal / Turkuaz Kurumsal Renk Paleti
private val TealPrimary = Color(0xFF00897B)       // Canlı Kurumsal Teal
private val TealDark = Color(0xFF00695C)          // Koyu Teal
private val TealLight = Color(0xFFE0F2F1)         // Çok Açık Teal Arka Plan
private val TealBorder = Color(0xFF80CBC4)        // İnce Teal Çerçeve
private val SurfaceBg = Color(0xFFF8FAFC)         // Soft Açık Arka Plan
private val SlateTextDark = Color(0xFF0F172A)     // Yüksek Kontrast Başlık
private val SlateTextSubtle = Color(0xFF64748B)   // İkincil Metin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val usersList by UserManager.usersList.collectAsState()

    // Varsayılan olarak listedeki ilk kullanıcı seçili gelir (Çetin)
    var selectedUser by remember(usersList) { mutableStateOf(usersList.firstOrNull()) }
    var passwordInput by remember { mutableStateOf("3232") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Dialog Durumları
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun performLogin() {
        focusManager.clearFocus()
        val user = selectedUser
        if (user == null) {
            errorMessage = "Lütfen giriş yapacak görevli ekip üyesini seçin."
            return
        }
        if (passwordInput.isBlank()) {
            errorMessage = "Lütfen şifrenizi girin."
            return
        }

        errorMessage = null
        isAuthenticating = true

        coroutineScope.launch {
            delay(200) // Hafif akıcı geçiş hissi
            val success = UserManager.login(user.username, passwordInput)
            isAuthenticating = false
            if (success) {
                val loggedUser = UserManager.currentUser.value
                if (loggedUser != null) {
                    Toast.makeText(
                        context,
                        "Hoş geldiniz, ${loggedUser.fullName}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLoginSuccess(loggedUser)
                }
            } else {
                errorMessage = "Girdiğiniz şifre hatalı! Lütfen kontrol edin."
            }
        }
    }

    // Temiz, modern, teal ağırlıklı sade arka plan (hafif dikey gradient)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE6F4F1), // Yumuşak teal ışıltısı
                        SurfaceBg,         // Sade açık zemin
                        Color.White        // Beyaz taban
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 1. ÜST BÖLÜM: KÜÇÜK LOGO VE BAŞLIK
            // ==========================================
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = TealPrimary.copy(alpha = 0.3f),
                        ambientColor = TealPrimary.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(TealPrimary, TealDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "SKT Takip Logo",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SKT TAKİP",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SlateTextDark,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "STOK • SKT • KONTROL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                letterSpacing = 1.6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // 2. GİRİŞ FORMU KARTI (GÖREVLİ GİRİŞİ)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // BAŞLIK: "GÖREVLİ GİRİŞİ"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GÖREVLİ GİRİŞİ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateTextDark,
                            letterSpacing = 0.8.sp
                        )

                        // Güvenli Giriş Rozeti
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TealLight,
                            border = BorderStroke(0.8.dp, TealBorder.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Mağaza Modu",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark
                                )
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // GÖREVLİ EKİP ÜYESİ SEÇİMİ (MODERN DROPDOWN)
                    // ----------------------------------------------------
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Görevli Ekip Üyesi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateTextSubtle
                        )

                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isDropdownExpanded) TealPrimary else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                                    .testTag("dropdown_selected_user")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (selectedUser != null) {
                                        val currentUser = selectedUser!!
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Avatar Rozeti
                                            RoleBadge(role = currentUser.role, size = 34)

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = currentUser.fullName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SlateTextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = currentUser.roleTitle,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SlateTextSubtle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = SlateTextSubtle,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Ekip üyesi seçin...",
                                                fontSize = 14.sp,
                                                color = SlateTextSubtle
                                            )
                                        }
                                    }

                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                                }
                            }

                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .widthIn(min = 280.dp)
                            ) {
                                usersList.forEach { user ->
                                    val isSelected = selectedUser?.username.equals(user.username, ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RoleBadge(role = user.role, size = 32)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = user.fullName,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) TealPrimary else SlateTextDark
                                                    )
                                                    Text(
                                                        text = user.roleTitle,
                                                        fontSize = 11.sp,
                                                        color = SlateTextSubtle
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Seçili",
                                                        tint = TealPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedUser = user
                                            passwordInput = user.password
                                            errorMessage = null
                                            isDropdownExpanded = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = SlateTextDark
                                        ),
                                        modifier = Modifier.testTag("dropdown_item_${user.username}")
                                    )
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // ŞİFRE GİRİŞ ALANI (MODERN & NET)
                    // ----------------------------------------------------
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Görevli Şifresi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateTextSubtle
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "Şifrenizi girin",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster",
                                        tint = SlateTextSubtle,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { performLogin() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SlateTextDark,
                                unfocusedTextColor = SlateTextDark,
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    // HATA MESAJI BİLDİRİMİ
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // BÜYÜK, TEAL RENKLİ "GİRİŞ YAP" BUTONU
                    // ----------------------------------------------------
                    Button(
                        onClick = { performLogin() },
                        enabled = !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = TealPrimary.copy(alpha = 0.4f)
                            )
                            .testTag("login_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = TealPrimary.copy(alpha = 0.6f),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isAuthenticating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "GİRİŞ YAPILIYOR...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "GİRİŞ YAP",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Giriş Yap",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // GİRİŞ BUTONU ALTINDA "ŞİFREMİ UNUTTUM" LİNKİ
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Şifremi Unuttum",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showForgotPasswordDialog = true }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 3. EN ALTTA: İKİNCİL "+ YENİ EKİP ÜYESİ" BUTONU
            // ==========================================
            OutlinedButton(
                onClick = { showRegisterDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_register_new_user"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TealBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Yeni Ekip Üyesi Ekle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ==========================================
    // ŞİFREMİ UNUTTUM DİYALOĞU
    // ==========================================
    if (showForgotPasswordDialog) {
        var resetUsername by remember { mutableStateOf(selectedUser?.username ?: "") }
        var newPasswordInput by remember { mutableStateOf("") }
        var resetStatusMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Şifre Sıfırlama",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SlateTextDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Şifresini yenilemek istediğiniz personelin kullanıcı adını ve yeni şifreyi giriniz:",
                        fontSize = 13.sp,
                        color = SlateTextSubtle
                    )

                    OutlinedTextField(
                        value = resetUsername,
                        onValueChange = { resetUsername = it },
                        label = { Text("Kullanıcı Adı") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Yeni Şifre") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    resetStatusMsg?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.contains("başarıyla")) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetUsername.isBlank() || newPasswordInput.isBlank()) {
                            resetStatusMsg = "Lütfen kullanıcı adı ve yeni şifreyi doldurun."
                            return@Button
                        }
                        val success = UserManager.resetPasswordForUser(resetUsername, newPasswordInput)
                        if (success) {
                            Toast.makeText(context, "Şifre başarıyla güncellendi!", Toast.LENGTH_LONG).show()
                            passwordInput = newPasswordInput
                            showForgotPasswordDialog = false
                        } else {
                            resetStatusMsg = "Kullanıcı bulunamadı! Lütfen ismi kontrol edin."
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Şifreyi Güncelle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("İptal", color = SlateTextSubtle, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ==========================================
    // YENİ EKİP ÜYESİ KAYIT DİYALOĞU
    // ==========================================
    if (showRegisterDialog) {
        var regFullName by remember { mutableStateOf("") }
        var regUsername by remember { mutableStateOf("") }
        var regPassword by remember { mutableStateOf("") }
        var regRole by remember { mutableStateOf("P1") }
        var regError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Yeni Ekip Üyesi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SlateTextDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mağazaya yeni personel eklemek için bilgileri doldurunuz:",
                        fontSize = 12.sp,
                        color = SlateTextSubtle
                    )

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        label = { Text("Kullanıcı Adı") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Şifre") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Görevi / Rolü:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("P1", "P2", "MSY", "MS").forEach { role ->
                            FilterChip(
                                selected = (regRole == role),
                                onClick = { regRole = role },
                                label = { Text(role, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    regError?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (regFullName.isBlank() || regUsername.isBlank() || regPassword.isBlank()) {
                            regError = "Tüm alanları doldurmanız gerekmektedir."
                            return@Button
                        }
                        val roleTitle = when (regRole) {
                            "MS" -> "Mağaza Sorumlusu (MS)"
                            "MSY" -> "Mağaza Sorumlu Yardımcısı (MSY)"
                            "P2" -> "Personel 2 (P2)"
                            else -> "Personel 1 (P1)"
                        }
                        val newUser = UserAccount(
                            username = regUsername.trim(),
                            password = regPassword.trim(),
                            role = regRole,
                            roleTitle = roleTitle,
                            fullName = regFullName.trim()
                        )
                        val added = UserManager.addUser(newUser)
                        if (added) {
                            Toast.makeText(context, "Yeni ekip üyesi başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                            selectedUser = newUser
                            passwordInput = newUser.password
                            showRegisterDialog = false
                        } else {
                            regError = "Bu kullanıcı adı zaten mevcut!"
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Kaydet & Seç", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("İptal", color = SlateTextSubtle, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun RoleBadge(role: String, size: Int = 32) {
    val gradientColors = when (role) {
        "MS" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626)) // Canlı Kırmızı
        "MSY" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Amber / Turuncu
        "P1" -> listOf(TealPrimary, TealDark)                 // Teal / Turkuaz
        else -> listOf(Color(0xFF0284C7), Color(0xFF0369A1))  // Mavi
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = role,
            color = Color.White,
            fontSize = if (size > 32) 11.sp else 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp
        )
    }
}
