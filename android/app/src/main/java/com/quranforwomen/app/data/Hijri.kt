package com.quranforwomen.app.data

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import android.icu.util.Calendar as IcuCalendar

/** A day in the Islamic (Hijri) calendar. Month is 1 to 12. */
data class HijriDay(val year: Int, val month: Int, val day: Int)

/**
 * The Umm al-Qura Islamic calendar, worked out on the phone (same tables as the iPhone app).
 * A Hijri month is identified by an index: year * 12 + month - 1.
 */
object Hijri {
    const val RAMADAN = 9

    val monthNames = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul Qa'dah", "Dhul Hijjah",
    )

    private fun calendar(): IslamicCalendar = IslamicCalendar(TimeZone.GMT_ZONE, ULocale.US).apply {
        calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
    }

    fun of(date: LocalDate): HijriDay {
        val c = calendar()
        c.timeInMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return HijriDay(c.get(IcuCalendar.YEAR), c.get(IcuCalendar.MONTH) + 1, c.get(IcuCalendar.DAY_OF_MONTH))
    }

    /** The Gregorian date of the first day of a Hijri month. */
    fun start(year: Int, month: Int): LocalDate {
        val c = calendar()
        c.clear()
        c.set(year, month - 1, 1, 12, 0, 0)
        return Instant.ofEpochMilli(c.timeInMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    fun index(year: Int, month: Int): Int = year * 12 + month - 1
    fun yearOf(index: Int): Int = index / 12
    fun monthOf(index: Int): Int = index % 12 + 1

    fun start(index: Int): LocalDate = start(yearOf(index), monthOf(index))

    /** 29 or 30. */
    fun length(index: Int): Int = ChronoUnit.DAYS.between(start(index), start(index + 1)).toInt().coerceIn(29, 30)

    fun monthName(month: Int): String = monthNames[(month - 1).coerceIn(0, 11)]

    fun isRamadan(date: LocalDate): Boolean = of(date).month == RAMADAN

    /** The widely known days, and nothing more. */
    fun specialDay(month: Int, day: Int): String? = when {
        month == 1 && day == 1 -> "Islamic New Year"
        month == 1 && day == 10 -> "Ashura"
        month == 9 && day == 1 -> "First day of Ramadan"
        month == 9 && day >= 21 -> "Last ten nights"
        month == 10 && day == 1 -> "Eid al-Fitr"
        month == 12 && day == 9 -> "Day of Arafah"
        month == 12 && day == 10 -> "Eid al-Adha"
        else -> null
    }
}
