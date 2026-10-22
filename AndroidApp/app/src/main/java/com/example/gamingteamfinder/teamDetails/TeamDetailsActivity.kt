
package com.example.gamingteamfinder.teamDetails

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.example.gamingteamfinder.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.bumptech.glide.Glide
import com.example.gamingteamfinder.utils.TeamLogoHelper
import com.example.gamingteamfinder.utils.TeamScheduleFormatter
import android.content.Intent
import android.view.View
import com.example.gamingteamfinder.createTeam.EditTeamActivity


class TeamDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teamListener: ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_details)

        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        val teamId = intent.getStringExtra("TEAM_ID")

        if (teamId.isNullOrBlank()) {
            Toast.makeText(this, "Team ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadTeam(teamId)
        loadMembers(teamId)
    }

    private fun loadTeam(teamId: String) {
        teamListener = db.collection("teams")
            .document(teamId)
            .addSnapshotListener { document, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Failed to load team: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {
                    Toast.makeText(this, "Team not found", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val name = document.getString("teamName") ?: "Unnamed Team"
                val game = document.getString("game") ?: "Unknown"
                val mode = document.getString("gameMode") ?: "Unknown"
                val minRank = document.getString("minRank") ?: ""
                val maxRank = document.getString("maxRank") ?: ""
                val about = document.getString("about").orEmpty()
                val requirements = document.getString("requirements").orEmpty()
                val startTime = document.getString("startTime").orEmpty()
                val endTime = document.getString("endTime").orEmpty()
                val days = (document.get("days") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                val ownerTimeZone = document.getString("ownerTimeZone")
                val count = document.getLong("currentPlayers") ?: 1L
                val max = document.getLong("maxPlayers") ?: 5L
                val ownerId = document.getString("ownerId")
                val logoId = document.getString("logoId")

                findViewById<TextView>(R.id.textTeamName).text = name
                findViewById<TextView>(R.id.textMemberCount).text =
                    "$count/$max Members - $mode"
                findViewById<TextView>(R.id.textGame).text = "Game: $game"
                findViewById<TextView>(R.id.textRanks).text =
                    "Ranks: $minRank - $maxRank"
                findViewById<TextView>(R.id.textAbout).text =
                    about.ifBlank { "No description provided" }
                findViewById<TextView>(R.id.textRequirements).text =
                    requirements.ifBlank { "No additional requirements" }
                val playTimeView = findViewById<TextView>(R.id.textPlayTime)
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                if (currentUserId == null) {
                    playTimeView.text =
                        "$startTime - $endTime\n${days.joinToString(", ")}"
                } else {
                    db.collection("users")
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { userDocument ->

                            val viewerTimeZone = userDocument.getString("timeZone")

                            playTimeView.text = TeamScheduleFormatter.format(
                                startTime,
                                endTime,
                                days,
                                ownerTimeZone,
                                viewerTimeZone
                            )
                        }
                        .addOnFailureListener {
                            playTimeView.text =
                                "$startTime - $endTime\n${days.joinToString(", ")}\nUnable to load your time zone"
                        }
                }

                // Show Edit Team only to the team owner
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                val joinButton = findViewById<Button>(R.id.buttonRequestJoin)
                val editButton = findViewById<Button>(R.id.buttonEditTeam)

                if (currentUid != null && currentUid == ownerId) {

                    // Owner can edit, but doesn't need to request to join
                    editButton.visibility = View.VISIBLE
                    joinButton.visibility = View.GONE

                    editButton.setOnClickListener {
                        val intent = Intent(
                            this,
                            EditTeamActivity::class.java
                        )

                        intent.putExtra("TEAM_ID", teamId)
                        startActivity(intent)
                    }

                } else {

                    // Other players cannot edit
                    editButton.visibility = View.GONE

                    joinButton.visibility = View.VISIBLE
                    joinButton.isEnabled = false
                    joinButton.text = "Join Requests Coming Soon"
                }

                // Logo loading is added in Step 4
                showTeamLogo(logoId)
            }
    }

    private fun loadMembers(teamId: String) {
        membersListener = db.collection("teams")
            .document(teamId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Failed to load members: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val container = findViewById<LinearLayout>(R.id.layoutMembers)
                container.removeAllViews()

                snapshot?.documents?.forEach { member ->
                    val userId = member.getString("userId") ?: return@forEach
                    val role = member.getString("role") ?: "member"

                    val row = TextView(this).apply {
                        text = "Loading member... ($role)"
                        textSize = 16f
                        setTextColor(Color.DKGRAY)
                        setPadding(8, 12, 8, 12)
                    }
                    container.addView(row)

                    // Look up the member's display name
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val displayName =
                                userDoc.getString("displayName")
                                    ?: userDoc.getString("username")
                                    ?: "Player"
                            row.text = "$displayName ($role)"
                        }
                        .addOnFailureListener {
                            row.text = "Player ($role)"
                        }
                }
            }
    }

    private fun showTeamLogo(logoId: String?) {
        val imageView = findViewById<ImageView>(R.id.imageTeamLogo)

        imageView.setImageResource(
            TeamLogoHelper.getDrawableId(this, logoId)
        )
    }

    override fun onDestroy() {
        teamListener?.remove()
        membersListener?.remove()
        super.onDestroy()
    }
}
