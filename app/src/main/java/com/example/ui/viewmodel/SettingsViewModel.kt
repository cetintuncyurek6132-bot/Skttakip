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
    private val _userName = MutableStateFlow("Kullanıcı")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBranch = MutableStateFlow("")
    val userBranch: StateFlow<String> = _userBranch.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userDepartment = MutableStateFlow("")
    val userDepartment: StateFlow<String> = _userDepartment.asStateFlow()

    private val _userDutyStatus = MutableStateFlow("Aktif")
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

    private var sharedPrefs: android.content.SharedPreferences? = null

    fun initializePreferences(context: Context) {
        val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        sharedPrefs = prefs
        _isDarkMode.value = prefs.getBoolean("is_dark_mode", false)
        _isBatterySaverMode.value = prefs.getBoolean("is_battery_saver", false)
        _morningCheckReminderEnabled.value = prefs.getBoolean("morning_reminder", true)
        _criticalSktAlertEnabled.value = prefs.getBoolean("critical_alert", true)
        _highStockAlertEnabled.value = prefs.getBoolean("high_stock_alert", true)
        _soundEffectsEnabled.value = prefs.getBoolean("sound_effects", true)
        _vibrationEnabled.value = prefs.getBoolean("vibration_effects", true)
        _userName.value = prefs.getString("user_name", "Kullanıcı") ?: "Kullanıcı"
        _userBranch.value = prefs.getString("user_branch", "") ?: ""
        _userRole.value = prefs.getString("user_role", "") ?: ""
        _userDepartment.value = prefs.getString("user_department", "") ?: ""
        _userDutyStatus.value = prefs.getString("user_duty_status", "Aktif") ?: "Aktif"
    }

    private fun persistBoolean(key: String, value: Boolean) {
        sharedPrefs?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun persistString(key: String, value: String) {
        sharedPrefs?.edit()?.putString(key, value)?.apply()
    }

    // Data Protection & Migration
    val migrationStatus: StateFlow<MigrationStatus?> = DataMigrationManager.migrationStatus

    fun dismissMigrationStatus() {
        DataMigrationManager.dismissStatus()
    }

    fun runStartupDataProtection(context: Context) {
        initializePreferences(context)
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
        if (name.isNotBlank()) {
            _userName.value = name.trim()
            persistString("user_name", name.trim())
        }
        if (branch.isNotBlank()) {
            _userBranch.value = branch.trim()
            persistString("user_branch", branch.trim())
        }
        if (role.isNotBlank()) {
            _userRole.value = role.trim()
            persistString("user_role", role.trim())
        }
        if (department.isNotBlank()) {
            _userDepartment.value = department.trim()
            persistString("user_department", department.trim())
        }
    }

    fun updateDutyStatus(status: String) {
        _userDutyStatus.value = status
        persistString("user_duty_status", status)
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        persistBoolean("is_dark_mode", next)
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        persistBoolean("is_dark_mode", dark)
    }

    fun toggleBatterySaverMode() {
        val nextVal = !_isBatterySaverMode.value
        _isBatterySaverMode.value = nextVal
        persistBoolean("is_battery_saver", nextVal)
        if (nextVal) {
            _isDarkMode.value = true
            persistBoolean("is_dark_mode", true)
        }
    }

    fun setBatterySaverMode(enabled: Boolean) {
        _isBatterySaverMode.value = enabled
        persistBoolean("is_battery_saver", enabled)
        if (enabled) {
            _isDarkMode.value = true
            persistBoolean("is_dark_mode", true)
        }
    }

    fun toggleMorningCheckReminder() {
        val next = !_morningCheckReminderEnabled.value
        _morningCheckReminderEnabled.value = next
        persistBoolean("morning_reminder", next)
    }

    fun toggleCriticalSktAlert() {
        val next = !_criticalSktAlertEnabled.value
        _criticalSktAlertEnabled.value = next
        persistBoolean("critical_alert", next)
    }

    fun toggleHighStockAlert() {
        val next = !_highStockAlertEnabled.value
        _highStockAlertEnabled.value = next
        persistBoolean("high_stock_alert", next)
    }

    fun toggleSoundEffects() {
        val next = !_soundEffectsEnabled.value
        _soundEffectsEnabled.value = next
        persistBoolean("sound_effects", next)
    }

    fun toggleVibration() {
        val next = !_vibrationEnabled.value
        _vibrationEnabled.value = next
        persistBoolean("vibration_effects", next)
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
