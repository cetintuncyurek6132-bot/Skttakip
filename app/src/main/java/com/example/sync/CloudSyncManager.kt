package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.Product
import com.example.data.ProductDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class SyncState {
    CONNECTED, SYNCING, OFFLINE, ERROR
}

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val PREF_NAME = "skt_sync_prefs"
    private const val KEY_STORE_CODE = "store_code"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_DEVICE_ID = "device_id"

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.CONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _activeTeamMembers = MutableStateFlow(1)
    val activeTeamMembers: StateFlow<Int> = _activeTeamMembers.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private var productDaoRef: ProductDao? = null

    fun initialize(context: Context, productDao: ProductDao) {
        this.productDaoRef = productDao
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (getDeviceId().isBlank()) {
            val newDeviceId = UUID.randomUUID().toString().take(8)
            prefs?.edit()?.putString(KEY_DEVICE_ID, newDeviceId)?.apply()
        }

        _syncState.value = SyncState.CONNECTED
        Log.d(TAG, "CloudSyncManager initialized in local mode")
    }

    fun getStoreCode(): String {
        return prefs?.getString(KEY_STORE_CODE, "MAĞAZA-101") ?: "MAĞAZA-101"
    }

    fun setStoreCode(code: String) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isNotBlank()) {
            prefs?.edit()?.putString(KEY_STORE_CODE, cleanCode)?.apply()
        }
    }

    fun getUserName(): String {
        return prefs?.getString(KEY_USER_NAME, "Ekip Üyesi") ?: "Ekip Üyesi"
    }

    fun setUserName(name: String) {
        if (name.isNotBlank()) {
            prefs?.edit()?.putString(KEY_USER_NAME, name.trim())?.apply()
        }
    }

    fun getDeviceId(): String {
        return prefs?.getString(KEY_DEVICE_ID, "") ?: ""
    }

    fun setHasUserResetData(reset: Boolean) {
        prefs?.edit()?.putBoolean("user_has_reset_data", reset)?.apply()
    }

    fun hasUserResetData(): Boolean {
        return prefs?.getBoolean("user_has_reset_data", false) ?: false
    }

    fun stopRealtimeListeners() {
        _syncState.value = SyncState.CONNECTED
    }

    fun startRealtimeListeners() {
        _syncState.value = SyncState.CONNECTED
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }

    fun syncProductToCloud(product: Product) {
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }

    fun deleteProductFromCloud(productId: Int) {
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }

    fun clearAllCloudData() {
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }
}
