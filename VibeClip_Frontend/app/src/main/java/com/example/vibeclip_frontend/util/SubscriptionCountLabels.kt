package com.example.vibeclip_frontend.util

fun subscriptionsCountLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "подписок"
        mod10 == 1 -> "подписка"
        mod10 in 2..4 -> "подписки"
        else -> "подписок"
    }
}

fun subscribersCountLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "подписчиков"
        mod10 == 1 -> "подписчик"
        mod10 in 2..4 -> "подписчика"
        else -> "подписчиков"
    }
}

fun pendingRequestsLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "заявок"
        mod10 == 1 -> "заявка"
        mod10 in 2..4 -> "заявки"
        else -> "заявок"
    }
}
