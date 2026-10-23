package com.gpstrigger.ui

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** The days of the week in the order used by [locale] (Monday first in Italy, Sunday first in the US). */
fun weekDays(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}

fun DayOfWeek.shortName(locale: Locale): String =
    getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }
