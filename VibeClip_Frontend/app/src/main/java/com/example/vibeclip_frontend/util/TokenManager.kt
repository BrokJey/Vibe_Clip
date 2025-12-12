package com.example.vibeclip_frontend.util

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("VibeClipPrefs", Context.MODE_PRIVATE)
    private val tokenKey = "auth_token"
    
    fun saveToken(token: String) {
        prefs.edit().putString(tokenKey, token).apply()
    }
    
    fun getToken(): String? {
        return prefs.getString(tokenKey, null)
    }
    
    fun clearToken() {
        prefs.edit().remove(tokenKey).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}


