package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UserAccount(
    val username: String,
    val password: String,
    val role: String,      // "MS", "MSY", "P1", "P2"
    val roleTitle: String, // "Mağaza Sorumlusu", etc.
    val fullName: String,
    val department: String = "Süt & Şarküteri Reyonu"
) {
    val canAccessSettings: Boolean get() = true
    val canAccessReports: Boolean get() = role == "MS" || role == "MSY"
    val canEditUsers: Boolean get() = role == "MS"
}

object UserManager {
    private const val PREF_NAME = "skt_user_prefs"
    private const val KEY_LOGGED_IN_USER = "logged_in_user"
    private const val KEY_USERS_JSON = "users_json_data"

    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _usersList = MutableStateFlow<List<UserAccount>>(emptyList())
    val usersList: StateFlow<List<UserAccount>> = _usersList.asStateFlow()

    private val defaultUsers = listOf(
        UserAccount("Cetin", "3232", "MS", "Mağaza Sorumlusu (MS)", "Çetin Tunçyürek"),
        UserAccount("Ayse", "3232", "MSY", "Mağaza Sorumlu Yardımcısı (MSY)", "Ayşe Yılmaz"),
        UserAccount("Aykut", "3232", "P1", "Personel 1 (P1)", "Aykut Demir"),
        UserAccount("Rumeysa", "3232", "P2", "Personel 2 (P2)", "Rümeysa Kaya")
    )

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadUsers()

        val savedUsername = prefs?.getString(KEY_LOGGED_IN_USER, null)
        if (!savedUsername.isNull_or_Empty()) {
            val matched = _usersList.value.find { it.username.equals(savedUsername, ignoreCase = true) }
            if (matched != null) {
                _currentUser.value = matched
                CloudSyncManager.setUserName("${matched.fullName} (${matched.role})")
            } else {
                val defaultUser = _usersList.value.firstOrNull()
                _currentUser.value = defaultUser
                if (defaultUser != null) {
                    CloudSyncManager.setUserName("${defaultUser.fullName} (${defaultUser.role})")
                }
            }
        } else {
            val defaultUser = _usersList.value.firstOrNull()
            if (defaultUser != null) {
                _currentUser.value = defaultUser
                prefs?.edit()?.putString(KEY_LOGGED_IN_USER, defaultUser.username)?.apply()
                CloudSyncManager.setUserName("${defaultUser.fullName} (${defaultUser.role})")
            }
        }
    }

    private fun loadUsers() {
        val jsonStr = prefs?.getString(KEY_USERS_JSON, null)
        if (jsonStr.isNull_or_Empty()) {
            _usersList.value = defaultUsers
            saveUsersToPrefs(defaultUsers)
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<UserAccount>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        UserAccount(
                            username = obj.getString("username"),
                            password = obj.getString("password"),
                            role = obj.getString("role"),
                            roleTitle = obj.getString("roleTitle"),
                            fullName = obj.getString("fullName"),
                            department = obj.optString("department", "Süt & Şarküteri Reyonu")
                        )
                    )
                }
                _usersList.value = list
            } catch (e: Exception) {
                _usersList.value = defaultUsers
            }
        }
    }

    private fun saveUsersToPrefs(users: List<UserAccount>) {
        try {
            val array = JSONArray()
            for (u in users) {
                val obj = JSONObject().apply {
                    put("username", u.username)
                    put("password", u.password)
                    put("role", u.role)
                    put("roleTitle", u.roleTitle)
                    put("fullName", u.fullName)
                    put("department", u.department)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString(KEY_USERS_JSON, array.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun login(usernameInput: String, passwordInput: String): Boolean {
        val cleanUser = usernameInput.trim()
        val cleanPass = passwordInput.trim()

        val user = _usersList.value.find {
            it.username.equals(cleanUser, ignoreCase = true) && it.password == cleanPass
        }

        return if (user != null) {
            _currentUser.value = user
            prefs?.edit()?.putString(KEY_LOGGED_IN_USER, user.username)?.apply()
            CloudSyncManager.setUserName("${user.fullName} (${user.role})")
            true
        } else {
            false
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs?.edit()?.remove(KEY_LOGGED_IN_USER)?.apply()
    }

    fun updateUserAccount(updatedUser: UserAccount) {
        val currentList = _usersList.value.toMutableList()
        val idx = currentList.indexOfFirst { it.username.equals(updatedUser.username, ignoreCase = true) }
        if (idx != -1) {
            currentList[idx] = updatedUser
            _usersList.value = currentList
            saveUsersToPrefs(currentList)

            if (_currentUser.value?.username.equals(updatedUser.username, ignoreCase = true)) {
                _currentUser.value = updatedUser
                CloudSyncManager.setUserName("${updatedUser.fullName} (${updatedUser.role})")
            }
        }
    }

    fun resetPasswordForUser(username: String, newPass: String): Boolean {
        val currentList = _usersList.value.toMutableList()
        val idx = currentList.indexOfFirst { it.username.equals(username.trim(), ignoreCase = true) }
        if (idx != -1) {
            val updated = currentList[idx].copy(password = newPass)
            currentList[idx] = updated
            _usersList.value = currentList
            saveUsersToPrefs(currentList)
            return true
        }
        return false
    }

    fun addUser(newUser: UserAccount): Boolean {
        val currentList = _usersList.value.toMutableList()
        if (currentList.any { it.username.equals(newUser.username, ignoreCase = true) }) {
            return false
        }
        currentList.add(newUser)
        _usersList.value = currentList
        saveUsersToPrefs(currentList)
        return true
    }

    fun resetToDefaults() {
        _usersList.value = defaultUsers
        saveUsersToPrefs(defaultUsers)
    }
}

private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()
