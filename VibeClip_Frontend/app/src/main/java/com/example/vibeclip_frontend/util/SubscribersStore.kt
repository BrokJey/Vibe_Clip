package com.example.vibeclip_frontend.util

import android.content.Context
import com.example.vibeclip_frontend.data.model.StoredSubscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Локальный список принятых подписчиков (на бэкенде нет GET-списка подписчиков). */
class SubscribersStore(context: Context) {
    private val prefs = context.getSharedPreferences("VibeClipPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "my_subscribers"

    fun getAll(): List<StoredSubscription> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<StoredSubscription>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun add(subscriber: StoredSubscription) {
        val updated = getAll()
            .filterNot { it.userId == subscriber.userId }
            .plus(subscriber)
        save(updated)
    }

    fun remove(userId: String) {
        save(getAll().filterNot { it.userId == userId })
    }

    fun updateAvatar(userId: String, avatarUrl: String?) {
        save(getAll().map { if (it.userId == userId) it.copy(avatarUrl = avatarUrl) else it })
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    private fun save(list: List<StoredSubscription>) {
        prefs.edit().putString(key, gson.toJson(list)).apply()
    }
}
