package com.example.vibeclip_frontend.util

import androidx.media3.common.PlaybackException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class ErrorContext {
    General,
    AuthLogin,
    AuthRegister,
    VideoUpload,
    Subscription
}

data class UserFacingError(
    val message: String,
    val showRetry: Boolean = false
)

object ErrorMessages {

    const val VIDEO_UNAVAILABLE = "Видео не доступно"
    const val TIMEOUT = "Время ожидания загрузки превышено. Возможно у вас не подключен интернет или сервер временно не доступен. Попробуйте позже"
    const val NETWORK = "Сервер временно не работает. Попробуйте зайти позже."
    const val SERVER_UNAVAILABLE = "Сервер временно недоступен. Попробуйте позже"
    const val SESSION_EXPIRED = "Упс, ваша сессия истекла. Перезайдите в аккаунт для продолжения работы приложения."
    const val WRONG_CREDENTIALS = "Не верно введены учётные данные"
    const val ACCOUNT_NOT_FOUND = "Аккаунта с такими данными не существует. Зарегистрируйтесь"
    const val GENERIC = "Что-то пошло не так. Попробуйте позже"

    private val gson = Gson()

    fun fromThrowable(throwable: Throwable?, context: ErrorContext = ErrorContext.General): UserFacingError {
        if (throwable == null) return UserFacingError(GENERIC, showRetry = true)
        if (isTimeout(throwable)) return UserFacingError(TIMEOUT, showRetry = true)
        if (isNetwork(throwable)) return UserFacingError(NETWORK, showRetry = true)

        val raw = throwable.message.orEmpty()
        if (looksSessionExpired(raw)) {
            return UserFacingError(SESSION_EXPIRED, showRetry = false)
        }
        if (context == ErrorContext.AuthLogin) {
            return UserFacingError(mapAuthLoginMessage(raw), showRetry = false)
        }
        if (context == ErrorContext.AuthRegister) {
            return UserFacingError(mapAuthRegisterMessage(raw), showRetry = false)
        }

        val friendly = sanitizeRawMessage(raw)
        if (friendly != null) return UserFacingError(friendly, showRetry = true)
        return UserFacingError(GENERIC, showRetry = true)
    }

    fun fromHttp(
        statusCode: Int,
        errorBody: String?,
        context: ErrorContext = ErrorContext.General
    ): UserFacingError {
        if (isTimeoutCode(statusCode)) return UserFacingError(TIMEOUT, showRetry = true)

        when (context) {
            ErrorContext.AuthLogin -> {
                return UserFacingError(
                    mapLoginHttp(statusCode, errorBody),
                    showRetry = statusCode >= 500
                )
            }
            ErrorContext.AuthRegister -> {
                return UserFacingError(
                    mapRegisterHttp(statusCode, errorBody),
                    showRetry = statusCode >= 500
                )
            }
            else -> {
                val serverMsg = extractServerMessage(errorBody)
                val message = when (statusCode) {
                    401 -> "Требуется вход в аккаунт"
                    403 -> "Недостаточно прав для этого действия"
                    404 -> when (context) {
                        ErrorContext.Subscription -> "Пользователь не найден"
                        else -> "Данные не найдены"
                    }
                    409 -> serverMsg ?: "Действие уже выполнено"
                    in 500..599 -> SERVER_UNAVAILABLE
                    else -> serverMsg ?: GENERIC
                }
                return UserFacingError(message, showRetry = statusCode >= 500 || statusCode == 408)
            }
        }
    }

    fun fromPlaybackError(error: PlaybackException): UserFacingError {
        if (isPlaybackTimeout(error)) {
            return UserFacingError(TIMEOUT, showRetry = true)
        }
        return UserFacingError(VIDEO_UNAVAILABLE, showRetry = true)
    }

    fun messageOnly(throwable: Throwable?, context: ErrorContext = ErrorContext.General): String =
        fromThrowable(throwable, context).message

    fun messageFromHttp(
        statusCode: Int,
        errorBody: String?,
        context: ErrorContext = ErrorContext.General
    ): String = fromHttp(statusCode, errorBody, context).message

    private fun mapLoginHttp(statusCode: Int, errorBody: String?): String {
        val serverMsg = extractServerMessage(errorBody)?.lowercase().orEmpty()
        return when (statusCode) {
            404 -> ACCOUNT_NOT_FOUND
            401 -> when {
                serverMsg.contains("не найден") || serverMsg.contains("not found") -> ACCOUNT_NOT_FOUND
                else -> WRONG_CREDENTIALS
            }
            in 500..599 -> SERVER_UNAVAILABLE
            else -> when {
                serverMsg.contains("не найден") || serverMsg.contains("not found") -> ACCOUNT_NOT_FOUND
                serverMsg.contains("неверные") || serverMsg.contains("учетн") ||
                    serverMsg.contains("учётн") || serverMsg.contains("credentials") -> WRONG_CREDENTIALS
                statusCode in 400..499 -> WRONG_CREDENTIALS
                else -> GENERIC
            }
        }
    }

