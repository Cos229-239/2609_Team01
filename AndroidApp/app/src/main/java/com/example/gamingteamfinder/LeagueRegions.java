package com.example.gamingteamfinder;

public class LeagueRegions {

    public static final String[] LABELS = {
            "Select server / time zone",
            "NA (ET)", "NA (CT)", "NA (MT)",
            "NA (PT)", "NA (AT)", "NA (HT)",
            "EUW (UK)", "EUW (CET)",
            "EUNE (CET)", "EUNE (EET)",
            "BR (BRT)",
            "LAN (CT)", "LAN (COT)",
            "LAS (CLT)", "LAS (ART)",
            "KR (KST)", "JP (JST)",
            "OCE (AEST)", "OCE (ACST)", "OCE (AWST)",
            "TR (TRT)", "RU (MSK)",
            "PH (PHT)", "SG (SGT)",
            "TH (ICT)", "VN (ICT)", "TW (CST)",
            "ME (GST)"
    };

    public static final String[] TIME_ZONES = {
            "",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "America/Halifax",
            "Pacific/Honolulu",
            "Europe/London",
            "Europe/Berlin",
            "Europe/Warsaw",
            "Europe/Helsinki",
            "America/Sao_Paulo",
            "America/Mexico_City",
            "America/Bogota",
            "America/Santiago",
            "America/Argentina/Buenos_Aires",
            "Asia/Seoul",
            "Asia/Tokyo",
            "Australia/Sydney",
            "Australia/Adelaide",
            "Australia/Perth",
            "Europe/Istanbul",
            "Europe/Moscow",
            "Asia/Manila",
            "Asia/Singapore",
            "Asia/Bangkok",
            "Asia/Ho_Chi_Minh",
            "Asia/Taipei",
            "Asia/Dubai"
    };

    public static String getTimeZone(int position) {
        return TIME_ZONES[position];
    }

    public static int findPosition(String region) {
        if (region == null) return 0;

        for (int i = 0; i < LABELS.length; i++) {
            if (LABELS[i].equals(region)) return i;
        }

        return 0;
    }

    public static String getServer(int position) {
        if (position <= 0 || position >= LABELS.length) {
            return "";
        }

        return LABELS[position].split(" ")[0];
    }
}