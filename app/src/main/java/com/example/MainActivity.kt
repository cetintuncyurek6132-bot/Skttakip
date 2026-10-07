package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.UserManager
import com.example.data.AppDatabase
import com.example.data.ProductRepository
import com.example.navigation.AppNavHost
import com.example.ui.MainViewModel
import com.example.ui.components.GlobalModalHost
import com.example.ui.components.ProfessionalLoadingOverlay
import com.example.ui.components.SktBottomNavBar
import com.example.ui.components.SktTopAppBar
import com.example.ui.components.TopBarLoadingBar
import com.example.ui.screens.AppSplashScreen
import com.example.ui.screens.scanner.ScannerOpenMode
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
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

    override fun attachBaseContext(newBase: Context) {
        try {
            val trLocale = Locale.forLanguageTag("tr-TR")
            Locale.setDefault(trLocale)
            val config = Configuration(newBase.resources.configuration)
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

        val db = AppDatabase.getDatabase(applicationContext)
        try {
            UserManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "UserManager init error", e)
        }

        val repository = ProductRepository(
            db.productDao(),
            db.reportDao(),
            db.adetselDao(),
            db.stockMovementDao(),
            db.stockLogDao()
        )
        mainViewModel = ViewModelProvider(this, MainViewModel.Factory(repository))[MainViewModel::class.java]
        inventoryViewModel = ViewModelProvider(this, InventoryViewModel.Factory(repository))[InventoryViewModel::class.java]
        adetselViewModel = ViewModelProvider(this, AdetselViewModel.Factory(repository))[AdetselViewModel::class.java]
        settingsViewModel = ViewModelProvider(this, SettingsViewModel.Factory(repository))[SettingsViewModel::class.java]

        try {
            MorningCheckWorker.scheduleDailyMorningCheck(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "Worker schedule error", e)
        }

        try {
            settingsViewModel.runStartupDataProtection(applicationContext)
        } catch (t: Throwable) {
            Log.e("MainActivity", "SafeStartup error: ${t.message}", t)
        }

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
                        onPendingNavigationConsumed = { pendingNavigationRoute.value = null }
                    )

                    if (isSplashVisible) {
                        AppSplashScreen(onSplashFinished = { isSplashVisible = false })
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
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val tabRoutes = remember { listOf("panel", "products", "takip", "adetsel") }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route ?: "main"

    val activeRoute = if (currentNavRoute == "main") {
        tabRoutes.getOrElse(pagerState.currentPage) { "panel" }
    } else {
        currentNavRoute
    }

    val navigateToTab: (String) -> Unit = { target ->
        val resolved = when (target) {
            "reports" -> "takip"
            else -> target
        }
        val targetPageIndex = tabRoutes.indexOf(resolved)
        if (targetPageIndex != -1) {
            if (currentNavRoute != "main") {
                navController.popBackStack("main", inclusive = false)
            }
            coroutineScope.launch {
                pagerState.scrollToPage(targetPageIndex)
            }
        } else {
            if (currentNavRoute != resolved) {
                navController.navigate(resolved) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Log.d("MainActivity", "Notification permission granted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val currentUser by UserManager.currentUser.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val loadingMessage by mainViewModel.loadingMessage.collectAsStateWithLifecycle()
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
    val stockMovements by inventoryViewModel.allStockMovements.collectAsStateWithLifecycle()
    val stockLogs by inventoryViewModel.allStockLogs.collectAsStateWithLifecycle()

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
    var scannerOpenMode by remember { mutableStateOf(ScannerOpenMode.BARCODE_SEARCH) }
    var isNotificationDialogOpen by remember { mutableStateOf(false) }
    var isProfileDialogOpen by remember { mutableStateOf(false) }

    // Auto-dismiss migration bar after 3 seconds
    LaunchedEffect(migrationStatus) {
        if (migrationStatus?.isVisible == true) {
            kotlinx.coroutines.delay(3000L)
            settingsViewModel.dismissMigrationStatus()
        }
    }

    // Android sistem geri tuşunda ana sayfaya (page 0) dön
    BackHandler(enabled = currentNavRoute == "main" && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                SktBottomNavBar(
                    currentRoute = activeRoute,
                    onNavigate = { target -> navigateToTab(target) },
                    onScanClick = {
                        scannerOpenMode = if (activeRoute == "adetsel") ScannerOpenMode.ADETSEL_SAYIM else ScannerOpenMode.BARCODE_SEARCH
                        isBarcodeScannerOpen = true
                    },
                    userRoleCode = currentUser?.role ?: "MS"
                )
            },
            topBar = {
                if (currentNavRoute == "main") {
                    Column {
                        SktTopAppBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { query -> inventoryViewModel.onSearchQueryChanged(query) },
                            allProducts = allProducts,
                            onProductClick = { prod -> inventoryViewModel.openProductDetailModal(prod) },
                            onAddNewProductClick = { inventoryViewModel.openAddProductModal() },
                            onOpenScanner = {
                                scannerOpenMode = if (activeRoute == "adetsel") ScannerOpenMode.ADETSEL_SAYIM else ScannerOpenMode.BARCODE_SEARCH
                                isBarcodeScannerOpen = true
                            },
                            onBellClick = { isNotificationDialogOpen = true },
                            unreadCount = dashboardState.unreadNotificationCount,
                            onAvatarClick = { navigateToTab("reminders") },
                            onSettingsClick = { navigateToTab("csv") },
                            showHomeButton = activeRoute != "panel",
                            onHomeClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                        TopBarLoadingBar(isLoading = isLoading)

                        AnimatedVisibility(
                            visible = migrationStatus?.isVisible == true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            migrationStatus?.let { status ->
                                Surface(
                                    color = if (status.isSuccess) EmeraldSuccess else TurquoisePrimary,
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
            AppNavHost(
                navController = navController,
                innerPadding = innerPadding,
                pagerState = pagerState,
                context = context,
                dashboardState = dashboardState,
                allProducts = allProducts,
                filteredProducts = filteredProducts,
                yapilacakAdetsel = yapilacakAdetsel,
                yapildiAdetsel = yapildiAdetsel,
                stockLogs = stockLogs,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                selectedGroupFilter = selectedGroupFilter,
                startDateFilter = startDateFilter,
                endDateFilter = endDateFilter,
                isDarkMode = isDarkMode,
                isBatterySaverMode = isBatterySaverMode,
                soundEffectsEnabled = soundEffectsEnabled,
                vibrationEnabled = vibrationEnabled,
                inventoryViewModel = inventoryViewModel,
                settingsViewModel = settingsViewModel,
                mainViewModel = mainViewModel,
                adetselViewModel = adetselViewModel,
                navigateToTab = { tab -> navigateToTab(tab) },
                onOpenScanner = { mode ->
                    scannerOpenMode = mode
                    isBarcodeScannerOpen = true
                },
                onOpenProfile = { isProfileDialogOpen = true },
                onOpenNotifications = { isNotificationDialogOpen = true }
            )
        }

        // TÜM GLOBAL DİYALOG VE MODAL YÖNETİMİ
        GlobalModalHost(
            mainViewModel = mainViewModel,
            inventoryViewModel = inventoryViewModel,
            adetselViewModel = adetselViewModel,
            settingsViewModel = settingsViewModel,
            currentUser = currentUser,
            allProducts = allProducts,
            stockLogs = stockLogs,
            dashboardState = dashboardState,
            currentRoute = activeRoute,
            isNotificationDialogOpen = isNotificationDialogOpen,
            onDismissNotificationDialog = { isNotificationDialogOpen = false },
            isProfileDialogOpen = isProfileDialogOpen,
            onDismissProfileDialog = { isProfileDialogOpen = false },
            isBarcodeScannerOpen = isBarcodeScannerOpen,
            scannerOpenMode = scannerOpenMode,
            onDismissBarcodeScanner = { isBarcodeScannerOpen = false },
            isAddEditModalOpen = isAddEditModalOpen,
            editingProduct = editingProduct,
            detailProduct = detailProduct,
            addSktProduct = addSktProduct,
            prefilledBarcode = prefilledBarcode,
            userName = userName,
            userBranch = userBranch,
            userRole = userRole,
            userDepartment = userDepartment,
            userDutyStatus = userDutyStatus,
            morningReminderEnabled = morningReminderEnabled,
            criticalAlertEnabled = criticalAlertEnabled,
            highStockAlertEnabled = highStockAlertEnabled,
            soundEffectsEnabled = soundEffectsEnabled,
            vibrationEnabled = vibrationEnabled,
            isBatterySaverMode = isBatterySaverMode,
            navigateToTab = { tab -> navigateToTab(tab) }
        )
    }

    ProfessionalLoadingOverlay(
        isLoading = isLoading,
        title = "İşlem Yapılıyor",
        message = loadingMessage
    )
}
