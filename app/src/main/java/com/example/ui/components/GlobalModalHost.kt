package com.example.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.auth.UserAccount
import com.example.data.Product
import com.example.data.StockLog
import com.example.data.findMatchingProducts
import com.example.data.parseShelfQrPayload
import com.example.ui.DashboardState
import com.example.ui.MainViewModel
import com.example.ui.screens.BarcodeScannerSheet
import com.example.ui.screens.NotificationSheet
import com.example.ui.screens.ProfileSheet
import com.example.ui.screens.scanner.ScannerOpenMode
import com.example.ui.viewmodel.AdetselViewModel
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GlobalModalHost(
    mainViewModel: MainViewModel,
    inventoryViewModel: InventoryViewModel,
    adetselViewModel: AdetselViewModel,
    settingsViewModel: SettingsViewModel,
    currentUser: UserAccount?,
    allProducts: List<Product>,
    stockLogs: List<StockLog>,
    dashboardState: DashboardState,
    currentRoute: String,
    // Dialog visibility states
    isNotificationDialogOpen: Boolean,
    onDismissNotificationDialog: () -> Unit,
    isProfileDialogOpen: Boolean,
    onDismissProfileDialog: () -> Unit,
    isBarcodeScannerOpen: Boolean,
    scannerOpenMode: ScannerOpenMode,
    onDismissBarcodeScanner: () -> Unit,
    // Modal states from ViewModels
    isAddEditModalOpen: Boolean,
    editingProduct: Product?,
    detailProduct: Product?,
    addSktProduct: Product?,
    prefilledBarcode: String,
    // User info & preferences
    userName: String,
    userBranch: String,
    userRole: String,
    userDepartment: String,
    userDutyStatus: String,
    morningReminderEnabled: Boolean,
    criticalAlertEnabled: Boolean,
    highStockAlertEnabled: Boolean,
    soundEffectsEnabled: Boolean,
    vibrationEnabled: Boolean,
    isBatterySaverMode: Boolean,
    // Navigation
    navigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. GITHUB UPDATE DIALOG
    var updateInfoState by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val downloadProgressInfo by AppUpdateChecker.downloadProgressState.collectAsState()

    LaunchedEffect(Unit) {
        AppUpdateChecker.checkCurrentDownloadStatus(context)
        delay(2000L)
        val updateResult = AppUpdateChecker.checkForUpdates()
        updateResult.onSuccess { info ->
            if (info.hasUpdate) {
                updateInfoState = info
                showUpdateDialog = true
            }
        }
    }

    LaunchedEffect(downloadProgressInfo.isCompleted) {
        if (downloadProgressInfo.isCompleted) {
            showUpdateDialog = false
        }
    }

    if (showUpdateDialog && updateInfoState != null) {
        AppUpdateDialog(
            updateInfo = updateInfoState!!,
            isDownloading = downloadProgressInfo.isDownloading,
            downloadProgress = downloadProgressInfo.progress,
            downloadedBytes = downloadProgressInfo.downloadedBytes,
            totalBytes = downloadProgressInfo.totalBytes,
            onConfirmUpdate = {
                val downloadUrl = updateInfoState?.downloadUrl.orEmpty()
                if (downloadUrl.isNotBlank()) {
                    AppUpdateChecker.startDownloadWithManager(
                        context = context,
                        downloadUrl = downloadUrl,
                        versionName = updateInfoState?.latestVersionName.orEmpty()
                    )
                }
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }

    // 2. NOTIFICATION SHEET (Bildirim & Aksiyon Merkezi)
    if (isNotificationDialogOpen) {
        NotificationSheet(
            dashboardState = dashboardState,
            allProducts = allProducts,
            morningReminderEnabled = morningReminderEnabled,
            criticalAlertEnabled = criticalAlertEnabled,
            highStockAlertEnabled = highStockAlertEnabled,
            onDismiss = onDismissNotificationDialog,
            onFilterSelected = { filter -> inventoryViewModel.onFilterSelected(filter) },
            onNavigate = { route -> navigateToTab(route) },
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

    // 3. USER PROFILE SHEET (Personel Profili & Reyon Sayfası)
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
            onDismiss = onDismissProfileDialog,
            onUpdateProfile = { name, branch, role, dept ->
                settingsViewModel.updateUserProfile(name, branch, role, dept)
            },
            onUpdateDutyStatus = { status -> settingsViewModel.updateDutyStatus(status) },
            onToggleSoundEffects = { settingsViewModel.toggleSoundEffects() },
            onToggleVibration = { settingsViewModel.toggleVibration() },
            onToggleBatterySaverMode = { settingsViewModel.toggleBatterySaverMode() },
            onNavigateToCsv = { navigateToTab("csv") }
        )
    }

    // 4. ADD / EDIT PRODUCT MODAL
    if (isAddEditModalOpen) {
        AddEditProductModal(
            product = editingProduct,
            prefilledBarcode = prefilledBarcode,
            onDismiss = { inventoryViewModel.closeAddEditModal() },
            onSave = { barkod, urunKodu, urunAdi, kategori, sktTarihi, stokAdedi, isNewSkt, fiyat, isImportant ->
                if (isNewSkt && editingProduct != null) {
                    inventoryViewModel.addSktToExistingProduct(editingProduct, sktTarihi, stokAdedi) {
                        inventoryViewModel.closeAddEditModal()
                    }
                } else {
                    inventoryViewModel.saveProduct(barkod, urunKodu, urunAdi, kategori, sktTarihi, stokAdedi, fiyat, isImportant)
                    if (currentRoute == "adetsel") {
                        val newProduct = Product(
                            barkod = barkod.trim(),
                            urunKodu = urunKodu.trim(),
                            urunAdi = urunAdi.uppercase().trim(),
                            kategori = kategori,
                            sktTarihi = sktTarihi,
                            stokAdedi = stokAdedi,
                            fiyat = fiyat
                        )
                        adetselViewModel.addToAdetsel(newProduct) { isSuccess ->
                            if (isSuccess) {
                                Toast.makeText(context, "${newProduct.urunAdi} sayım listesine eklendi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onDelete = if (editingProduct != null) {
                { prod -> inventoryViewModel.deleteProductWithAllBatches(prod) }
            } else null
        )
    }

    // 5. PRODUCT DETAIL MODAL (Ürün Detay Sayfası)
    if (detailProduct != null) {
        val allMatchingProducts = remember(detailProduct, allProducts) {
            allProducts.filter {
                it.barkod.equals(detailProduct.barkod.trim(), ignoreCase = true) ||
                (detailProduct.urunKodu.isNotBlank() && it.urunKodu.equals(detailProduct.urunKodu.trim(), ignoreCase = true))
            }.sortedBy { it.sktTarihi }
        }

        ProductDetailModal(
            product = detailProduct,
            matchingProducts = allMatchingProducts.ifEmpty { listOf(detailProduct) },
            stockLogs = stockLogs,
            onDismiss = { inventoryViewModel.closeProductDetailModal() },
            onEditClick = { prod -> inventoryViewModel.openEditProductModal(prod) },
            onAddNewSktClick = { prod -> inventoryViewModel.openAddSktModal(prod) },
            onEditSktItem = { item, newSkt, newCount -> inventoryViewModel.updateSktItem(item, newSkt, newCount) },
            onDeleteSkt = { prod -> inventoryViewModel.deleteProduct(prod) },
            onUpdatePrice = { prod, newPrice -> inventoryViewModel.updateProductPrice(prod, newPrice) },
            onAddToAdetsel = { prod ->
                adetselViewModel.addToAdetsel(prod) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "${prod.urunAdi} adetsel listesine eklendi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Bu ürün zaten sayım listesinde ekli.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeductStock = { prod, amount, reason -> inventoryViewModel.deductProductStock(prod, amount, reason) }
        )
    }

    // 6. ADD DEDICATED SKT MODAL
    if (addSktProduct != null) {
        AddSktModal(
            product = addSktProduct,
            onDismiss = { inventoryViewModel.closeAddSktModal() },
            onSaveSkt = { prod, sktTarihi, stokAdedi ->
                inventoryViewModel.addSktToExistingProduct(prod, sktTarihi, stokAdedi)
            }
        )
    }

    // 7. CAMERA / BARCODE SCANNER SHEET
    if (isBarcodeScannerOpen) {
        BarcodeScannerSheet(
            products = allProducts,
            userName = currentUser?.fullName ?: userName,
            openMode = scannerOpenMode,
            startInFixQrMode = (scannerOpenMode == ScannerOpenMode.LABEL_FIX),
            isBatterySaverMode = isBatterySaverMode,
            soundEffectsEnabled = soundEffectsEnabled,
            vibrationEnabled = vibrationEnabled,
            onDismiss = onDismissBarcodeScanner,
            onFixQrScanned = { rawQr, onResult ->
                inventoryViewModel.fixProductBarcodeAndPriceFromQr(rawQr, onResult)
            },
            onBarcodeDetected = { scannedRaw ->
                if (scannerOpenMode == ScannerOpenMode.ADETSEL_SAYIM) {
                    val clean = scannedRaw.trim()
                    if (clean.isNotBlank()) {
                        val matches = allProducts.findMatchingProducts(clean)
                        val matchedProduct = matches.firstOrNull()
                        if (matchedProduct != null) {
                            adetselViewModel.addToAdetsel(matchedProduct) { isSuccess ->
                                if (isSuccess) {
                                    Toast.makeText(context, "${matchedProduct.urunAdi} sayım listesine eklendi", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "${matchedProduct.urunAdi} zaten sayım listesinde ekli.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val notFoundClean = parseShelfQrPayload(clean).barcode.ifBlank { clean }
                            Toast.makeText(context, "Ürün veritabanında bulunamadı. Yeni ürün ekleme ekranı açılıyor...", Toast.LENGTH_SHORT).show()
                            onDismissBarcodeScanner()
                            inventoryViewModel.openAddProductModal(prefilledBarcode = notFoundClean)
                        }
                    }
                } else {
                    inventoryViewModel.handleBarcodeScanned(
                        barkod = scannedRaw,
                        onFound = { productFound ->
                            onDismissBarcodeScanner()
                            inventoryViewModel.openEditProductModal(productFound)
                        },
                        onNotFound = { barcodeNotFound ->
                            val notFoundClean = parseShelfQrPayload(barcodeNotFound).barcode.ifBlank { barcodeNotFound }
                            onDismissBarcodeScanner()
                            inventoryViewModel.openAddProductModal(prefilledBarcode = notFoundClean)
                        }
                    )
                }
            },
            onAddSkt = { product, sktMillis, count ->
                inventoryViewModel.addSktToExistingProduct(product, sktMillis, count)
            },
            onDeductStock = { product, amount, reason ->
                inventoryViewModel.deductProductStock(product, amount, reason)
            }
        )
    }
}
