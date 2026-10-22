package com.example.gamingteamfinder.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.temporal.TemporalAdjusters

object TeamScheduleFormatter {

    private val inputFormat =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    private val outputFormat =
        DateTimeFormatter.ofPattern(
            "EEE, MMM d · h:mm a z",
            Locale.US
        )

    @JvmStatic
    fun format(
        startTime: String,
        endTime: String,
        days: List<String>,
        ownerTimeZone: String?,
        viewerTimeZone: String?
    ): String {

        val original = "$startTime - $endTime\n${days.joinToString(", ")}"

        if (ownerTimeZone.isNullOrBlank()) {
            return "$original\nTime zone not specified"
        }

        if (viewerTimeZone.isNullOrBlank()) {
            return "$original\nOwner time zone: $ownerTimeZone\nSet your profile time zone to see converted times"
        }

        return try {
            val ownerZone = ZoneId.of(ownerTimeZone)
            val viewerZone = ZoneId.of(viewerTimeZone)

            val start = LocalTime.parse(startTime, inputFormat)
            val end = LocalTime.parse(endTime, inputFormat)

            val dayMap = mapOf(
                "Monday" to DayOfWeek.MONDAY,
                "Tuesday" to DayOfWeek.TUESDAY,
                "Wednesday" to DayOfWeek.WEDNESDAY,
                "Thursday" to DayOfWeek.THURSDAY,
                "Friday" to DayOfWeek.FRIDAY,
                "Saturday" to DayOfWeek.SATURDAY,
                "Sunday" to DayOfWeek.SUNDAY
            )

            val today = LocalDate.now(ownerZone)

            val sessions = days.mapNotNull { dayName ->
                val dayOfWeek = dayMap[dayName] ?: return@mapNotNull null

                val sessionDate = today.with(
                    TemporalAdjusters.nextOrSame(dayOfWeek)
                )

                val endDate = if (end <= start) {
                    sessionDate.plusDays(1)
                } else {
                    sessionDate
                }

                val ownerStart = ZonedDateTime.of(
                    sessionDate,
                    start,
                    ownerZone
                )

                val ownerEnd = ZonedDateTime.of(
                    endDate,
                    end,
                    ownerZone
                )

                val localStart = ownerStart.withZoneSameInstant(viewerZone)
                val localEnd = ownerEnd.withZoneSameInstant(viewerZone)

                Pair(localStart, localEnd)
            }.sortedBy { it.first.toInstant() }

            if (sessions.isEmpty()) {
                "$original\nNo valid schedule days"
            } else {
                buildString {
                    append("Your selected time zone: ")
                    append(viewerTimeZone)
                    append("\n\nUpcoming sessions:\n")

                    sessions.forEachIndexed { index, session ->
                        if (index > 0) append("\n\n")

                        append(session.first.format(outputFormat))
                        append("\nto ")
                        append(session.second.format(outputFormat))
                    }

                    append("\n\nTeam time zone: ")
                    append(ownerTimeZone)
                }
            }

        } catch (e: Exception) {
            "$original\nUnable to convert time zone"
        }
    }
}