package com.silpi.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun formatChatListTime(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val timeZone = TimeZone.getTimeZone(KOREA_TIME_ZONE)
        val now = Calendar.getInstance(timeZone)
        val messageDate = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
        }

        if (isSameDate(now, messageDate)) {
            return formatChatTime(timestamp)
        }

        val yesterday = Calendar.getInstance(timeZone).apply {
            add(Calendar.DATE, -1)
        }
        if (isSameDate(yesterday, messageDate)) {
            return "어제"
        }

        val formatter = SimpleDateFormat("M월 d일", Locale.KOREAN)
        formatter.timeZone = timeZone
        return formatter.format(Date(timestamp))
    }

    fun formatDateDivider(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val formatter = SimpleDateFormat("yyyy년 M월 d일 E요일 >", Locale.KOREAN)
        formatter.timeZone = TimeZone.getTimeZone(KOREA_TIME_ZONE)
        return formatter.format(Date(timestamp))
    }

    fun dateKey(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val formatter = SimpleDateFormat("yyyyMMdd", Locale.KOREAN)
        formatter.timeZone = TimeZone.getTimeZone(KOREA_TIME_ZONE)
        return formatter.format(Date(timestamp))
    }

    private fun isSameDate(first: Calendar, second: Calendar): Boolean {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
