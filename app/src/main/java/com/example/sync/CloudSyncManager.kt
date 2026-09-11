package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.Product
import com.example.data.ProductDao
import com.example.data.TurDao
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class SyncState {
    CONNECTED, SYNCING, OFFLINE, ERROR
}

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val PREF_NAME = "a101_sync_prefs"
    private const val KEY_STORE_CODE = "store_code"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_DEVICE_ID = "device_id"

    private var prefs: SharedPreferences? = null
    private var firestore: FirebaseFirestore? = null
    private var productListener: ListenerRegistration? = null
    private var reportListener: ListenerRegistration? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.OFFLINE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _activeTeamMembers = MutableStateFlow(1)
    val activeTeamMembers: StateFlow<Int> = _activeTeamMembers.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private var productDaoRef: ProductDao? = null
    private var turDaoRef: TurDao? = null

    fun initialize(context: Context, productDao: ProductDao, turDao: TurDao?) {
        this.productDaoRef = productDao
        this.turDaoRef = turDao
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (getDeviceId().isBlank()) {
            val newDeviceId = UUID.randomUUID().toString().take(8)
            prefs?.edit()?.putString(KEY_DEVICE_ID, newDeviceId)?.apply()
        }

        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()
                try {
                    val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                                .setSizeBytes(50 * 1024 * 1024L) // 50 MB disk cache limit
                                .build()
                        )
                        .build()
                    db.firestoreSettings = settings
                } catch (e: Exception) {
                    Log.w(TAG, "Persistent cache settings already applied or not supported: ${e.message}")
                }
                firestore = db
                Log.d(TAG, "Firebase initialized successfully with persistent disk cache")
            } else {
                Log.d(TAG, "Firebase configuration not provided, running in standalone local mode")
            }
            _syncState.value = SyncState.CONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Firebase init error, running in local mode: ${e.message}")
            _syncState.value = SyncState.CONNECTED
        }

        startRealtimeListeners()
    }

    fun getStoreCode(): String {
        return prefs?.getString(KEY_STORE_CODE, "A101-MAĞAZA-101") ?: "A101-MAĞAZA-101"
    }

    fun setStoreCode(code: String) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isNotBlank()) {
            prefs?.edit()?.putString(KEY_STORE_CODE, cleanCode)?.apply()
            startRealtimeListeners()
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
        try {
            productListener?.remove()
            productListener = null
            reportListener?.remove()
            reportListener = null
            _syncState.value = SyncState.CONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listeners: ${e.message}")
        }
    }

    fun startRealtimeListeners() {
        stopRealtimeListeners()

        val storeCode = getStoreCode()
        val db = firestore ?: return

        _syncState.value = SyncState.SYNCING

        // Listen for live Product changes across team
        try {
            productListener = db.collection("stores")
                .document(storeCode)
                .collection("products")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed for products: ${error.message}")
                        _syncState.value = SyncState.OFFLINE
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _syncState.value = SyncState.CONNECTED
                        _lastSyncTimestamp.value = System.currentTimeMillis()

                        scope.launch {
                            snapshot.documentChanges.forEach { change ->
                                try {
                                    val data = change.document.data
                                    val id = (data["id"] as? Long)?.toInt() ?: change.document.id.hashCode()
                                    val barkod = data["barkod"] as? String ?: ""
                                    val urunKodu = data["urunKodu"] as? String ?: ""
                                    val urunAdi = data["urunAdi"] as? String ?: ""
                                    val kategori = data["kategori"] as? String ?: "Genel"
                                    val sktTarihi = (data["sktTarihi"] as? Long) ?: 0L
                                    val stokAdedi = (data["stokAdedi"] as? Long)?.toInt() ?: 0
                                    val eklenmeTarihi = (data["eklenmeTarihi"] as? Long) ?: System.currentTimeMillis()
                                    val isImportant = (data["isImportant"] as? Boolean) ?: false
                                    val sonKontrolTarihi = (data["sonKontrolTarihi"] as? Long) ?: 0L

                                    val remoteProduct = Product(
                                        id = id,
                                        barkod = barkod,
                                        urunKodu = urunKodu,
                                        urunAdi = urunAdi,
                                        kategori = kategori,
                                        sktTarihi = sktTarihi,
                                        stokAdedi = stokAdedi,
                                        eklenmeTarihi = eklenmeTarihi,
                                        isImportant = isImportant,
                                        sonKontrolTarihi = sonKontrolTarihi,
                                        fiyat = (data["fiyat"] as? Double) ?: (data["fiyat"] as? Long)?.toDouble()
                                    )

                                    when (change.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            productDaoRef?.insertProduct(remoteProduct)
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            productDaoRef?.deleteProduct(remoteProduct)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing remote product change: ${e.message}")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listener: ${e.message}")
            _syncState.value = SyncState.CONNECTED
        }
    }

    fun syncProductToCloud(product: Product) {
        val db = firestore ?: return
        val storeCode = getStoreCode()

        scope.launch {
            try {
                _syncState.value = SyncState.SYNCING
                val productMap = hashMapOf(
                    "id" to product.id,
                    "barkod" to product.barkod,
                    "urunKodu" to product.urunKodu,
                    "urunAdi" to product.urunAdi,
                    "kategori" to product.kategori,
                    "sktTarihi" to product.sktTarihi,
                    "stokAdedi" to product.stokAdedi,
                    "eklenmeTarihi" to product.eklenmeTarihi,
                    "isImportant" to product.isImportant,
                    "fiyat" to product.fiyat,
                    "sonKontrolTarihi" to product.sonKontrolTarihi,
                    "lastUpdatedBy" to getUserName(),
                    "lastUpdatedAt" to System.currentTimeMillis()
                )

                db.collection("stores")
                    .document(storeCode)
                    .collection("products")
                    .document(product.id.toString())
                    .set(productMap)
                    .addOnSuccessListener {
                        _syncState.value = SyncState.CONNECTED
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload product: ${e.message}")
            }
        }
    }

    fun deleteProductFromCloud(productId: Int) {
        val db = firestore ?: return
        val storeCode = getStoreCode()

        scope.launch {
            try {
                db.collection("stores")
                    .document(storeCode)
                    .collection("products")
                    .document(productId.toString())
                    .delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete product from cloud: ${e.message}")
            }
        }
    }

    fun clearAllCloudData() {
        val db = firestore ?: return
        val storeCode = getStoreCode()

        scope.launch {
            try {
                db.collection("stores")
                    .document(storeCode)
                    .collection("products")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val batch = db.batch()
                            for (doc in snapshot.documents) {
                                batch.delete(doc.reference)
                            }
                            batch.commit()
                        }
                    }

                db.collection("stores")
                    .document(storeCode)
                    .collection("reports")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val batch = db.batch()
                            for (doc in snapshot.documents) {
                                batch.delete(doc.reference)
                            }
                            batch.commit()
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cloud data: ${e.message}")
            }
        }
    }

    fun syncTourReportToCloud(rapor: TurRaporu, kayitlar: List<TurKontrolKaydi>) {
        val db = firestore ?: return
        val storeCode = getStoreCode()

        scope.launch {
            try {
                val reportMap = hashMapOf(
                    "id" to rapor.id,
                    "turTarihi" to rapor.turTarihi,
                    "hedefReyon" to rapor.hedefReyon,
                    "toplamUrunSayisi" to rapor.toplamUrunSayisi,
                    "satilanUrunSayisi" to rapor.satilanUrunSayisi,
                    "toplamSatilanAdet" to rapor.toplamSatilanAdet,
                    "fireUrunSayisi" to rapor.fireUrunSayisi,
                    "toplamFireAdet" to rapor.toplamFireAdet,
                    "notrUrunSayisi" to rapor.notrUrunSayisi,
                    "tamamlandiMi" to rapor.tamamlandiMi,
                    "createdBy" to getUserName()
                )

                db.collection("stores")
                    .document(storeCode)
                    .collection("reports")
                    .document(rapor.id.toString())
                    .set(reportMap)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload tour report: ${e.message}")
            }
        }
    }
}
