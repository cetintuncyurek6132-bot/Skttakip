package com.example

import android.Manifest
import android.content.Intent
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
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import com.example.ui.screens.AppSplashScreen
import com.example.ui.screens.BarcodeScannerSheet
import com.example.ui.screens.CsvScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NotificationSheet
import com.example.ui.screens.ProfileSheet
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.TakipScreen
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.viewmodel.AdetselViewModel
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.worker.MorningCheckWorker
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    private lateinit var adetselViewModel: AdetselViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    private var pendingNavigationRoute = mutableStateOf<String?>(null)

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

        val deepLinkRoute = intent?.getStringExtra("navigate_to")
        if (!deepLinkRoute.isNullOrBlank()) {
            pendingNavigationRoute.value = deepLinkRoute
        }

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
            com.example.sync.CloudSyncManager.initialize(applicationContext, db.productDao())
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "CloudSyncManager init error", e)
        }

        val repository = ProductRepository(db.productDao(), db.reportDao(), db.adetselDao())
        mainViewModel = ViewModelProvider(this, MainViewModel.Factory(repository))[MainViewModel::class.java]
        inventoryViewModel = ViewModelProvider(this, InventoryViewModel.Factory(repository))[InventoryViewModel::class.java]
        adetselViewModel = ViewModelProvider(this, AdetselViewModel.Factory(repository))[AdetselViewModel::class.java]
        settingsViewModel = ViewModelProvider(this, SettingsViewModel.Factory(repository))[SettingsViewModel::class.java]

        try {
            MorningCheckWorker.scheduleDailyMorningCheck(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Worker schedule error", e)
        }

        settingsViewModel.runStartupDataProtection(applicationContext)

        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
            var isSplashVisible by remember { mutableStateOf(true) }
            val pendingRoute by pendingNavigationRoute

            MyApplicationTheme(darkTheme = isDarkMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SktMainApp(
                        mainViewModel = mainViewModel,
                        inventoryViewModel = inventoryViewModel,
                        adetselViewModel = adetselViewModel,
                        settingsViewModel = settingsViewModel,
                        pendingNavigationRoute = pendingRoute,
                        onPendingNavigationConsumed = {
                            pendingNavigationRoute.value = null
                        }
                    )

                    if (isSplashVisible) {
                        AppSplashScreen(
                            onSplashFinished = {
                                isSplashVisible = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val deepLinkRoute = intent.getStringExtra("navigate_to")
        if (!deepLinkRoute.isNullOrBlank()) {
            pendingNavigationRoute.value = deepLinkRoute
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.sync.CloudSyncManager.startRealtimeListeners()
    }

    override fun onPause() {
        super.onPause()
        com.example.sync.CloudSyncManager.stopRealtimeListeners()
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
fun SktMainApp(
    mainViewModel: MainViewModel,
    inventoryViewModel: InventoryViewModel,
    adetselViewModel: AdetselViewModel,
    settingsViewModel: SettingsViewModel,
    pendingNavigationRoute: String? = null,
    onPendingNavigationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "panel"

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
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

    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val loadingMessage by mainViewModel.loadingMessage.collectAsStateWithLifecycle()
    val syncState by com.example.sync.CloudSyncManager.syncState.collectAsStateWithLifecycle()
    val isSyncingOrLoading = isLoading || syncState == com.example.sync.SyncState.SYNCING

    val dashboardState by mainViewModel.dashboardState.collectAsStateWithLifecycle()
    val filteredProducts by inventoryViewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by inventoryViewModel.allProducts.collectAsStateWithLifecycle()
    val searchQuery by inventoryViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by inventoryViewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedGroupFilter by inventoryViewModel.selectedGroupFilter.collectAsStateWithLifecycle()
    val startDateFilter by inventoryViewModel.startDateFilter.collectAsStateWithLifecycle()
    val endDateFilter by inventoryViewModel.endDateFilter.collectAsStateWithLifecycle()

    val isAddEditModalOpen by inventoryViewModel.isAddEditModalOpen.collectAsStateWithLifecycle()
    val editingProduct by inventoryViewModel.editingProduct.collectAsStateWithLifecycle()
    val detailProduct by inventoryViewModel.detailProduct.collectAsStateWithLifecycle()
    val addSktProduct by inventoryViewModel.addSktProduct.collectAsStateWithLifecycle()
    val prefilledBarcode by inventoryViewModel.prefilledBarcode.collectAsStateWithLifecycle()

    val migrationStatus by settingsViewModel.migrationStatus.collectAsStateWithLifecycle()
    val yapilacakAdetsel by adetselViewModel.yapilacakAdetselKayitlari.collectAsStateWithLifecycle()
    val yapildiAdetsel by adetselViewModel.yapildiAdetselKayitlari.collectAsStateWithLifecycle()

    // Bilgilendirme çubuğunu 3 saniye sonra otomatik olarak kaybet
    LaunchedEffect(migrationStatus) {
        if (migrationStatus != null && migrationStatus?.isVisible == true) {
            kotlinx.coroutines.delay(3000L)
            settingsViewModel.dismissMigrationStatus()
        }
    }

    val userName by settingsViewModel.userName.collectAsStateWithLifecycle()
    val userBranch by settingsViewModel.userBranch.collectAsStateWithLifecycle()
    val userRole by settingsViewModel.userRole.collectAsStateWithLifecycle()
    val userDepartment by settingsViewModel.userDepartment.collectAsStateWithLifecycle()
    val userDutyStatus by settingsViewModel.userDutyStatus.collectAsStateWithLifecycle()
    val morningReminderEnabled by settingsViewModel.morningCheckReminderEnabled.collectAsStateWithLifecycle()
    val criticalAlertEnabled by settingsViewModel.criticalSktAlertEnabled.collectAsStateWithLifecycle()
    val highStockAlertEnabled by settingsViewModel.highStockAlertEnabled.collectAsStateWithLifecycle()
    val soundEffectsEnabled by settingsViewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val isBatterySaverMode by settingsViewModel.isBatterySaverMode.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()

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
                isDownloadingApk = false
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
            onFilterSelected = { filter -> inventoryViewModel.onFilterSelected(filter) },
            onNavigate = { route -> navController.navigate(route) },
            onMarkAllAsRead = { mainViewModel.markNotificationsAsRead() },
            onToggleMorningReminder = { settingsViewModel.toggleMorningCheckReminder() },
            onToggleCriticalAlert = { settingsViewModel.toggleCriticalSktAlert() },
            onToggleHighStockAlert = { settingsViewModel.toggleHighStockAlert() },
            onRemoveFromShelf = { prod -> inventoryViewModel.removeProductFromShelf(prod) },
            onRemoveMultipleFromShelf = { list -> inventoryViewModel.removeMultipleProductsFromShelf(list) },
            onAddToAdetsel = { prod ->
                adetselViewModel.addToAdetsel(prod) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "${prod.urunAdi} adetsel listesine eklendi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Bu ürün zaten sayım listesinde ekli.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onOpenProductDetail = { prod -> inventoryViewModel.openProductDetailModal(prod) }
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
                settingsViewModel.updateUserProfile(name, branch, role, dept)
            },
            onUpdateDutyStatus = { status -> settingsViewModel.updateDutyStatus(status) },
            onToggleSoundEffects = { settingsViewModel.toggleSoundEffects() },
            onToggleVibration = { settingsViewModel.toggleVibration() },
            onToggleBatterySaverMode = { settingsViewModel.toggleBatterySaverMode() },
            onNavigateToCsv = { navController.navigate("csv") }
        )
    }

    // ADD / EDIT PRODUCT MODAL
    if (isAddEditModalOpen) {
        AddEditProductModal(
            product = editingProduct,
            prefilledBarcode = prefilledBarcode,
            onDismiss = { inventoryViewModel.closeAddEditModal() },
            onSave = { barkod, urunKodu, urunAdi, kategori, sktTarihi, stokAdedi, isNewSkt, fiyat, isImportant ->
                if (isNewSkt && editingProduct != null) {
                    inventoryViewModel.addSktToExistingProduct(editingProduct!!, sktTarihi, stokAdedi) {
                        inventoryViewModel.closeAddEditModal()
                    }
                } else {
                    inventoryViewModel.saveProduct(barkod, urunKodu, urunAdi, kategori, sktTarihi, stokAdedi, fiyat, isImportant)
                }
            },
            onDelete = if (editingProduct != null) {
                { prod -> inventoryViewModel.deleteProductWithAllBatches(prod) }
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
            onDismiss = { inventoryViewModel.closeProductDetailModal() },
            onEditClick = { prod ->
                inventoryViewModel.openEditProductModal(prod)
            },
            onAddNewSktClick = { prod ->
                inventoryViewModel.openAddSktModal(prod)
            },
            onEditSktItem = { item, newSkt, newCount ->
                inventoryViewModel.updateSktItem(item, newSkt, newCount)
            },
            onDeleteSkt = { prod ->
                inventoryViewModel.deleteProduct(prod)
            },
            onUpdatePrice = { prod, newPrice ->
                inventoryViewModel.updateProductPrice(prod, newPrice)
            },
            onAddToAdetsel = { prod ->
                adetselViewModel.addToAdetsel(prod) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "${prod.urunAdi} adetsel listesine eklendi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Bu ürün zaten sayım listesinde ekli.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeductStock = { prod, amount, reason ->
                inventoryViewModel.deductProductStock(prod, amount, reason)
            }
        )
    }

    // ADD DEDICATED SKT MODAL
    if (addSktProduct != null) {
        AddSktModal(
            product = addSktProduct!!,
            onDismiss = { inventoryViewModel.closeAddSktModal() },
            onSaveSkt = { prod, sktTarihi, stokAdedi ->
                inventoryViewModel.addSktToExistingProduct(prod, sktTarihi, stokAdedi)
            }
        )
    }

    // CAMERA OR MANUAL BARCODE SCANNER SHEET
    if (isBarcodeScannerOpen) {
        BarcodeScannerSheet(
            products = allProducts,
            userName = currentUser?.fullName ?: userName,
            startInFixQrMode = startScannerInFixMode,
            isBatterySaverMode = isBatterySaverMode,
            onDismiss = {
                isBarcodeScannerOpen = false
                startScannerInFixMode = false
            },
            onFixQrScanned = { rawQr, onResult ->
                inventoryViewModel.fixProductBarcodeAndPriceFromQr(rawQr, onResult)
            },
            onBarcodeDetected = { scannedRaw ->
                inventoryViewModel.handleBarcodeScanned(
                    barkod = scannedRaw,
                    onFound = { productFound ->
                        isBarcodeScannerOpen = false
                        inventoryViewModel.openEditProductModal(productFound)
                    },
                    onNotFound = { barcodeNotFound ->
                        val notFoundClean = com.example.data.parseShelfQrPayload(barcodeNotFound).barcode.ifBlank { barcodeNotFound }
                        isBarcodeScannerOpen = false
                        inventoryViewModel.openAddProductModal(prefilledBarcode = notFoundClean)
                    }
                )
            },
            onAddSkt = { product, sktMillis, count ->
                inventoryViewModel.addSktToExistingProduct(product, sktMillis, count)
            },
            onDeductStock = { product, amount, reason ->
                inventoryViewModel.deductProductStock(product, amount, reason)
            }
        )
    }

    val focusManager = LocalFocusManager.current

    val navigateToTab: (String) -> Unit = { target ->
        if (target != currentRoute) {
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Android sistem geri tuşunda her zaman ana sayfaya dön
    BackHandler(enabled = currentRoute != "panel") {
        navigateToTab("panel")
    }

    // Bildirim veya harici intent ile gelen sekmeye yönlendir
    LaunchedEffect(pendingNavigationRoute) {
        val target = pendingNavigationRoute
        if (!target.isNullOrBlank()) {
            val resolvedTarget = when (target) {
                "reports", "takip" -> "takip"
                "products" -> "products"
                "adetsel" -> "adetsel"
                "csv" -> "csv"
                "reminders" -> "reminders"
                "panel" -> "panel"
                else -> target
            }
            navigateToTab(resolvedTarget)
            onPendingNavigationConsumed()
        }
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
                if (currentRoute in listOf("panel", "products", "takip", "adetsel", "csv")) {
                    Column {
                        SktTopAppBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { query ->
                                inventoryViewModel.onSearchQueryChanged(query)
                            },
                            allProducts = allProducts,
                            onProductClick = { prod ->
                                inventoryViewModel.openProductDetailModal(prod)
                            },
                            onAddNewProductClick = {
                                inventoryViewModel.openAddProductModal()
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
                                            onClick = { settingsViewModel.dismissMigrationStatus() },
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
            enterTransition = { fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)) },
            exitTransition = { fadeOut(animationSpec = tween(150, easing = LinearOutSlowInEasing)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)) },
            popExitTransition = { fadeOut(animationSpec = tween(150, easing = LinearOutSlowInEasing)) }
        ) {
            // 1. PANEL (DASHBOARD)
            composable("panel") {
                DashboardScreen(
                    state = dashboardState,
                    products = allProducts,
                    onQuickActionClick = { action ->
                        when (action) {
                            "add_product" -> inventoryViewModel.openAddProductModal()
                            "scan" -> isBarcodeScannerOpen = true
                            "reminders" -> navigateToTab("reminders")
                            "csv" -> navigateToTab("csv")
                            "adetsel" -> navigateToTab("adetsel")
                            "reports", "takip" -> navigateToTab("takip")
                            else -> navigateToTab("products")
                        }
                    },
                    onFilterSelectAndNavigate = { filter ->
                        inventoryViewModel.onFilterSelected(filter)
                        navigateToTab("products")
                    },
                    onProductClick = { prod ->
                        inventoryViewModel.openProductDetailModal(prod)
                    },
                    onViewAllProductsClick = {
                        inventoryViewModel.onFilterSelected(ProductFilter.ALL)
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
                    onSearchQueryChange = { q -> inventoryViewModel.onSearchQueryChanged(q) },
                    onFilterSelect = { f -> inventoryViewModel.onFilterSelected(f) },
                    onGroupFilterSelect = { group -> inventoryViewModel.onGroupFilterSelected(group) },
                    onDateRangeSelect = { start, end -> inventoryViewModel.setDateRangeFilter(start, end) },
                    onClearDateRange = { inventoryViewModel.clearDateRangeFilter() },
                    onProductClick = { prod -> inventoryViewModel.openProductDetailModal(prod) },
                    onDeleteProduct = { prod -> inventoryViewModel.deleteProduct(prod) },
                    onAddProductClick = { inventoryViewModel.openAddProductModal() },
                    onQuickAddSkt = { prod -> inventoryViewModel.openAddSktModal(prod) },
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

            // 5. CSV VERİ AKTARIMI & AYARLAR
            composable("csv") {
                CsvScreen(
                    isDarkMode = isDarkMode,
                    isBatterySaverMode = isBatterySaverMode,
                    soundEffectsEnabled = soundEffectsEnabled,
                    vibrationEnabled = vibrationEnabled,
                    products = allProducts,
                    onToggleDarkMode = { settingsViewModel.toggleDarkMode() },
                    onToggleBatterySaverMode = { settingsViewModel.toggleBatterySaverMode() },
                    onToggleSoundEffects = { settingsViewModel.toggleSoundEffects() },
                    onToggleVibration = { settingsViewModel.toggleVibration() },
                    onFixAndRepairDatabase = { callback -> settingsViewModel.fixAndRepairDatabase(callback) },
                    onImportLines = { lines ->
                        inventoryViewModel.importCsvLines(lines) { isLoading, msg ->
                            if (isLoading) mainViewModel.showLoading(msg) else mainViewModel.hideLoading()
                        }
                    },
                    onResetDatabase = {
                        settingsViewModel.resetAllData { isLoading, msg ->
                            if (isLoading) mainViewModel.showLoading(msg) else mainViewModel.hideLoading()
                        }
                        com.example.data.DepoIadeManager.clearAllRecords(context)
                    },
                    onRestoreSeedData = {
                        settingsViewModel.restoreDefaultSeedData { isLoading, msg ->
                            if (isLoading) mainViewModel.showLoading(msg) else mainViewModel.hideLoading()
                        }
                    },
                    onOpenQrFixMode = {
                        startScannerInFixMode = true
                        isBarcodeScannerOpen = true
                    },
                    onExportJsonBackup = { cb -> settingsViewModel.createUnifiedBackupJson(context, cb) },
                    onSaveLocalBackup = { tag, cb -> settingsViewModel.saveLocalBackup(context, tag, cb) },
                    onGetLocalBackups = { settingsViewModel.getLocalBackups(context) },
                    onRestoreFromJson = { json, merge, cb -> settingsViewModel.restoreFromJson(context, json, merge, cb) },
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
                        adetselViewModel.saveAdetselSayim(kayit, sonuc, fark, notlar = notlar) {
                            val msg = when (sonuc) {
                                "EKSIK" -> "${kayit.urunAdi} ($fark Adet Eksik) kaydedildi"
                                "FAZLA" -> "${kayit.urunAdi} (+$fark Fazla) kaydedildi"
                                else -> "${kayit.urunAdi} (Tam) kaydedildi"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUndoSayim = { kayit ->
                        adetselViewModel.undoAdetselKayit(kayit)
                        Toast.makeText(context, "${kayit.urunAdi} sayım listesine geri alındı.", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteKayit = { id ->
                        adetselViewModel.deleteAdetselKayit(id)
                    },
                    onClearCompleted = {
                        adetselViewModel.clearCompletedAdetselKayitlar()
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
