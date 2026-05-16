package com.silpi.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ChatTimeHelper {
    private const val KOREA_TIME_ZONE = "Asia/Seoul"

    fun readMillis(document: DocumentSnapshot, fieldName: String): Long? {
        return when (val value = document.get(fieldName)) {
            is Timestamp -> value.toDate().time
            is Date -> value.time
            is Number -> value.toLong()
            else -> null
        }
    }

    fun formatChatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val formatter = SimpleDateFormat("a h:mm", Locale.KOREAN)
        formatter.timeZone = TimeZone.getTimeZone(KOREA_TIME_ZONE)
        return formatter.format(Date(timestamp))
    }
}
