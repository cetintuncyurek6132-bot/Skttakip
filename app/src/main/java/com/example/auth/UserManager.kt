package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UserAccount(
    val username: String,
    val password: String,
    val role: String = "USER",
    val roleTitle: String = "",
    val fullName: String,
    val department: String = ""
) {
    val canAccessSettings: Boolean get() = true
    val canAccessReports: Boolean get() = true
    val canEditUsers: Boolean get() = true
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
        UserAccount("Kullanici1", "3232", "ADMIN", "", "Kullanıcı 1", ""),
        UserAccount("Kullanici2", "3232", "USER", "", "Kullanıcı 2", ""),
        UserAccount("Kullanici3", "3232", "USER", "", "Kullanıcı 3", ""),
        UserAccount("Kullanici4", "3232", "USER", "", "Kullanıcı 4", "")
    )

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadUsers()

        val savedUsername = prefs?.getString(KEY_LOGGED_IN_USER, null)
        if (!savedUsername.isNullOrEmpty()) {
            val matched = _usersList.value.find { it.username.equals(savedUsername, ignoreCase = true) }
            if (matched != null) {
                _currentUser.value = matched
            } else {
                val defaultUser = _usersList.value.firstOrNull()
                _currentUser.value = defaultUser
            }
        } else {
            val defaultUser = _usersList.value.firstOrNull()
            if (defaultUser != null) {
                _currentUser.value = defaultUser
                prefs?.edit()?.putString(KEY_LOGGED_IN_USER, defaultUser.username)?.apply()
            }
        }
    }

    private fun loadUsers() {
        val jsonStr = prefs?.getString(KEY_USERS_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
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
                            role = obj.optString("role", "USER"),
                            roleTitle = obj.optString("roleTitle", ""),
                            fullName = obj.getString("fullName"),
                            department = obj.optString("department", "")
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
