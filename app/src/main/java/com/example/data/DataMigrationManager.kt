package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class MigrationStatus(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val isSuccess: Boolean = true,
    val preservedProductsCount: Int = 0,
    val preservedDepoCount: Int = 0
)

object DataMigrationManager {
    private const val TAG = "DataMigrationManager"
    private const val PREFS_NAME = "skt_migration_prefs"
    private const val KEY_DATA_VERSION = "app_data_schema_version"
    private const val KEY_FIRST_INSTALL_TIME = "app_first_install_timestamp"
    private const val KEY_LAST_UPDATE_TIME = "app_last_data_protection_check"

    const val CURRENT_DATA_VERSION = 3

    private val _migrationStatus = MutableStateFlow<MigrationStatus?>(null)
    val migrationStatus: StateFlow<MigrationStatus?> = _migrationStatus.asStateFlow()

    fun dismissStatus() {
        _migrationStatus.value = null
    }

    /**
     * Inspects current database state and performs safe non-destructive migration.
     * Guarantees that existing user data is NEVER deleted or overwritten by default seed data.
     */
    suspend fun performStartupDataProtectionCheck(
        context: Context,
        productDao: ProductDao,
        reportDao: InspectionReportDao,
        adetselDao: AdetselDao? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedVersion = prefs.getInt(KEY_DATA_VERSION, 0)
            val now = System.currentTimeMillis()

            val existingProducts = productDao.getAllProductsList()
            val existingDepo = DepoIadeManager.loadRecords(context)
            val existingAdetsel = adetselDao?.getAllAdetselKayitlariDirect() ?: emptyList()

            val productCount = existingProducts.size
            val depoCount = existingDepo.size
            val adetselCount = existingAdetsel.size
            val hasExistingUserData = productCount > 0 || depoCount > 0 || adetselCount > 0

            Log.i(TAG, "Data check: storedVersion=$storedVersion, currentVersion=$CURRENT_DATA_VERSION, existingProducts=$productCount, existingDepo=$depoCount, existingAdetsel=$adetselCount")

            if (hasExistingUserData) {
                // 1. Existing user data detected! First, take a safety auto-backup
                DataBackupManager.saveAutoBackupToStorage(
                    context = context,
                    productDao = productDao,
                    reportDao = reportDao,
                    adetselDao = adetselDao,
                    tag = "protection_v${CURRENT_DATA_VERSION}"
                )

                // 2. Perform safe non-destructive migration / healing for all products if version upgraded
                if (storedVersion < CURRENT_DATA_VERSION) {
                    _migrationStatus.value = MigrationStatus(
                        isVisible = true,
                        title = "Veri Güvenliği & Güncelleme",
                        message = "Mevcut verileriniz bulundu. Verileriniz korunarak yeni sürüme aktarılıyor...",
                        isSuccess = true,
                        preservedProductsCount = productCount,
                        preservedDepoCount = depoCount
                    )

                    // Heal any inconsistent columns without dropping data
                    for (prod in existingProducts) {
                        val healed = com.example.util.ProductDataHealer.autoHealProduct(prod)
                        if (healed != prod) {
                            productDao.updateProduct(healed)
                        }
                    }

                    // Save new version
                    prefs.edit()
                        .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
                        .putLong(KEY_LAST_UPDATE_TIME, now)
                        .apply()

                    // Update UI with reassurance banner
                    _migrationStatus.value = MigrationStatus(
                        isVisible = true,
                        title = "Verileriniz Başarıyla Korundu",
                        message = "$productCount ürün, $depoCount takip kaydı ve $adetselCount adetsel sayım kaydı yeni sürüme kayıpsız aktarıldı.",
                        isSuccess = true,
                        preservedProductsCount = productCount,
                        preservedDepoCount = depoCount
                    )
                } else {
                    // Already at latest version, make sure timestamp is noted
                    prefs.edit().putLong(KEY_LAST_UPDATE_TIME, now).apply()
                }
            } else {
                // Brand new installation: mark current version
                if (storedVersion == 0) {
                    prefs.edit()
                        .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
                        .putLong(KEY_FIRST_INSTALL_TIME, now)
                        .putLong(KEY_LAST_UPDATE_TIME, now)
                        .apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in data protection check", e)
        }
    }
}
