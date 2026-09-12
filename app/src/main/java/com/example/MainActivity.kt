package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.ui.DashboardState
import com.example.ui.MainViewModel
import com.example.ui.ProductFilter
import com.example.ui.components.AddEditProductModal
import com.example.ui.components.AddSktModal
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.ProductDetailModal
import com.example.ui.components.SktBottomNavBar
import com.example.ui.components.SktTopAppBar
import com.example.ui.screens.AdetselScreen
import com.example.ui.screens.BarcodeScannerSheet
import com.example.ui.screens.CsvScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NotificationSheet
import com.example.ui.screens.ProfileSheet
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import com.example.worker.MorningCheckWorker
import java.util.Locale
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.TakipScreen
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun attachBaseContext(newBase: android.content.Context) {
        try {
            val trLocale = java.util.Locale.forLanguageTag("tr-TR")
            java.util.Locale.setDefault(trLocale)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(trLocale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val trLocale = java.util.Locale.forLanguageTag("tr-TR")
            java.util.Locale.setDefault(trLocale)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Locale setup fallback: ${e.message}")
        }

        val db = AppDatabase.getDatabase(applicationContext)
        try {
            com.example.auth.UserManager.initialize(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "UserManager init error", e)
        }

        try {
            com.example.sync.CloudSyncManager.initialize(applicationContext, db.productDao(), db.turDao())
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "CloudSyncManager init error", e)
        }

        val repository = ProductRepository(db.productDao(), db.reportDao(), db.turDao(), db.adetselDao())
        viewModel = ViewModelProvider(this, MainViewModel.Factory(repository))[MainViewModel::class.java]

        try {
            MorningCheckWorker.scheduleDailyMorningCheck(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Worker schedule error", e)
        }

        viewModel.runStartupDataProtection(applicationContext)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkMode) {
                SktMainApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.sync.CloudSyncManager.startRealtimeListeners()
    }

    override fun onStop() {
        super.onStop()
        com.example.sync.CloudSyncManager.stopRealtimeListeners()
    }
}

