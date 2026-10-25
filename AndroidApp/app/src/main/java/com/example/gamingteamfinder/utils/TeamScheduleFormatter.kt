package com.example.gamingteamfinder.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object TeamScheduleFormatter {
    private val inputFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val outputFormat = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a z", Locale.US)
    private val dayMap = mapOf(
        "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    @JvmStatic
    fun format(
        startTime: String, endTime: String, days: List<String>,
        ownerTimeZone: String?, viewerTimeZone: String?
    ): String {
        val shortDays = days.map { day ->
            if (day.length >= 3) day.take(3) else day
        }

        val original = "$startTime - $endTime\n${shortDays.joinToString(", ")}"
        if (startTime.isBlank() || endTime.isBlank() || days.isEmpty()) {
            return "Schedule not configured"
        }
        if (ownerTimeZone.isNullOrBlank()) {
            return "$original\n\nCannot convert: team owner's time zone is missing."
        }
        val ownerZone = try { ZoneId.of(ownerTimeZone.trim()) } catch (_: Exception) {
            return "$original\n\nCannot convert: invalid team time zone '$ownerTimeZone'. Use an IANA ID such as America/New_York."
        }
        val viewerZone = try {
            if (viewerTimeZone.isNullOrBlank()) ZoneId.systemDefault()
            else ZoneId.of(viewerTimeZone.trim())
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
        return try {
            val start = LocalTime.parse(startTime.trim().uppercase(Locale.US), inputFormat)
            val end = LocalTime.parse(endTime.trim().uppercase(Locale.US), inputFormat)
            val today = LocalDate.now(ownerZone)
            val sessions = days.mapNotNull { name ->
                val day = dayMap[name.trim().lowercase(Locale.US)] ?: return@mapNotNull null
                val date = today.with(TemporalAdjusters.nextOrSame(day))
                val endDate = if (!end.isAfter(start)) date.plusDays(1) else date
                val localStart = ZonedDateTime.of(date, start, ownerZone).withZoneSameInstant(viewerZone)
                val localEnd = ZonedDateTime.of(endDate, end, ownerZone).withZoneSameInstant(viewerZone)
                localStart to localEnd
            }.sortedBy { it.first.toInstant() }
            if (sessions.isEmpty()) return "$original\n\nNo recognized schedule days"
            buildString {
                append("Your time zone: ${viewerZone.id}\n")
                append("Team time zone: ${ownerZone.id}\n\n")
                sessions.forEachIndexed { index, (localStart, localEnd) ->
                    if (index > 0) append("\n\n")
                    append(localStart.format(outputFormat))
                    append("\n→ ")
                    append(localEnd.format(outputFormat))
                }
            }
        } catch (e: Exception) {
            "$original\n\nCannot convert schedule: ${e.message ?: "invalid time format"}"
        }
    }
}
