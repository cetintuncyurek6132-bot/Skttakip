package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.Product
import com.example.data.ProductDao
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resumeWithException

enum class SyncState {
    CONNECTED, SYNCING, OFFLINE, ERROR
}

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val PREF_NAME = "skt_sync_prefs"
    private const val KEY_STORE_CODE = "store_code"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_DEVICE_ID = "device_id"
    private const val COLLECTION_PRODUCTS = "products"

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.CONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _activeTeamMembers = MutableStateFlow(1)
    val activeTeamMembers: StateFlow<Int> = _activeTeamMembers.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private var productDaoRef: ProductDao? = null
    private var firestoreInstance: FirebaseFirestore? = null
    private var snapshotListener: ListenerRegistration? = null

    /**
     * Firebase ve Firestore'u Offline-First persistence ile başlatır.
     */
    fun initialize(context: Context, productDao: ProductDao) {
        this.productDaoRef = productDao
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (getDeviceId().isBlank()) {
            val newDeviceId = UUID.randomUUID().toString().take(8)
            prefs?.edit()?.putString(KEY_DEVICE_ID, newDeviceId)?.apply()
        }

        try {
            // FirebaseApp kontrolü
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val db = FirebaseFirestore.getInstance()
            try {
                // Offline-First Persistent Cache ayarı
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(100L * 1024 * 1024) // 100 MB yerel önbellek
                            .build()
                    )
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Firestore cache settings already initialized: ${e.message}")
            }

            firestoreInstance = db
            _syncState.value = SyncState.CONNECTED
            Log.d(TAG, "CloudSyncManager Firestore initialized successfully with offline persistence")

            // Gerçek zamanlı dinleyiciyi başlat ve ilk eşitlemeyi yap
            startRealtimeListeners()
            scope.launch {
                syncWithCloud()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Firestore init error: ${e.message}", e)
            _syncState.value = SyncState.OFFLINE
        }
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

    /**
     * Gerçek zamanlı Firestore dinleyicisini durdurur.
     */
    fun stopRealtimeListeners() {
        try {
            snapshotListener?.remove()
            snapshotListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error removing snapshot listener: ${e.message}")
        }
    }

    /**
     * Firestore "products" koleksiyonu için gerçek zamanlı dinleyici başlatır.
     */
    fun startRealtimeListeners() {
        val db = firestoreInstance ?: return
        val dao = productDaoRef ?: return

        stopRealtimeListeners()

        try {
            snapshotListener = db.collection(COLLECTION_PRODUCTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listen failed (running offline): ${error.message}")
                        _syncState.value = SyncState.OFFLINE
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        _syncState.value = SyncState.CONNECTED
                        _lastSyncTimestamp.value = System.currentTimeMillis()

                        scope.launch(Dispatchers.IO) {
                            try {
                                for (change in snapshots.documentChanges) {
                                    val data = change.document.data
                                    val prod = documentToProduct(change.document.id, data) ?: continue

                                    when (change.type) {
                                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                            val existing = if (prod.id > 0) dao.getProductById(prod.id) else null
                                            if (existing != null) {
                                                // Yerel veriyi güncelle (ezmeden güvenle birleştir)
                                                val merged = existing.copy(
                                                    urunAdi = if (prod.urunAdi.isNotBlank()) prod.urunAdi else existing.urunAdi,
                                                    urunKodu = if (prod.urunKodu.isNotBlank()) prod.urunKodu else existing.urunKodu,
                                                    kategori = if (prod.kategori.isNotBlank()) prod.kategori else existing.kategori,
                                                    stokAdedi = prod.stokAdedi,
                                                    sktTarihi = prod.sktTarihi,
                                                    fiyat = prod.fiyat ?: existing.fiyat,
                                                    resimUrl = prod.resimUrl ?: existing.resimUrl,
                                                    isImportant = prod.isImportant || existing.isImportant,
                                                    sonKontrolTarihi = maxOf(prod.sonKontrolTarihi, existing.sonKontrolTarihi)
                                                )
                                                dao.updateProduct(merged)
                                            } else {
                                                val byBarcodes = dao.getProductsByBarcode(prod.barkod)
                                                val exactMatch = byBarcodes.find { it.sktTarihi == prod.sktTarihi }
                                                if (exactMatch != null) {
                                                    val merged = exactMatch.copy(
                                                        stokAdedi = prod.stokAdedi,
                                                        fiyat = prod.fiyat ?: exactMatch.fiyat,
                                                        resimUrl = prod.resimUrl ?: exactMatch.resimUrl
                                                    )
                                                    dao.updateProduct(merged)
                                                } else {
                                                    dao.insertProduct(prod.copy(id = 0))
                                                }
                                            }
                                        }
                                        DocumentChange.Type.REMOVED -> {
                                            if (prod.id > 0) {
                                                val existing = dao.getProductById(prod.id)
                                                if (existing != null) {
                                                    dao.deleteProduct(existing)
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Realtime sync merge error: ${e.message}", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting realtime listeners: ${e.message}", e)
        }
    }

    /**
     * Ürünü Firestore'a senkronize eder (Offline-first / SetOptions.merge).
     */
    fun syncProductToCloud(product: Product) {
        _lastSyncTimestamp.value = System.currentTimeMillis()
        val db = firestoreInstance ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val docId = getDocumentIdForProduct(product)
                val data = productToMap(product)
                db.collection(COLLECTION_PRODUCTS)
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        _syncState.value = SyncState.CONNECTED
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore syncProduct error (queued locally): ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "syncProductToCloud exception: ${e.message}")
            }
        }
    }

    /**
     * Ürünü Firestore'dan siler.
     */
    fun deleteProductFromCloud(productId: Int) {
        _lastSyncTimestamp.value = System.currentTimeMillis()
        val db = firestoreInstance ?: return

        scope.launch(Dispatchers.IO) {
            try {
                db.collection(COLLECTION_PRODUCTS)
                    .document(productId.toString())
                    .delete()
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore delete error: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "deleteProductFromCloud exception: ${e.message}")
            }
        }
    }

    /**
     * Tüm ürünleri buluttan temizler (Veri sıfırlama).
     */
    fun clearAllCloudData() {
        _lastSyncTimestamp.value = System.currentTimeMillis()
        val db = firestoreInstance ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection(COLLECTION_PRODUCTS).get().awaitTask()
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitTask()
                Log.d(TAG, "Cleared all products from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "clearAllCloudData error: ${e.message}")
            }
        }
    }

    /**
     * Firestore'daki tüm ürünleri çekip yerel Room veritabanıyla güvenle birleştirir.
     * Mevcut yerel verileri silmez; eşleşenleri günceller, eksikleri ekler.
     */
    suspend fun syncWithCloud(): Boolean = withContext(Dispatchers.IO) {
        val db = firestoreInstance ?: return@withContext false
        val dao = productDaoRef ?: return@withContext false

        try {
            _syncState.value = SyncState.SYNCING
            val snapshot = db.collection(COLLECTION_PRODUCTS).get().awaitTask()

            val cloudProducts = mutableListOf<Product>()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val prod = documentToProduct(doc.id, data)
                if (prod != null) {
                    cloudProducts.add(prod)
                }
            }

            val localProducts = dao.getAllProductsList()
            val localById = localProducts.associateBy { it.id }
            val localByBarcodeAndSkt = localProducts.groupBy { "${it.barkod.trim()}_${it.sktTarihi}" }

            for (cloudProd in cloudProducts) {
                val existingLocal = if (cloudProd.id > 0) localById[cloudProd.id] else null
                if (existingLocal != null) {
                    val merged = existingLocal.copy(
                        urunAdi = if (cloudProd.urunAdi.isNotBlank()) cloudProd.urunAdi else existingLocal.urunAdi,
                        urunKodu = if (cloudProd.urunKodu.isNotBlank()) cloudProd.urunKodu else existingLocal.urunKodu,
                        kategori = if (cloudProd.kategori.isNotBlank()) cloudProd.kategori else existingLocal.kategori,
                        stokAdedi = cloudProd.stokAdedi,
                        sktTarihi = cloudProd.sktTarihi,
                        fiyat = cloudProd.fiyat ?: existingLocal.fiyat,
                        resimUrl = cloudProd.resimUrl ?: existingLocal.resimUrl,
                        isImportant = cloudProd.isImportant || existingLocal.isImportant,
                        sonKontrolTarihi = maxOf(cloudProd.sonKontrolTarihi, existingLocal.sonKontrolTarihi)
                    )
                    dao.updateProduct(merged)
                } else {
                    val key = "${cloudProd.barkod.trim()}_${cloudProd.sktTarihi}"
                    val match = localByBarcodeAndSkt[key]?.firstOrNull()
                    if (match == null) {
                        dao.insertProduct(cloudProd.copy(id = 0))
                    } else {
                        val merged = match.copy(
                            stokAdedi = cloudProd.stokAdedi,
                            fiyat = cloudProd.fiyat ?: match.fiyat,
                            resimUrl = cloudProd.resimUrl ?: match.resimUrl
                        )
                        dao.updateProduct(merged)
                    }
                }
            }

            // Yerel ürünleri de buluta aktar (mükerrerliği önlemek için)
            for (local in localProducts) {
                val docId = getDocumentIdForProduct(local)
                db.collection(COLLECTION_PRODUCTS)
                    .document(docId)
                    .set(productToMap(local), SetOptions.merge())
            }

            _syncState.value = SyncState.CONNECTED
            _lastSyncTimestamp.value = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e(TAG, "syncWithCloud error: ${e.message}", e)
            _syncState.value = SyncState.OFFLINE
            false
        }
    }

    private fun getDocumentIdForProduct(product: Product): String {
        return if (product.id > 0) {
            product.id.toString()
        } else {
            "${product.barkod.trim()}_${product.sktTarihi}"
        }
    }

    private fun productToMap(product: Product): Map<String, Any?> {
        return mapOf(
            "id" to product.id,
            "barkod" to product.barkod,
            "urunKodu" to product.urunKodu,
            "urunAdi" to product.urunAdi,
            "kategori" to product.kategori,
            "sktTarihi" to product.sktTarihi,
            "stokAdedi" to product.stokAdedi,
            "eklenmeTarihi" to product.eklenmeTarihi,
            "isImportant" to product.isImportant,
            "sonKontrolTarihi" to product.sonKontrolTarihi,
            "fiyat" to product.fiyat,
            "resimUrl" to product.resimUrl,
            "updatedAt" to System.currentTimeMillis(),
            "deviceId" to getDeviceId(),
            "storeCode" to getStoreCode()
        )
    }

    private fun documentToProduct(docId: String, data: Map<String, Any?>): Product? {
        val id = (data["id"] as? Number)?.toInt() ?: docId.toIntOrNull() ?: 0
        val barkod = (data["barkod"] as? String) ?: ""
        val urunAdi = (data["urunAdi"] as? String) ?: ""
        if (barkod.isBlank() && urunAdi.isBlank()) return null

        val urunKodu = (data["urunKodu"] as? String) ?: ""
        val kategori = (data["kategori"] as? String) ?: "Genel"
        val sktTarihi = (data["sktTarihi"] as? Number)?.toLong() ?: 0L
        val stokAdedi = (data["stokAdedi"] as? Number)?.toInt() ?: 0
        val eklenmeTarihi = (data["eklenmeTarihi"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val isImportant = (data["isImportant"] as? Boolean) ?: false
        val sonKontrolTarihi = (data["sonKontrolTarihi"] as? Number)?.toLong() ?: 0L
        val fiyat = (data["fiyat"] as? Number)?.toDouble()
        val resimUrl = data["resimUrl"] as? String

        return Product(
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
            fiyat = fiyat,
            resimUrl = resimUrl
        )
    }
}

/**
 * Tasks için suspend extension fonksiyonu (Harici bağımlılık gerektirmez)
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result, null)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