    private fun mapRegisterHttp(statusCode: Int, errorBody: String?): String {
        val serverMsg = extractServerMessage(errorBody).orEmpty()
        return when {
            serverMsg.contains("почта уже", ignoreCase = true) ||
                serverMsg.contains("email", ignoreCase = true) && serverMsg.contains("зарегистр", ignoreCase = true) ->
                "Эта почта уже зарегистрирована"
            serverMsg.contains("логин", ignoreCase = true) && serverMsg.contains("существует", ignoreCase = true) ->
                "Пользователь с таким логином уже существует"
            statusCode in 500..599 -> SERVER_UNAVAILABLE
            serverMsg.isNotBlank() -> serverMsg
            else -> "Не удалось зарегистрироваться"
        }
    }

    private fun mapAuthLoginMessage(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("http:404") || lower.contains("404") && lower.contains("не найден") -> ACCOUNT_NOT_FOUND
            lower.contains("не найден") || lower.contains("not found") -> ACCOUNT_NOT_FOUND
            lower.contains("неверные") || lower.contains("учетн") || lower.contains("учётн") -> WRONG_CREDENTIALS
            isTimeoutMessage(lower) -> TIMEOUT
            isNetworkMessage(lower) -> NETWORK
            looksTechnical(raw) -> WRONG_CREDENTIALS
            raw.isNotBlank() && !looksTechnical(raw) -> raw
            else -> WRONG_CREDENTIALS
        }
    }

    private fun mapAuthRegisterMessage(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("почта уже") -> "Эта почта уже зарегистрирована"
            lower.contains("логин") && lower.contains("существует") -> "Пользователь с таким логином уже существует"
            isTimeoutMessage(lower) -> TIMEOUT
            isNetworkMessage(lower) -> NETWORK
            looksTechnical(raw) -> "Не удалось зарегистрироваться"
            raw.isNotBlank() && !looksTechnical(raw) -> raw
            else -> "Не удалось зарегистрироваться"
        }
    }

    private fun sanitizeRawMessage(raw: String): String? {
        if (raw.isBlank() || looksTechnical(raw)) return null
        return raw.take(200)
    }

    private fun extractServerMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = gson.fromJson(body, type) ?: return null
            (map["message"] as? String)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun isTimeout(throwable: Throwable): Boolean {
        var t: Throwable? = throwable
        while (t != null) {
            if (t is SocketTimeoutException) return true
            if (isTimeoutMessage(t.message.orEmpty().lowercase())) return true
            t = t.cause
        }
        return false
    }

    private fun isNetwork(throwable: Throwable): Boolean {
        var t: Throwable? = throwable
        while (t != null) {
            if (t is UnknownHostException || t is SSLException) return true
            if (isNetworkMessage(t.message.orEmpty().lowercase())) return true
            t = t.cause
        }
        return false
    }

    private fun isPlaybackTimeout(error: PlaybackException): Boolean {
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) return true
        return isTimeout(error)
    }

    private fun isTimeoutMessage(lower: String): Boolean =
        lower.contains("timeout") || lower.contains("timed out") || lower.contains("таймаут")

    private fun isNetworkMessage(lower: String): Boolean =
        lower.contains("unable to resolve host") ||
            lower.contains("failed to connect") ||
            lower.contains("network") ||
            lower.contains("internet") ||
            lower.contains("connection")

    private fun isTimeoutCode(code: Int): Boolean = code == 408 || code == 504

    private fun looksTechnical(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.contains("exception") ||
            lower.contains("http://") ||
            lower.contains("https://") ||
            lower.contains("retrofit") ||
            lower.contains("okhttp") ||
            lower.contains("json") ||
            lower.contains("source error") ||
            lower.contains("response code") ||
            lower.contains("androidx.") ||
            lower.contains("kotlin.") ||
            lower.contains("error code") ||
            lower.contains("playbackexception") ||
            lower.length > 180
    }

    private fun looksSessionExpired(raw: String): Boolean {
        val lower = raw.lowercase()
        // Мы ловим 403 даже если его “не пробросили” в status-код, а отдали как текст сообщения Exception.
        return lower.contains(" код ошибки 403") ||
            lower.contains("code: 403") ||
            lower.contains("code 403") ||
            lower.contains("\"status\":403") ||
            lower.contains("\"statusCode\":403") ||
            (lower.contains("403") && (lower.contains("forbidden") || lower.contains("access denied") || lower.contains("недостат")));
    }
}
