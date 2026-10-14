package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini API üzerinden İngilizce sürüm notlarını dinamik olarak Türkçeye çevirir.
 * 
 * Başarısız olursa null döner, çağıran taraf yerel sözlüğe düşer.
 */
object GeminiTranslator {

    private const val TAG = "GeminiTranslator"
    
    // Çeviri kalitesini artırmak için sistem talimatı
    private val SYSTEM_PROMPT = """
        Sen bir Türkçe yazılım editörüsün. Mağaza çalışanlarının anlayacağı 
        sade, kibar ve net Türkçe cümleler yazıyorsun. 
        
        Kurallar:
        - İngilizce commit mesajını Türkçeye çevir
        - Teknik terimler varsa (Room, Compose, API, JDK, Kotlin) olduğu gibi bırak
        - Cümleyi 10-15 kelimede tut
        - "Eklendi", "düzeltildi", "iyileştirildi", "güncellendi" gibi 
          geçmiş zaman fiilleriyle bitir
        - Sadece çeviri metnini yaz, açıklama veya önsöz ekleme
        - Her satır bir madde olacak, başında madde işareti olmayacak
        - Birden fazla madde varsa ayrı satırlarda ver
    """.trimIndent()

    @Volatile
    private var model: GenerativeModel? = null

    /**
     * Lazy model initializer. İlk çağrıda oluşturur, sonra cache'ler.
     */
    private fun getModel(): GenerativeModel? {
        if (model == null) {
            synchronized(this) {
                if (model == null) {
                    model = try {
                        Firebase.ai(backend = GenerativeBackend.googleAI())
                            .generativeModel("gemini-2.0-flash-exp")
                    } catch (e: Exception) {
                        Log.e(TAG, "Gemini model oluşturulamadı", e)
                        null
                    }
                }
            }
        }
        return model
    }

    /**
     * İngilizce sürüm notlarını Gemini ile Türkçeye çevirir.
     * 
     * @param englishNotes GitHub Release'ten gelen ham İngilizce notlar (çok satırlı)
     * @return Türkçe çevrilmiş madde listesi veya hata durumunda null
     */
    suspend fun translateToTurkish(englishNotes: String): List<String>? = withContext(Dispatchers.IO) {
        if (englishNotes.isBlank()) return@withContext emptyList()
        
        val prompt = """
            $SYSTEM_PROMPT
            
            Şimdi aşağıdaki İngilizce commit mesajlarını Türkçeye çevir. 
            Her satırı ayrı bir madde olarak ver, aralarında boş satır bırakma:
            
            $englishNotes
        """.trimIndent()

        val generativeModel = getModel()
        if (generativeModel != null) {
            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    val lines = parseResponseLines(text)
                    if (lines.isNotEmpty()) {
                        Log.d(TAG, "Firebase AI ile çeviri başarılı (${lines.size} madde)")
                        return@withContext lines
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini çeviri hatası: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Gemini model yok, yerel sözlüğe düşülecek")
        }

        // Direct REST API fallback if API key is present in BuildConfig
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isNotBlank() && !apiKey.startsWith("MY_GEMINI")) {
            val restResult = translateViaRestApi(prompt, apiKey)
            if (restResult != null && restResult.isNotEmpty()) {
                Log.d(TAG, "Gemini REST API ile çeviri başarılı (${restResult.size} madde)")
                return@withContext restResult
            }
        }

        null
    }

    private fun parseResponseLines(text: String): List<String> {
        return text
            .lines()
            .map { it.trim() }
            .map { it.removePrefix("-").removePrefix("•").removePrefix("*").trim() }
            .filter { it.isNotBlank() && it.length >= 5 }
            .take(10) // En fazla 10 madde
    }

    private fun translateViaRestApi(prompt: String, apiKey: String): List<String>? {
        return try {
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val jsonBody = org.json.JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = org.json.JSONObject(responseText)
                val candidates = rootJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")
                if (!text.isNullOrBlank()) {
                    parseResponseLines(text)
                } else null
            } else {
                Log.w(TAG, "Gemini REST API hata kodu: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini REST API bağlantı hatası: ${e.message}")
            null
        }
    }
}
