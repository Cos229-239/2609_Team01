
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


class TeamDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teamListener: ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_details)

        findViewById<TextView>(R.id.textBack).setOnClickListener {
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
                val days = document.get("days") as? List<*> ?: emptyList<Any>()
                val count = document.getLong("currentPlayers") ?: 1L
                val max = document.getLong("maxPlayers") ?: 5L
                val ownerId = document.getString("ownerId")
                val logoUrl = document.getString("logoUrl").orEmpty()

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
                findViewById<TextView>(R.id.textPlayTime).text =
                    "$startTime - $endTime\n${days.joinToString(", ")}"

                // The owner is already a member
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                val joinButton = findViewById<Button>(R.id.buttonRequestJoin)

                if (currentUid == ownerId) {
                    joinButton.visibility = android.view.View.GONE
                } else {
                    joinButton.visibility = android.view.View.VISIBLE
                    joinButton.isEnabled = false
                    joinButton.text = "Join Requests Coming Soon"
                }

                // Logo loading is added in Step 4
                showTeamLogo(logoUrl)
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

    private fun showTeamLogo(url: String) {
        val imageView = findViewById<ImageView>(R.id.imageTeamLogo)

        if (url.isBlank()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }

        Glide.with(this)
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(imageView)
    }

    override fun onDestroy() {
        teamListener?.remove()
        membersListener?.remove()
        super.onDestroy()
    }
}
