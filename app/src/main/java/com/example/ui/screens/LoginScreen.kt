package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.auth.UserManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val usersList by UserManager.usersList.collectAsState()

    var selectedUser by remember { mutableStateOf<UserAccount?>(usersList.firstOrNull()) }
    var usernameInput by remember { mutableStateOf(usersList.firstOrNull()?.username ?: "Cetin") }
    var passwordInput by remember { mutableStateOf("3232") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Dialog States
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // SADE BEYAZ & MAVİ ARKA PLAN
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE), // Açık Mavi
                        Color(0xFFF0F9FF), // Çok Açık Mavi
                        Color(0xFFFFFFFF)  // Beyaz
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // LOGO & BAŞLIK (MAVİ - BEYAZ TEMA, YÜKSEK KONTRAST)
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(2.5.dp, Color(0xFF0284C7)),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "A101 SKT Takip",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = "A101 SKT TAKİP & STOK",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A), // Koyu Lacivert / Siyah - Yüksek Kontrast
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // EKİP ÜYESİ SEÇİM ALANI
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👤 Görevli Ekip Üyesi Seçin:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )

                    if (selectedUser != null) {
                        Surface(
                            onClick = {
                                selectedUser = null
                                usernameInput = ""
                                passwordInput = ""
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Manuel Yaz",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }

                val chunked = usersList.chunked(2)
                chunked.forEach { rowUsers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowUsers.forEach { user ->
                            val isSelected = selectedUser?.username.equals(user.username, ignoreCase = true) ||
                                    (selectedUser == null && usernameInput.equals(user.username, ignoreCase = true))

                            Surface(
                                onClick = {
                                    selectedUser = user
                                    usernameInput = user.username
                                    passwordInput = user.password
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFFE0F2FE) else Color.White,
                                border = BorderStroke(
                                    if (isSelected) 2.5.dp else 1.2.dp,
                                    if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("user_chip_${user.username}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (user.role) {
                                                    "MS" -> Color(0xFFDC2626) // Canlı Kırmızı
                                                    "MSY" -> Color(0xFFD97706) // Turuncu
                                                    else -> Color(0xFF0284C7) // Mavi
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.role,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = user.username,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = user.fullName.split(" ").firstOrNull() ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GİRİŞ FORM KARTI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GİRİŞ BİLGİLERİ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0284C7),
                            letterSpacing = 1.sp
                        )

                        if (selectedUser != null) {
                            Surface(
                                color = Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kullanıcı Adı Kilitli",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                            }
                        }
                    }

                    // KULLANICI ADI - EKİP ÜYESİ SEÇİLİNCE DEĞİŞTİRİLEMEZ (READONLY)
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = {
                            if (selectedUser == null) {
                                usernameInput = it
                                errorMessage = null
                            }
                        },
                        readOnly = (selectedUser != null), // Ekip üyesi seçilince değiştirmeye izin verilmez
                        label = {
                            Text(
                                text = if (selectedUser != null) "Kullanıcı Adı (Seçili Ekip Üyesi)" else "Kullanıcı Adı",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF0284C7)
                            )
                        },
                        trailingIcon = {
                            if (selectedUser != null) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Kilitli",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            disabledTextColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFF94A3B8),
                            focusedContainerColor = if (selectedUser != null) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                            unfocusedContainerColor = if (selectedUser != null) Color(0xFFF1F5F9) else Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // ŞİFRE INPUT (SADECE ŞİFRE YAZILSIN)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = {
                            Text(
                                text = "Şifrenizi Girin",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF0284C7)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Şifreyi Göster",
                                    tint = Color(0xFF475569)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFF94A3B8),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // GİRİŞ YAP BUTONU
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            errorMessage = null
                            isAuthenticating = true
                            coroutineScope.launch {
                                delay(300)
                                val success = UserManager.login(usernameInput, passwordInput)
                                isAuthenticating = false
                                if (success) {
                                    val user = UserManager.currentUser.value
                                    if (user != null) {
                                        Toast.makeText(
                                            context,
                                            "Hoş geldiniz, ${user.fullName} (${user.role})!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onLoginSuccess(user)
                                    }
                                } else {
                                    errorMessage = "Kullanıcı adı veya şifre hatalı! Lütfen kontrol edin."
                                }
                            }
                        },
                        enabled = !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isAuthenticating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "GİRİŞ YAPILIYOR...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GİRİŞ YAP",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ŞİFREMİ UNUTTUM & KAYIT OL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Şifremi Unuttum",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { showForgotPasswordDialog = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )

                        Text(
                            text = "+ Yeni Ekip Kaydı (Kayıt Ol)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { showRegisterDialog = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- ŞİFREMİ UNUTTUM DIALOG ---
    if (showForgotPasswordDialog) {
        var resetUsername by remember { mutableStateOf(usernameInput) }
        var newPasswordInput by remember { mutableStateOf("") }
        var resetStatusMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Şifre Sıfırlama",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ekip üyenizin kullanıcı adını ve yeni belirlemek istediğiniz şifreyi giriniz:",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )

                    OutlinedTextField(
                        value = resetUsername,
                        onValueChange = { resetUsername = it },
                        label = { Text("Kullanıcı Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Yeni Şifre") },
                        singleLine = true,
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Şifreyi Güncelle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("İptal", color = Color(0xFF64748B))
                }
            }
        )
    }

    // --- YENİ EKİP ÜYESİ KAYIT DIALOG ---
    if (showRegisterDialog) {
        var regFullName by remember { mutableStateOf("") }
        var regUsername by remember { mutableStateOf("") }
        var regPassword by remember { mutableStateOf("") }
        var regRole by remember { mutableStateOf("P1") }
        var regError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yeni Ekip Üyesi Kaydı",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mağaza personeli veya yöneticisi eklemek için bilgileri giriniz:",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        label = { Text("Kullanıcı Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Şifre") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Görevi / Rolü:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
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
                            Toast.makeText(context, "Yeni personel kaydı başarılı!", Toast.LENGTH_SHORT).show()
                            selectedUser = newUser
                            usernameInput = newUser.username
                            passwordInput = newUser.password
                            showRegisterDialog = false
                        } else {
                            regError = "Bu kullanıcı adı zaten mevcut!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Kaydet & Kullan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("İptal", color = Color(0xFF64748B))
                }
            }
        )
    }
}
