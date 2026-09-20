package com.example.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.AdetselKayit
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.MainViewModel
import com.example.ui.ProductFilter
import com.example.ui.ProductGroupFilter
import com.example.ui.screens.AdetselScreen
import com.example.ui.screens.CsvScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.TakipScreen
import com.example.ui.viewmodel.AdetselViewModel
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    context: Context,
    dashboardState: DashboardState,
    allProducts: List<Product>,
    filteredProducts: List<Product>,
    yapilacakAdetsel: List<AdetselKayit>,
    yapildiAdetsel: List<AdetselKayit>,
    searchQuery: String,
    selectedFilter: ProductFilter,
    selectedGroupFilter: ProductGroupFilter,
    startDateFilter: Long?,
    endDateFilter: Long?,
    isDarkMode: Boolean,
    isBatterySaverMode: Boolean,
    soundEffectsEnabled: Boolean,
    vibrationEnabled: Boolean,
    inventoryViewModel: InventoryViewModel,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    adetselViewModel: AdetselViewModel,
    navigateToTab: (String) -> Unit,
    onOpenScanner: (fixMode: Boolean) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "panel",
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        enterTransition = { fadeIn(animationSpec = tween(120)) },
        exitTransition = { fadeOut(animationSpec = tween(120)) },
        popEnterTransition = { fadeIn(animationSpec = tween(120)) },
        popExitTransition = { fadeOut(animationSpec = tween(120)) }
    ) {
        // 1. PANEL (DASHBOARD)
        composable("panel") {
            DashboardScreen(
                state = dashboardState,
                products = allProducts,
                pendingAdetselCount = yapilacakAdetsel.size,
                onQuickActionClick = { action ->
                    when (action) {
                        "add_product" -> inventoryViewModel.openAddProductModal()
                        "scan" -> onOpenScanner(false)
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
                onAvatarClick = onOpenProfile,
                onNotificationClick = onOpenNotifications
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
                onOpenQrFixMode = { onOpenScanner(true) }
            )
        }

        // 3. TAKİP (İADE & DEPO RED TAKİBİ)
        composable("takip") {
            TakipScreen(
                products = allProducts,
                onBackClick = { navigateToTab("panel") },
                onOpenScanner = { onOpenScanner(false) }
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
                    inventoryViewModel.importCsvLines(lines) { isLoading: Boolean, msg: String ->
                        if (isLoading) mainViewModel.showLoading(msg) else mainViewModel.hideLoading()
                    }
                },
                onResetDatabase = {
                    settingsViewModel.resetAllData { isLoading: Boolean, msg: String ->
                        if (isLoading) mainViewModel.showLoading(msg) else mainViewModel.hideLoading()
                    }
                    com.example.data.DepoIadeManager.clearAllRecords(context)
                },
                onRestoreSeedData = {
                    settingsViewModel.restoreDefaultSeedData { isLoading: Boolean, msg: String ->
                        if (isLoading) mainViewModel.showLoading(msg) else mainViewModel.hideLoading()
                    }
                },
                onOpenQrFixMode = { onOpenScanner(true) },
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
                onBackClick = { navigateToTab("panel") }
            )
        }

        // 7. ADETSEL SAYIM TAKİBİ
        composable("adetsel") {
            AdetselScreen(
                yapilacakList = yapilacakAdetsel,
                yapildiList = yapildiAdetsel,
                onSaveSayim = { kayit, sonuc, fark, notlar ->
                    adetselViewModel.saveAdetselSayim(
                        kayit = kayit,
                        sonuc = sonuc,
                        fark = fark,
                        notlar = notlar
                    ) {
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
                onNavigateToProducts = { navigateToTab("products") },
                onBackClick = { navigateToTab("panel") }
            )
        }
    }
}
