package com.ceyinfo.cerp.util

import android.content.Context
import android.content.SharedPreferences
import com.ceyinfo.cerp.data.model.BusinessUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cerp_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_ORG_ID = "organization_id"
        private const val KEY_IS_OWNER = "is_owner"
        private const val KEY_BU_ID = "business_unit_id"
        private const val KEY_BU_NAME = "business_unit_name"
        private const val KEY_BU_LEVEL = "business_unit_level"
        private const val KEY_PERMITTED_BUS = "permitted_bus"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var organizationId: String?
        get() = prefs.getString(KEY_ORG_ID, null)
        set(value) = prefs.edit().putString(KEY_ORG_ID, value).apply()

    var isOwner: Boolean
        get() = prefs.getBoolean(KEY_IS_OWNER, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_OWNER, value).apply()

    var businessUnitId: String?
        get() = prefs.getString(KEY_BU_ID, null)
        set(value) = prefs.edit().putString(KEY_BU_ID, value).apply()

    var businessUnitName: String?
        get() = prefs.getString(KEY_BU_NAME, null)
        set(value) = prefs.edit().putString(KEY_BU_NAME, value).apply()

    var businessUnitLevel: String?
        get() = prefs.getString(KEY_BU_LEVEL, null)
        set(value) = prefs.edit().putString(KEY_BU_LEVEL, value).apply()

    fun savePermittedBUs(bus: List<BusinessUnit>) {
        prefs.edit().putString(KEY_PERMITTED_BUS, gson.toJson(bus)).apply()
    }

    fun getPermittedBUs(): List<BusinessUnit> {
        val json = prefs.getString(KEY_PERMITTED_BUS, null) ?: return emptyList()
        val type = object : TypeToken<List<BusinessUnit>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
