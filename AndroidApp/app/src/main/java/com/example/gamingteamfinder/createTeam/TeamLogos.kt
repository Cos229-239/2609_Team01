package com.example.gamingteamfinder.createTeam

import com.example.gamingteamfinder.R

object TeamLogos {

    val logos = listOf(
        "team_logo_01",
        "team_logo_02",
        "team_logo_03",
        "team_logo_04",
        "team_logo_05",
        "team_logo_06",
        "team_logo_07",
        "team_logo_08"
    )

    fun getDrawableId(logoId: String?): Int {
        return when (logoId) {
            "team_logo_01" -> R.drawable.team_logo_01
            "team_logo_02" -> R.drawable.team_logo_02
            "team_logo_03" -> R.drawable.team_logo_03
            "team_logo_04" -> R.drawable.team_logo_04
            "team_logo_05" -> R.drawable.team_logo_05
            "team_logo_06" -> R.drawable.team_logo_06
            "team_logo_07" -> R.drawable.team_logo_07
            "team_logo_08" -> R.drawable.team_logo_08
            else -> android.R.drawable.ic_menu_gallery
        }
    }
}