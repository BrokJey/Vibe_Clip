package com.example.vibeclip_frontend.util

import com.example.vibeclip_frontend.BuildConfig

object MediaUrlResolver {
    private val baseHost: String = BuildConfig.API_BASE_URL
        .removeSuffix("/")
        .substringBefore("/api")

    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "$baseHost/${url.trimStart('/')}"
        }
    }
}
