package com.example.gamingteamfinder.utils

import android.content.Context
import com.example.gamingteamfinder.R

object TeamLogoHelper {

    @JvmStatic
    fun getDrawableId(context: Context, logoId: String?): Int {
        val validIds = (1..8).map {
            "team_logo_%02d".format(java.util.Locale.US, it)
        }

        if (logoId !in validIds) {
            return R.drawable.team_logo_01
        }

        val resourceId = context.resources.getIdentifier(
            logoId,
            "drawable",
            context.packageName
        )

        return if (resourceId != 0) {
            resourceId
        } else {
            R.drawable.team_logo_01
        }
    }
}