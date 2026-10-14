package com.example.gamingteamfinder.createTeam

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gamingteamfinder.R
import android.app.TimePickerDialog
import android.widget.TimePicker
import java.util.Calendar
import android.widget.ImageButton
import android.graphics.Color
import android.widget.EditText
import android.widget.CheckBox
import android.widget.RadioGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage

class CreateTeamActivity : AppCompatActivity() {

    private val maxTeamSize = 5

    private lateinit var leagueRanksLayout: GridLayout
    private lateinit var valorantRanksLayout: GridLayout

    private lateinit var radioLeague: RadioButton
    private lateinit var radioValorant: RadioButton


    // Stores the rank buttons currently selected
    private val selectedRanks = mutableListOf<ImageButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        val buttonChooseLogo = findViewById<Button>(
            R.id.buttonChooseLogo
        )

        buttonChooseLogo.setOnClickListener {
            pickTeamLogo.launch("image/*")
        }

        leagueRanksLayout = findViewById(R.id.leagueRanksLayout)
        valorantRanksLayout = findViewById(R.id.valorantRanksLayout)

        radioLeague = findViewById(R.id.radioLeague)
        radioValorant = findViewById(R.id.radioValorant)

        setupGameButtons()
        setupRankButtons()

        val buttonStartTime = findViewById<Button>(R.id.buttonStartTime)
        val buttonEndTime = findViewById<Button>(R.id.buttonEndTime)

        buttonStartTime.setOnClickListener {
            showTimePicker(buttonStartTime)
        }

        buttonEndTime.setOnClickListener {
            showTimePicker(buttonEndTime)
        }

        // Create Team button
        val buttonCreateTeam = findViewById<Button>(
            R.id.buttonCreateTeam
        )