@Composable
fun SktMainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "panel"

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            MorningCheckWorker.scheduleDailyMorningCheck(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val currentUser by com.example.auth.UserManager.currentUser.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val syncState by com.example.sync.CloudSyncManager.syncState.collectAsStateWithLifecycle()
    val isSyncingOrLoading = isLoading || syncState == com.example.sync.SyncState.SYNCING

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedGroupFilter by viewModel.selectedGroupFilter.collectAsStateWithLifecycle()
    val startDateFilter by viewModel.startDateFilter.collectAsStateWithLifecycle()
    val endDateFilter by viewModel.endDateFilter.collectAsStateWithLifecycle()

    val isAddEditModalOpen by viewModel.isAddEditModalOpen.collectAsStateWithLifecycle()
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()
    val detailProduct by viewModel.detailProduct.collectAsStateWithLifecycle()
    val addSktProduct by viewModel.addSktProduct.collectAsStateWithLifecycle()
    val prefilledBarcode by viewModel.prefilledBarcode.collectAsStateWithLifecycle()

    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val migrationStatus by viewModel.migrationStatus.collectAsStateWithLifecycle()
    val yapilacakAdetsel by viewModel.yapilacakAdetselKayitlari.collectAsStateWithLifecycle()
    val yapildiAdetsel by viewModel.yapildiAdetselKayitlari.collectAsStateWithLifecycle()

    // Bilgilendirme çubuğunu 3 saniye sonra otomatik olarak kaybet
    LaunchedEffect(migrationStatus) {
        if (migrationStatus != null && migrationStatus?.isVisible == true) {
            kotlinx.coroutines.delay(3000L)
            viewModel.dismissMigrationStatus()
        }
    }

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userBranch by viewModel.userBranch.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val userDepartment by viewModel.userDepartment.collectAsStateWithLifecycle()
    val userDutyStatus by viewModel.userDutyStatus.collectAsStateWithLifecycle()
    val morningReminderEnabled by viewModel.morningCheckReminderEnabled.collectAsStateWithLifecycle()
    val criticalAlertEnabled by viewModel.criticalSktAlertEnabled.collectAsStateWithLifecycle()
    val highStockAlertEnabled by viewModel.highStockAlertEnabled.collectAsStateWithLifecycle()
    val soundEffectsEnabled by viewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val isBatterySaverMode by viewModel.isBatterySaverMode.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // Dialog & Scanner control states
    var isBarcodeScannerOpen by remember { mutableStateOf(false) }
    var startScannerInFixMode by remember { mutableStateOf(false) }
    var isNotificationDialogOpen by remember { mutableStateOf(false) }
    var isProfileDialogOpen by remember { mutableStateOf(false) }

    // GitHub Güncelleme Kontrolü
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var updateInfoState by remember { mutableStateOf<com.example.util.AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingApk by remember { mutableStateOf(false) }
    var downloadProgressPercent by remember { mutableIntStateOf(0) }

    if (showUpdateDialog && updateInfoState != null) {
        AppUpdateDialog(
            updateInfo = updateInfoState!!,
            isDownloading = isDownloadingApk,
            downloadProgress = downloadProgressPercent,
            onConfirmUpdate = {
                val downloadUrl = updateInfoState?.downloadUrl.orEmpty()
                if (downloadUrl.isNotBlank()) {
                    isDownloadingApk = true
                    downloadProgressPercent = 0
                    coroutineScope.launch {
                        val downloadResult = com.example.util.AppUpdateChecker.downloadApk(
                            context = context,
                            downloadUrl = downloadUrl,
                            onProgress = { progress ->
                                downloadProgressPercent = progress
                            }
                        )
                        isDownloadingApk = false
                        downloadResult.onSuccess { apkFile ->
                            showUpdateDialog = false
                            com.example.util.AppUpdateChecker.installApk(context, apkFile)
                        }.onFailure { e ->
                            Toast.makeText(context, "Güncelleme indirilemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }

    // NOTIFICATIONS DIALOG (Geliştirilmiş Bildirim & Aksiyon Merkezi)
    if (isNotificationDialogOpen) {
        NotificationSheet(
            dashboardState = dashboardState,
            allProducts = allProducts,
            morningReminderEnabled = morningReminderEnabled,
            criticalAlertEnabled = criticalAlertEnabled,
            highStockAlertEnabled = highStockAlertEnabled,
            onDismiss = { isNotificationDialogOpen = false },
            onFilterSelected = { filter -> viewModel.onFilterSelected(filter) },
            onNavigate = { route -> navController.navigate(route) },
            onMarkAllAsRead = { viewModel.markNotificationsAsRead() },
            onToggleMorningReminder = { viewModel.toggleMorningCheckReminder() },
            onToggleCriticalAlert = { viewModel.toggleCriticalSktAlert() },
            onToggleHighStockAlert = { viewModel.toggleHighStockAlert() },
            onRemoveFromShelf = { prod -> viewModel.removeProductFromShelf(prod) },
            onRemoveMultipleFromShelf = { list -> viewModel.removeMultipleProductsFromShelf(list) },
            onAddToAdetsel = { prod -> viewModel.addToAdetsel(prod) },
            onOpenProductDetail = { prod -> viewModel.openProductDetailModal(prod) }
        )
    }

    // USER PROFILE DIALOG (Geliştirilmiş Personel Profili & Reyon Sayfası)
    if (isProfileDialogOpen) {
        ProfileSheet(
            userName = currentUser?.fullName ?: userName,
            userBranch = userBranch,
            userRole = currentUser?.let { "${it.roleTitle} (${it.role})" } ?: userRole,
            userDepartment = currentUser?.department ?: userDepartment,
            userDutyStatus = userDutyStatus,
            soundEffectsEnabled = soundEffectsEnabled,
            vibrationEnabled = vibrationEnabled,
            isBatterySaverMode = isBatterySaverMode,
            dashboardState = dashboardState,
            onDismiss = { isProfileDialogOpen = false },
            onUpdateProfile = { name, branch, role, dept ->
                viewModel.updateUserProfile(name, branch, role, dept)
            },
            onUpdateDutyStatus = { status -> viewModel.updateDutyStatus(status) },
            onToggleSoundEffects = { viewModel.toggleSoundEffects() },
            onToggleVibration = { viewModel.toggleVibration() },
            onToggleBatterySaverMode = { viewModel.toggleBatterySaverMode() },
            onNavigateToCsv = { navController.navigate("csv") }
        )
    }

    // ADD / EDIT PRODUCT MODAL
    if (isAddEditModalOpen) {
        AddEditProductModal(
            product = editingProduct,
            prefilledBarcode = prefilledBarcode,
            onDismiss = { viewModel.closeAddEditModal() },
            onSave = { barkod, urunKodu, urunAdi, kategori, sktTarihi, stokAdedi, isNewSkt, fiyat, isImportant ->
                if (isNewSkt && editingProduct != null) {
                    viewModel.addSktToExistingProduct(editingProduct!!, sktTarihi, stokAdedi) {
                        viewModel.closeAddEditModal()
                    }
                } else {
                    viewModel.saveProduct(barkod, urunKodu, urunAdi, kategori, sktTarihi, stokAdedi, fiyat, isImportant)
                }
            },
            onDelete = if (editingProduct != null) {
                { prod -> viewModel.deleteProductWithAllBatches(prod) }
            } else null
        )
    }

    // PRODUCT DETAIL MODAL (Ürün Detay Sayfası)
    if (detailProduct != null) {
        val allMatchingProducts = allProducts.filter {
            it.barkod.equals(detailProduct!!.barkod.trim(), ignoreCase = true) ||
            (detailProduct!!.urunKodu.isNotBlank() && it.urunKodu.equals(detailProduct!!.urunKodu.trim(), ignoreCase = true))
        }.sortedBy { it.sktTarihi }

        ProductDetailModal(
            product = detailProduct,
            matchingProducts = allMatchingProducts.ifEmpty { listOf(detailProduct!!) },
            onDismiss = { viewModel.closeProductDetailModal() },
            onEditClick = { prod ->
                viewModel.openEditProductModal(prod)
            },
            onAddNewSktClick = { prod ->
                viewModel.openAddSktModal(prod)
            },
            onEditSktItem = { item, newSkt, newCount ->
                viewModel.updateSktItem(item, newSkt, newCount)
            },
            onDeleteSkt = { prod ->
                viewModel.deleteProduct(prod)
            },
            onUpdatePrice = { prod, newPrice ->
                viewModel.updateProductPrice(prod, newPrice)
            },
            onAddToAdetsel = { prod ->
                viewModel.addToAdetsel(prod) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "${prod.urunAdi} adetsel listesine eklendi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "⚠️ Bu ürün zaten Adetsel Yapılacak listesinde mevcut!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeductStock = { prod, amount, reason ->
                viewModel.deductProductStock(prod, amount, reason)
            }
        )
    }

    // ADD DEDICATED SKT MODAL
    if (addSktProduct != null) {
        AddSktModal(
            product = addSktProduct!!,
            onDismiss = { viewModel.closeAddSktModal() },
            onSaveSkt = { prod, sktTarihi, stokAdedi ->
                viewModel.addSktToExistingProduct(prod, sktTarihi, stokAdedi)
            }
        )
    }

    // CAMERA OR MANUAL BARCODE SCANNER SHEET
    if (isBarcodeScannerOpen) {
        BarcodeScannerSheet(
            products = allProducts,
            startInFixQrMode = startScannerInFixMode,
            isBatterySaverMode = isBatterySaverMode,
            onDismiss = {
                isBarcodeScannerOpen = false
                startScannerInFixMode = false
            },
            onFixQrScanned = { rawQr, onResult ->
                viewModel.fixProductBarcodeAndPriceFromQr(rawQr, onResult)
            },
            onBarcodeDetected = { scannedRaw ->
                viewModel.handleBarcodeScanned(
                    barkod = scannedRaw,
                    onFound = { productFound ->
                        isBarcodeScannerOpen = false
                        viewModel.openEditProductModal(productFound)
                    },
                    onNotFound = { barcodeNotFound ->
                        val notFoundClean = com.example.data.parseShelfQrPayload(barcodeNotFound).barcode.ifBlank { barcodeNotFound }
                        isBarcodeScannerOpen = false
                        viewModel.openAddProductModal(prefilledBarcode = notFoundClean)
                    }
                )
            },
            onAddSkt = { product, sktMillis, count ->
                viewModel.addSktToExistingProduct(product, sktMillis, count)
            },
            onDeductStock = { product, amount, reason ->
                viewModel.deductProductStock(product, amount, reason)
            }
        )
    }

    val focusManager = LocalFocusManager.current

    val navigateToTab: (String) -> Unit = { target ->
        if (target == currentRoute) {
            // Zaten mevcut sayfadayız, gereksiz recomposition ve navigasyon yapma
        } else {
            val user = currentUser
            if (target == "reports" && user?.canAccessReports == false) {
                Toast.makeText(context, "Raporlar sayfasına sadece MS ve MSY yetkilileri erişebilir.", Toast.LENGTH_SHORT).show()
            } else {
                if (target == "panel") {
                    val popped = navController.popBackStack("panel", inclusive = false)
                    if (!popped) {
                        navController.navigate("panel") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                } else {
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    // Android sistem geri tuşunda her zaman ana sayfaya dön
    BackHandler(enabled = currentRoute != "panel") {
        navigateToTab("panel")
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                SktBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { target ->
                        navigateToTab(target)
                    },
                    onScanClick = { isBarcodeScannerOpen = true },
                    userRoleCode = currentUser?.role ?: "MS"
                )
            },
            topBar = {
                if (currentRoute in listOf("panel", "products", "takip", "adetsel", "reports", "csv")) {
                    Column {
                        SktTopAppBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { query ->
                                viewModel.onSearchQueryChanged(query)
                            },
                            allProducts = allProducts,
                            onProductClick = { prod ->
                                viewModel.openProductDetailModal(prod)
                            },
                            onAddNewProductClick = {
                                viewModel.openAddProductModal()
                            },
                            onOpenScanner = { isBarcodeScannerOpen = true },
                            onBellClick = { isNotificationDialogOpen = true },
                            unreadCount = dashboardState.unreadNotificationCount,
                            onAvatarClick = { navigateToTab("reminders") },
                            onRemindersClick = { navigateToTab("reminders") },
                            onSettingsClick = { navigateToTab("csv") },
                            showHomeButton = currentRoute != "panel",
                            onHomeClick = { navigateToTab("panel") }
                        )
                        com.example.ui.components.TopBarLoadingBar(isLoading = isSyncingOrLoading)

                        AnimatedVisibility(
                            visible = migrationStatus != null && migrationStatus?.isVisible == true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            migrationStatus?.let { status ->
                                Surface(
                                    color = if (status.isSuccess) com.example.ui.theme.EmeraldSuccess else com.example.ui.theme.TurquoisePrimary,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (status.isSuccess) Icons.Default.Check else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = status.message,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.dismissMigrationStatus() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Kapat",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "panel",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            // 1. PANEL (DASHBOARD)
            composable("panel") {
                DashboardScreen(
                    state = dashboardState,
                    products = allProducts,
                    onQuickActionClick = { action ->
                        when (action) {
                            "add_product" -> viewModel.openAddProductModal()
                            "scan" -> isBarcodeScannerOpen = true
                            "reminders" -> navigateToTab("reminders")
                            "csv" -> navigateToTab("csv")
                            "adetsel" -> navigateToTab("adetsel")
                            "reports", "takip" -> navigateToTab("takip")
                            else -> navigateToTab("products")
                        }
                    },
                    onFilterSelectAndNavigate = { filter ->
                        viewModel.onFilterSelected(filter)
                        navigateToTab("products")
                    },
                    onProductClick = { prod ->
                        viewModel.openProductDetailModal(prod)
                    },
                    onViewAllProductsClick = {
                        viewModel.onFilterSelected(ProductFilter.ALL)
                        navigateToTab("products")
                    },
                    onAvatarClick = {
                        isProfileDialogOpen = true
                    },
                    onNotificationClick = {
                        isNotificationDialogOpen = true
                    }
                )
            }

            // 2. ÜRÜNLER (PRODUCTS)
            composable("products") {
                ProductsScreen(
                    products = filteredProducts,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    selectedGroupFilter = selectedGroupFilter,
                    startDateFilter = startDateFilter,
                    endDateFilter = endDateFilter,
                    onSearchQueryChange = { q -> viewModel.onSearchQueryChanged(q) },
                    onFilterSelect = { f -> viewModel.onFilterSelected(f) },
                    onGroupFilterSelect = { group -> viewModel.onGroupFilterSelected(group) },
                    onDateRangeSelect = { start, end -> viewModel.setDateRangeFilter(start, end) },
                    onClearDateRange = { viewModel.clearDateRangeFilter() },
                    onProductClick = { prod -> viewModel.openProductDetailModal(prod) },
                    onDeleteProduct = { prod -> viewModel.deleteProduct(prod) },
                    onAddProductClick = { viewModel.openAddProductModal() },
                    onQuickAddSkt = { prod -> viewModel.openAddSktModal(prod) },
                    onOpenQrFixMode = {
                        startScannerInFixMode = true
                        isBarcodeScannerOpen = true
                    }
                )
            }

            // 3. TAKİP (İADE & DEPO RED TAKİBİ)
            composable("takip") {
                TakipScreen(
                    products = allProducts,
                    onBackClick = {
                        navigateToTab("panel")
                    },
                    onOpenScanner = {
                        isBarcodeScannerOpen = true
                    }
                )
            }

            composable("reports") {
                TakipScreen(
                    products = allProducts,
                    onBackClick = {
                        navigateToTab("panel")
                    },
                    onOpenScanner = {
                        isBarcodeScannerOpen = true
                    }
                )
            }

            // 5. CSV VERİ AKTARIMI
            composable("csv") {
                CsvScreen(
                    isDarkMode = isDarkMode,
                    isBatterySaverMode = isBatterySaverMode,
                    soundEffectsEnabled = soundEffectsEnabled,
                    vibrationEnabled = vibrationEnabled,
                    products = allProducts,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleBatterySaverMode = { viewModel.toggleBatterySaverMode() },
                    onToggleSoundEffects = { viewModel.toggleSoundEffects() },
                    onToggleVibration = { viewModel.toggleVibration() },
                    onFixAndRepairDatabase = { callback -> viewModel.fixAndRepairDatabase(callback) },
                    onImportLines = { lines -> viewModel.importCsvLines(lines) },
                    onResetDatabase = { viewModel.resetAllData() },
                    onRestoreSeedData = { viewModel.restoreDefaultSeedData() },
                    onOpenQrFixMode = {
                        startScannerInFixMode = true
                        isBarcodeScannerOpen = true
                    },
                    onExportJsonBackup = { cb -> viewModel.createUnifiedBackupJson(context, cb) },
                    onSaveLocalBackup = { tag, cb -> viewModel.saveLocalBackup(context, tag, cb) },
                    onGetLocalBackups = { viewModel.getLocalBackups(context) },
                    onRestoreFromJson = { json, merge, cb -> viewModel.restoreFromJson(context, json, merge, cb) },
                    onBackClick = { navigateToTab("panel") }
                )
            }

            // 6. HATIRLATICILAR & MAĞAZA NOTLARI
            composable("reminders") {
                RemindersScreen(
                    onBackClick = {
                        navigateToTab("panel")
                    }
                )
            }

            // 7. ADETSEL SAYIM TAKİBİ
            composable("adetsel") {
                AdetselScreen(
                    yapilacakList = yapilacakAdetsel,
                    yapildiList = yapildiAdetsel,
                    onSaveSayim = { kayit, sonuc, fark, notlar ->
                        viewModel.saveAdetselSayim(kayit, sonuc, fark, notlar = notlar) {
                            val msg = when (sonuc) {
                                "EKSIK" -> "${kayit.urunAdi} ($fark Eksik) kaydedildi"
                                "FAZLA" -> "${kayit.urunAdi} (+$fark Fazla) kaydedildi"
                                else -> "${kayit.urunAdi} (Tam) kaydedildi"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUndoSayim = { kayit ->
                        viewModel.undoAdetselKayit(kayit)
                        Toast.makeText(context, "${kayit.urunAdi} tekrar yapılacaklar listesine alındı", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteKayit = { id ->
                        viewModel.deleteAdetselKayit(id)
                    },
                    onClearCompleted = {
                        viewModel.clearCompletedAdetselKayitlar()
                        Toast.makeText(context, "Tamamlanan sayımlar temizlendi", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToProducts = {
                        navigateToTab("products")
                    },
                    onBackClick = {
                        navigateToTab("panel")
                    }
                )
            }
        }
    }

    com.example.ui.components.ProfessionalLoadingOverlay(
        isLoading = isLoading,
        title = "İşlem Yapılıyor",
        message = loadingMessage
    )
}
}
