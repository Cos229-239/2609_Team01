package com.example.gamingteamfinder.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TeamTimeConverter {

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern(
                    "h:mm a",
                    Locale.US
            );

    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern(
                    "EEE, MMM d · h:mm a z",
                    Locale.US
            );

    public static ZonedDateTime convert(
            String time,
            LocalDate ownerDate,
            String ownerTimeZone,
            String viewerTimeZone
    ) {

        LocalTime localTime = LocalTime.parse(
                time,
                INPUT_FORMAT
        );

        ZonedDateTime ownerDateTime =
                ZonedDateTime.of(
                        ownerDate,
                        localTime,
                        ZoneId.of(ownerTimeZone)
                );

        return ownerDateTime.withZoneSameInstant(
                ZoneId.of(viewerTimeZone)
        );
    }

    public static String format(ZonedDateTime dateTime) {
        return dateTime.format(OUTPUT_FORMAT);
    }
}