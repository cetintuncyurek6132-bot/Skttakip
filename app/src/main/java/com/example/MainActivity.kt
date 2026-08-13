package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.ProductDetailModal
import com.example.ui.components.SktBottomNavBar
import com.example.ui.components.SktTopAppBar
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
import java.util.Locale
import com.example.ui.screens.GameScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trLocale = java.util.Locale.forLanguageTag("tr-TR")
        java.util.Locale.setDefault(trLocale)
        val config = resources.configuration
        config.setLocale(trLocale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        com.example.auth.UserManager.initialize(applicationContext)
        com.example.sync.CloudSyncManager.initialize(applicationContext, db.productDao(), db.turDao())
        val repository = ProductRepository(db.productDao(), db.reportDao(), db.turDao())
        viewModel = ViewModelProvider(this, MainViewModel.Factory(repository))[MainViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkMode) {
                SktMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SktMainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "panel"

    val currentUser by com.example.auth.UserManager.currentUser.collectAsStateWithLifecycle()

    if (currentUser == null) {
        com.example.ui.screens.LoginScreen(
            onLoginSuccess = { user ->
                viewModel.updateUserProfile(
                    name = user.fullName,
                    branch = "Kadıköy Şubesi #4102",
                    role = "${user.roleTitle} (${user.role})",
                    department = user.department
                )
            }
        )
        return
    }

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

    // Game / Tour Mode State
    val gameTargetCategory by viewModel.gameTargetCategory.collectAsStateWithLifecycle()
    val gameActive by viewModel.gameActive.collectAsStateWithLifecycle()
    val gameScannedProducts by viewModel.gameScannedProducts.collectAsStateWithLifecycle()
    val lastScannedGameProduct by viewModel.lastScannedGameProduct.collectAsStateWithLifecycle()

    val tourQueue by viewModel.tourQueue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val tourLogs by viewModel.tourLogs.collectAsStateWithLifecycle()
    val tourFinished by viewModel.tourFinished.collectAsStateWithLifecycle()
    val lastSavedTourRaporu by viewModel.lastSavedTourRaporu.collectAsStateWithLifecycle()
    val allTurRaporlari by viewModel.allTurRaporlari.collectAsStateWithLifecycle()

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

    // Dialog & Scanner control states
    var isBarcodeScannerOpen by remember { mutableStateOf(false) }
    var startScannerInFixMode by remember { mutableStateOf(false) }
    var isNotificationDialogOpen by remember { mutableStateOf(false) }
    var isProfileDialogOpen by remember { mutableStateOf(false) }

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
            onToggleHighStockAlert = { viewModel.toggleHighStockAlert() }
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
            dashboardState = dashboardState,
            onDismiss = { isProfileDialogOpen = false },
            onUpdateProfile = { name, branch, role, dept ->
                viewModel.updateUserProfile(name, branch, role, dept)
            },
            onUpdateDutyStatus = { status -> viewModel.updateDutyStatus(status) },
            onToggleSoundEffects = { viewModel.toggleSoundEffects() },
            onToggleVibration = { viewModel.toggleVibration() },
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
                { prod -> viewModel.deleteProduct(prod) }
            } else null
        )
    }

    // PRODUCT DETAIL MODAL (Ürün Detay Sayfası)
    if (detailProduct != null) {
        val allMatchingProducts = filteredProducts.filter {
            it.barkod.equals(detailProduct!!.barkod.trim(), ignoreCase = true)
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
            products = filteredProducts,
            startInFixQrMode = startScannerInFixMode,
            onDismiss = {
                isBarcodeScannerOpen = false
                startScannerInFixMode = false
            },
            onFixQrScanned = { rawQr, onResult ->
                viewModel.fixProductBarcodeAndPriceFromQr(rawQr, onResult)
            },
            onBarcodeDetected = { scannedRaw ->
                if (currentRoute == "game" && gameActive) {
                    // We are in Gamification mode
                    viewModel.onGameScanBarcode(
                        barkod = scannedRaw,
                        onProductNotFound = {
                            isBarcodeScannerOpen = false
                            viewModel.openAddProductModal(prefilledBarcode = scannedRaw)
                        }
                    )
                    isBarcodeScannerOpen = false
                } else {
                    // Normal app barcode search mode
                    viewModel.handleBarcodeScanned(
                        barkod = scannedRaw,
                        onFound = { productFound ->
                            isBarcodeScannerOpen = false
                            viewModel.openEditProductModal(productFound)
                        },
                        onNotFound = { barcodeNotFound ->
                            isBarcodeScannerOpen = false
                            viewModel.openAddProductModal(prefilledBarcode = barcodeNotFound)
                        }
                    )
                }
            },
            onAddSkt = { product, sktMillis, count ->
                viewModel.addSktToExistingProduct(product, sktMillis, count)
            }
        )
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            focusManager.clearFocus(force = true)
                        }
                    }
                }
            }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (currentRoute != "game") {
                    SktBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { target ->
                            val user = currentUser
                            if (target == "reports" && user?.canAccessReports == false) {
                                Toast.makeText(context, "Raporlar sayfasına sadece MS ve MSY yetkilileri erişebilir.", Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(target) {
                                    popUpTo("panel") { inclusive = (target == "panel") }
                                }
                            }
                        },
                        onScanClick = { isBarcodeScannerOpen = true },
                        userRoleCode = currentUser?.role ?: "MS"
                    )
                }
            },
            topBar = {
                if (currentRoute != "game") {
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
                            onAvatarClick = { isProfileDialogOpen = true }
                        )
                        com.example.ui.components.TopBarLoadingBar(isLoading = isSyncingOrLoading)
                    }
                }
            }
        ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "panel",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. PANEL (DASHBOARD)
            composable("panel") {
                DashboardScreen(
                    state = dashboardState,
                    onStartGameClick = {
                        navController.navigate("game")
                    },
                    onQuickActionClick = { action ->
                        when (action) {
                            "scan" -> isBarcodeScannerOpen = true
                            "csv" -> navController.navigate("csv")
                            "reports" -> {
                                if (currentUser?.canAccessReports == true) {
                                    navController.navigate("reports")
                                } else {
                                    Toast.makeText(context, "Raporlar sayfasına sadece MS ve MSY yetkilileri erişebilir.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> navController.navigate("products")
                        }
                    },
                    onFilterSelectAndNavigate = { filter ->
                        viewModel.onFilterSelected(filter)
                        navController.navigate("products")
                    },
                    onProductClick = { prod ->
                        viewModel.openProductDetailModal(prod)
                    },
                    onViewAllProductsClick = {
                        viewModel.onFilterSelected(ProductFilter.ALL)
                        navController.navigate("products")
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
                    onOpenQrFixMode = {
                        startScannerInFixMode = true
                        isBarcodeScannerOpen = true
                    }
                )
            }

            // 3. KONTROL OYUNU (SABAH KONTROL TURU)
            composable("game") {
                GameScreen(
                    targetCategory = gameTargetCategory,
                    onCategoryChange = { cat -> viewModel.setGameTargetCategory(cat) },
                    tourActive = gameActive,
                    tourFinished = tourFinished,
                    tourQueue = tourQueue,
                    currentQueueIndex = currentQueueIndex,
                    tourLogs = tourLogs,
                    lastSavedReport = lastSavedTourRaporu,
                    onStartTour = { viewModel.startTourSession() },
                    onRecordSold = { prod, count -> viewModel.recordTourSold(prod, count) },
                    onRecordFire = { prod, count -> viewModel.recordTourFire(prod, count) },
                    onRecordNotr = { prod -> viewModel.recordTourNotr(prod) },
                    onUndoLastAction = { viewModel.undoLastTourAction() },
                    onCancelTour = { viewModel.cancelTourSession() },
                    onResetTour = { viewModel.resetTourState() },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 4. RAPORLAR (REPORTS)
            composable("reports") {
                ReportsScreen(
                    reports = allReports,
                    turRaporlari = allTurRaporlari,
                    onBackClick = {
                        navController.navigate("panel") {
                            popUpTo("panel") { inclusive = true }
                        }
                    },
                    onDeleteReport = { reportId -> viewModel.deleteReport(reportId) },
                    onDeleteTurRaporu = { turId -> viewModel.deleteTurRaporu(turId) },
                    onClearAllReports = {
                        viewModel.clearAllReports()
                        viewModel.clearAllTurRaporlari()
                    }
                )
            }

            // 5. CSV VERİ AKTARIMI
            composable("csv") {
                val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
                val soundEffectsEnabled by viewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
                val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
                val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

                CsvScreen(
                    isDarkMode = isDarkMode,
                    soundEffectsEnabled = soundEffectsEnabled,
                    vibrationEnabled = vibrationEnabled,
                    products = allProducts,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleSoundEffects = { viewModel.toggleSoundEffects() },
                    onToggleVibration = { viewModel.toggleVibration() },
                    onFixAndRepairDatabase = { callback -> viewModel.fixAndRepairDatabase(callback) },
                    onImportLines = { lines -> viewModel.importCsvLines(lines) },
                    onResetDatabase = { viewModel.resetAllData() },
                    onRestoreSeedData = { viewModel.restoreDefaultSeedData() },
                    onOpenQrFixMode = {
                        startScannerInFixMode = true
                        isBarcodeScannerOpen = true
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
