package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class UserAccount(
    val username: String = "Kullanici",
    val role: String = "ADMIN",
    val roleTitle: String = "",
    val fullName: String = "Kullanıcı",
    val department: String = ""
) {
    val canAccessSettings: Boolean get() = true
    val canAccessReports: Boolean get() = true
    val canEditUsers: Boolean get() = true
}

object UserManager {
    private const val PREF_NAME = "skt_user_prefs"
    private const val KEY_USER_PROFILE_JSON = "user_profile_json"

    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<UserAccount?>(UserAccount())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs?.getString(KEY_USER_PROFILE_JSON, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val obj = JSONObject(jsonStr)
                _currentUser.value = UserAccount(
                    username = obj.optString("username", "Kullanici"),
                    role = obj.optString("role", "ADMIN"),
                    roleTitle = obj.optString("roleTitle", ""),
                    fullName = obj.optString("fullName", "Kullanıcı"),
                    department = obj.optString("department", "")
                )
            } catch (_: Exception) {
                _currentUser.value = UserAccount()
            }
        } else {
            _currentUser.value = UserAccount()
        }
    }

    fun updateUserAccount(updatedUser: UserAccount) {
        _currentUser.value = updatedUser
        try {
            val obj = JSONObject().apply {
                put("username", updatedUser.username)
                put("role", updatedUser.role)
                put("roleTitle", updatedUser.roleTitle)
                put("fullName", updatedUser.fullName)
                put("department", updatedUser.department)
            }
            prefs?.edit()?.putString(KEY_USER_PROFILE_JSON, obj.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
