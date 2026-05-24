package com.example.vibeclip_frontend.util

import android.content.Context
import com.example.vibeclip_frontend.data.model.StoredSubscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Локальный список пользователей, на которых текущий пользователь подписался.
 * На бэкенде нет endpoint для списка принятых подписок — храним на устройстве.
 */
class SubscriptionsStore(context: Context) {
    private val prefs = context.getSharedPreferences("VibeClipPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "my_subscriptions"

    fun getAll(): List<StoredSubscription> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<StoredSubscription>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun add(subscription: StoredSubscription) {
        val updated = getAll()
            .filterNot { it.userId == subscription.userId }
            .plus(subscription)
        save(updated)
    }

    fun remove(userId: String) {
        save(getAll().filterNot { it.userId == userId })
    }

    fun updateAvatar(userId: String, avatarUrl: String?) {
        val updated = getAll().map {
            if (it.userId == userId) it.copy(avatarUrl = avatarUrl) else it
        }
        save(updated)
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    private fun save(list: List<StoredSubscription>) {
        prefs.edit().putString(key, gson.toJson(list)).apply()
    }
}
