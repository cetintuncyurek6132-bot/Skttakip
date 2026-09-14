package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BackupMetadata
import com.example.data.BackupRestoreResult
import com.example.data.DataMigrationManager
import com.example.data.MigrationStatus
import com.example.data.ProductRepository
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    // User Profile State
    private val _userName = MutableStateFlow("Ahmet Yılmaz")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBranch = MutableStateFlow("Kadıköy Şubesi #4102")
    val userBranch: StateFlow<String> = _userBranch.asStateFlow()

    private val _userRole = MutableStateFlow("Reyon Sorumlusu & SKT Görevlisi")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userDepartment = MutableStateFlow("Süt & Şarküteri Reyonu")
    val userDepartment: StateFlow<String> = _userDepartment.asStateFlow()

    private val _userDutyStatus = MutableStateFlow("Vardiyada (Aktif)")
    val userDutyStatus: StateFlow<String> = _userDutyStatus.asStateFlow()

    // Preferences & Theme
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isBatterySaverMode = MutableStateFlow(false)
    val isBatterySaverMode: StateFlow<Boolean> = _isBatterySaverMode.asStateFlow()

    // Alerts & Audio/Vibration Settings
    private val _morningCheckReminderEnabled = MutableStateFlow(true)
    val morningCheckReminderEnabled: StateFlow<Boolean> = _morningCheckReminderEnabled.asStateFlow()

    private val _criticalSktAlertEnabled = MutableStateFlow(true)
    val criticalSktAlertEnabled: StateFlow<Boolean> = _criticalSktAlertEnabled.asStateFlow()

    private val _highStockAlertEnabled = MutableStateFlow(true)
    val highStockAlertEnabled: StateFlow<Boolean> = _highStockAlertEnabled.asStateFlow()

    private val _soundEffectsEnabled = MutableStateFlow(true)
    val soundEffectsEnabled: StateFlow<Boolean> = _soundEffectsEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // Data Protection & Migration
    val migrationStatus: StateFlow<MigrationStatus?> = DataMigrationManager.migrationStatus

    fun dismissMigrationStatus() {
        DataMigrationManager.dismissStatus()
    }

    fun runStartupDataProtection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            DataMigrationManager.performStartupDataProtectionCheck(
                context = context,
                productDao = repository.productDao,
                reportDao = repository.reportDao,
                adetselDao = repository.adetselDao
            )
        }
    }

    fun updateUserProfile(
        name: String,
        branch: String,
        role: String = "Reyon Sorumlusu & SKT Görevlisi",
        department: String = "Süt & Şarküteri Reyonu"
    ) {
        if (name.isNotBlank()) _userName.value = name.trim()
        if (branch.isNotBlank()) _userBranch.value = branch.trim()
        if (role.isNotBlank()) _userRole.value = role.trim()
        if (department.isNotBlank()) _userDepartment.value = department.trim()
    }

    fun updateDutyStatus(status: String) {
        _userDutyStatus.value = status
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun toggleBatterySaverMode() {
        val nextVal = !_isBatterySaverMode.value
        _isBatterySaverMode.value = nextVal
        if (nextVal) {
            _isDarkMode.value = true
        }
    }

    fun setBatterySaverMode(enabled: Boolean) {
        _isBatterySaverMode.value = enabled
        if (enabled) {
            _isDarkMode.value = true
        }
    }

    fun toggleMorningCheckReminder() {
        _morningCheckReminderEnabled.value = !_morningCheckReminderEnabled.value
    }

    fun toggleCriticalSktAlert() {
        _criticalSktAlertEnabled.value = !_criticalSktAlertEnabled.value
    }

    fun toggleHighStockAlert() {
        _highStockAlertEnabled.value = !_highStockAlertEnabled.value
    }

    fun toggleSoundEffects() {
        _soundEffectsEnabled.value = !_soundEffectsEnabled.value
    }

    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
    }

    // Data Backup / Restore & Maintenance
    fun createUnifiedBackupJson(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.createUnifiedBackupJson(context)
            withContext(Dispatchers.Main) {
                onResult(json)
            }
        }
    }

    fun saveLocalBackup(context: Context, tag: String = "manual", onResult: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = repository.saveLocalBackup(context, tag)
            withContext(Dispatchers.Main) {
                onResult(file)
            }
        }
    }

    fun getLocalBackups(context: Context): List<BackupMetadata> {
        return repository.getLocalBackups(context)
    }

    fun restoreFromJson(
        context: Context,
        jsonString: String,
        mergeWithExisting: Boolean = true,
        onResult: (BackupRestoreResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.restoreFromJson(context, jsonString, mergeWithExisting)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun fixAndRepairDatabase(onResult: (Int, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepairHelper.repairDatabase(repository, onResult)
        }
    }

    fun resetAllData(onLoadingChange: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            onLoadingChange?.invoke(true, "Veritabanı Sıfırlanıyor...")
            try {
                CloudSyncManager.setHasUserResetData(true)
                repository.resetAllData()
            } finally {
                onLoadingChange?.invoke(false, "")
            }
        }
    }

    fun restoreDefaultSeedData(onLoadingChange: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            onLoadingChange?.invoke(true, "Varsayılan Ürünler Yükleniyor...")
            try {
                CloudSyncManager.setHasUserResetData(false)
                repository.reSeedDefaultData()
            } finally {
                onLoadingChange?.invoke(false, "")
            }
        }
    }

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