        buttonCreateTeam.setOnClickListener {
            saveTeamToFirestore()
        }
    }

    private fun uploadTeamLogo(
        teamId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val logoUri = selectedLogoUri

        // No logo selected: save an empty URL
        if (logoUri == null) {
            onSuccess("")
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("team_logos/$teamId/logo")

        storageRef.putFile(logoUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception
                        ?: Exception("Logo upload failed")
                }

                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    private fun showTimePicker(button: Button) {

        val calendar = Calendar.getInstance()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->

                val hour12 = when {
                    selectedHour % 12 == 0 -> 12
                    else -> selectedHour % 12
                }

                val amPm = if (selectedHour >= 12) "PM" else "AM"

                val formattedTime = String.format(
                    java.util.Locale.US,
                    "%d:%02d %s",
                    hour12,
                    selectedMinute,
                    amPm
                )

                button.text = formattedTime
            },
            hour,
            minute,
            false
        )

        timePickerDialog.show()
    }

    private fun setupGameButtons() {

        radioLeague.setOnClickListener {

            clearRankSelection()

            leagueRanksLayout.visibility = View.VISIBLE
            valorantRanksLayout.visibility = View.GONE
        }

        radioValorant.setOnClickListener {

            clearRankSelection()

            valorantRanksLayout.visibility = View.VISIBLE
            leagueRanksLayout.visibility = View.GONE
        }
    }

    private fun setupRankButtons() {

        setupButtonsInsideLayout(leagueRanksLayout)
        setupButtonsInsideLayout(valorantRanksLayout)
    }

    private fun setupButtonsInsideLayout(layout: GridLayout) {

        for (i in 0 until layout.childCount) {

            val view = layout.getChildAt(i)

            if (view is ImageButton) {

                view.setOnClickListener {
                    selectRank(view)
                }
            }
        }
    }

    private var selectedLogoUri: Uri? = null

    private val pickTeamLogo = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {
            selectedLogoUri = uri

            findViewById<ImageView>(
                R.id.imageTeamLogo
            ).setImageURI(uri)
        }
    }
    private fun selectRank(button: ImageButton) {

        // If already selected, deselect it
        if (selectedRanks.contains(button)) {

            selectedRanks.remove(button)
            button.isSelected = false

            // Return to original gray color
            button.setBackgroundColor(
                Color.parseColor("#E0E0E0")
            )

            return
        }

        // Prevent selecting more than 2 ranks
        if (selectedRanks.size >= 2) {

            Toast.makeText(
                this,
                "You can only select 2 ranks",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Select rank
        selectedRanks.add(button)
        button.isSelected = true

        // Change background to purple
        button.setBackgroundColor(
            Color.parseColor("#6C4FB3")
        )
    }

    private fun clearRankSelection() {

        for (button in selectedRanks) {

            button.isSelected = false

            button.setBackgroundColor(
                Color.parseColor("#E0E0E0")
            )
        }

        selectedRanks.clear()

    }


    private fun saveTeamToFirestore() {

        // 1. Check that the user is signed in
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Get team name
        val teamName = findViewById<EditText>(
            R.id.editTeamName
        ).text.toString().trim()

        if (teamName.isBlank()) {
            Toast.makeText(this, "Enter a team name", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Get selected game
        val game = when {
            radioLeague.isChecked -> "League of Legends"
            radioValorant.isChecked -> "Valorant"
            else -> {
                Toast.makeText(this, "Select a game", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 4. Get selected ranks
        if (selectedRanks.size != 2) {
            Toast.makeText(this, "Select exactly 2 ranks", Toast.LENGTH_SHORT).show()
            return
        }

        val rankOrder = if (game == "League of Legends") {
            listOf(
                "Iron", "Bronze", "Silver", "Gold",
                "Platinum", "Emerald", "Diamond",
                "Master", "Grandmaster", "Challenger"
            )
        } else {
            listOf(
                "Iron", "Bronze", "Silver", "Gold",
                "Platinum", "Diamond", "Ascendant",
                "Immortal", "Radiant"
            )
        }

        val ranks = selectedRanks.map {
            it.tag.toString()
        }.sortedBy {
            rankOrder.indexOf(it)
        }

        if (ranks.any { it !in rankOrder }) {
            Toast.makeText(this, "Invalid rank selection", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Get game mode
        val gameMode = when {
            findViewById<RadioButton>(R.id.radioRanked).isChecked -> "Ranked"
            findViewById<RadioButton>(R.id.radioCasual).isChecked -> "Casual"
            else -> {
                Toast.makeText(this, "Select a game mode", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 6. Get play times
        val startTime = findViewById<Button>(
            R.id.buttonStartTime
        ).text.toString()

        val endTime = findViewById<Button>(
            R.id.buttonEndTime
        ).text.toString()

        if (startTime == "Start Time" || endTime == "End Time") {
            Toast.makeText(this, "Select play times", Toast.LENGTH_SHORT).show()
            return
        }

        // 7. Get selected days
        val dayOptions = listOf(
            R.id.checkMonday to "Monday",
            R.id.checkTuesday to "Tuesday",
            R.id.checkWednesday to "Wednesday",
            R.id.checkThursday to "Thursday",
            R.id.checkFriday to "Friday",
            R.id.checkSaturday to "Saturday",
            R.id.checkSunday to "Sunday"
        )

        val selectedDays = dayOptions.filter {
            findViewById<CheckBox>(it.first).isChecked
        }.map {
            it.second
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        // 8. Get voice chat preference
        val voiceGroup = findViewById<RadioGroup>(
            R.id.voiceRadioGroup
        )

        if (voiceGroup.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Select voice chat preference", Toast.LENGTH_SHORT).show()
            return
        }

        val voiceChat = findViewById<RadioButton>(
            R.id.radioVoiceYes
        ).isChecked

        // 9. Get optional descriptions
        val about = findViewById<EditText>(
            R.id.editAbout
        ).text.toString().trim()

        val requirements = findViewById<EditText>(
            R.id.editRequirements
        ).text.toString().trim()

        // 10. Prepare Firestore
        val db = FirebaseFirestore.getInstance()
        val teamRef = db.collection("teams").document()

        val teamData = hashMapOf<String, Any>(
            "teamId" to teamRef.id,
            "teamName" to teamName,
            "logoUrl" to "",
            "ownerId" to user.uid,
            "game" to game,
            "minRank" to ranks[0],
            "maxRank" to ranks[1],
            "gameMode" to gameMode,
            "maxPlayers" to maxTeamSize,
            "currentPlayers" to 1,
            "memberIds" to listOf(user.uid),
            "startTime" to startTime,
            "endTime" to endTime,
            "days" to selectedDays,
            "voiceChat" to voiceChat,
            "about" to about,
            "requirements" to requirements,
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp()
        )

        // 11. Disable button while saving
        val createButton = findViewById<Button>(
            R.id.buttonCreateTeam
        )

        createButton.isEnabled = false

        // 12. Save team and creator together
        val memberRef = teamRef.collection("members")
            .document(user.uid)

        val memberData = hashMapOf<String, Any>(
            "userId" to user.uid,
            "role" to "owner",
            "joinedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(teamRef, teamData)
        batch.set(memberRef, memberData)

        uploadTeamLogo(
            teamRef.id,
            onSuccess = { logoUrl ->

                // Add the uploaded logo URL to team data
                teamData["logoUrl"] = logoUrl

                // Save team and owner membership to Firestore
                batch.commit()
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Team created successfully!",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                    .addOnFailureListener { exception ->

                        createButton.isEnabled = true

                        Toast.makeText(
                            this,
                            "Failed to save team: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            onFailure = { exception ->

                createButton.isEnabled = true

                Toast.makeText(
                    this,
                    "Logo upload failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

}