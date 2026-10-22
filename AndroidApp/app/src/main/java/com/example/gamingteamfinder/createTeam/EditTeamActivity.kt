package com.example.gamingteamfinder.createTeam

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.gamingteamfinder.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

class EditTeamActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var teamId: String
    private var selectedLogoId = "team_logo_01"
    private lateinit var leagueRanksLayout: GridLayout
    private lateinit var valorantRanksLayout: GridLayout
    private lateinit var radioLeague: RadioButton
    private lateinit var radioValorant: RadioButton
    private val selectedRanks = mutableListOf<View>()
    private val maxTeamSize = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_team)
        teamId = intent.getStringExtra("TEAM_ID").orEmpty()
        if (teamId.isBlank()) {
            Toast.makeText(this, "Team ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        findViewById<Button>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonChooseLogo).setOnClickListener { showLogoPicker() }
        leagueRanksLayout = findViewById(R.id.leagueRanksLayout)
        valorantRanksLayout = findViewById(R.id.valorantRanksLayout)
        radioLeague = findViewById(R.id.radioLeague)
        radioValorant = findViewById(R.id.radioValorant)
        radioLeague.setOnClickListener { changeGame(true) }
        radioValorant.setOnClickListener { changeGame(false) }
        for (grid in listOf(leagueRanksLayout, valorantRanksLayout)) {
            for (i in 0 until grid.childCount) {
                val view = grid.getChildAt(i)
                if (view is Button || view is ImageButton) {
                    view.setOnClickListener { selectRank(view) }
                }
            }
        }
        findViewById<Button>(R.id.buttonStartTime).setOnClickListener { showTimePicker(it as Button) }
        findViewById<Button>(R.id.buttonEndTime).setOnClickListener { showTimePicker(it as Button) }
        findViewById<Button>(R.id.buttonCreateTeam).apply {
            text = "Save Changes"
            isEnabled = false
            setOnClickListener { saveChanges() }
        }
        loadTeam()
    }

    private fun changeGame(league: Boolean) {
        clearRanks()
        leagueRanksLayout.visibility = if (league) View.VISIBLE else View.GONE
        valorantRanksLayout.visibility = if (league) View.GONE else View.VISIBLE
    }

    private fun clearRanks() {
        selectedRanks.forEach {
            it.isSelected = false
            it.setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        selectedRanks.clear()
    }

    private fun selectRank(view: View) {
        if (selectedRanks.contains(view)) {
            selectedRanks.remove(view)
            view.isSelected = false
            view.setBackgroundColor(Color.parseColor("#E0E0E0"))
        } else {
            if (selectedRanks.size >= 2) {
                Toast.makeText(this, "Select only 2 ranks", Toast.LENGTH_SHORT).show()
                return
            }
            selectedRanks.add(view)
            view.isSelected = true
            view.setBackgroundColor(Color.parseColor("#6C4FB3"))
        }
    }

    private fun selectSavedRank(grid: GridLayout, rank: String) {
        for (i in 0 until grid.childCount) {
            val view = grid.getChildAt(i)
            if (view.tag?.toString() == rank && !selectedRanks.contains(view)) {
                selectRank(view)
                return
            }
        }
    }

    private fun loadTeam() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        db.collection("teams").document(teamId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists() || doc.getString("ownerId") != uid) {
                    Toast.makeText(this, "Only the team owner can edit", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }
                findViewById<EditText>(R.id.editTeamName).setText(doc.getString("teamName").orEmpty())
                selectedLogoId = doc.getString("logoId") ?: "team_logo_01"
                updateLogoPreview()
                val isLeague = doc.getString("game") != "Valorant"
                if (isLeague) radioLeague.isChecked = true else radioValorant.isChecked = true
                changeGame(isLeague)
                val grid = if (isLeague) leagueRanksLayout else valorantRanksLayout
                selectSavedRank(grid, doc.getString("minRank").orEmpty())
                selectSavedRank(grid, doc.getString("maxRank").orEmpty())
                findViewById<RadioButton>(R.id.radioRanked).isChecked = doc.getString("gameMode") == "Ranked"
                findViewById<RadioButton>(R.id.radioCasual).isChecked = doc.getString("gameMode") == "Casual"
                findViewById<Button>(R.id.buttonStartTime).text = doc.getString("startTime") ?: "Start Time"
                findViewById<Button>(R.id.buttonEndTime).text = doc.getString("endTime") ?: "End Time"
                val days = (doc.get("days") as? List<*>)?.filterIsInstance<String>().orEmpty()
                dayIds().forEach { (id, name) -> findViewById<CheckBox>(id).isChecked = name in days }
                val voice = doc.getBoolean("voiceChat")
                findViewById<RadioButton>(R.id.radioVoiceYes).isChecked = voice == true
                findViewById<RadioButton>(R.id.radioVoiceNo).isChecked = voice == false
                findViewById<EditText>(R.id.editAbout).setText(doc.getString("about").orEmpty())
                findViewById<EditText>(R.id.editRequirements).setText(doc.getString("requirements").orEmpty())
                findViewById<Button>(R.id.buttonCreateTeam).isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to load team: ${it.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun updateLogoPreview() {
        val resourceId = resources.getIdentifier(selectedLogoId, "drawable", packageName)
        findViewById<ImageView>(R.id.imageTeamLogo).setImageResource(
            if (resourceId != 0) resourceId else R.drawable.team_logo_01
        )
    }

    private fun showLogoPicker() {
        val grid = GridLayout(this).apply { columnCount = 4; setPadding(24, 24, 24, 24) }
        val dialog = AlertDialog.Builder(this).setTitle("Choose a Team Logo")
            .setView(grid).setNegativeButton("Cancel", null).create()
        for (i in 1..8) {
            val logoId = "team_logo_%02d".format(Locale.US, i)
            val resourceId = resources.getIdentifier(logoId, "drawable", packageName)
            if (resourceId == 0) continue
            val image = ImageView(this).apply {
                setImageResource(resourceId)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(8, 8, 8, 8)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = (72 * resources.displayMetrics.density).toInt()
                    height = (72 * resources.displayMetrics.density).toInt()
                }
                setOnClickListener {
                    selectedLogoId = logoId
                    updateLogoPreview()
                    dialog.dismiss()
                }
            }
            grid.addView(image)
        }
        if (grid.childCount == 0) {
            Toast.makeText(this, "No team logos found", Toast.LENGTH_SHORT).show()
            return
        }
        dialog.show()
    }

    private fun showTimePicker(button: Button) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            button.text = String.format(Locale.US, "%d:%02d %s", hour12, minute,
                if (hour >= 12) "PM" else "AM")
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    private fun dayIds() = listOf(
        R.id.checkMonday to "Monday", R.id.checkTuesday to "Tuesday",
        R.id.checkWednesday to "Wednesday", R.id.checkThursday to "Thursday",
        R.id.checkFriday to "Friday", R.id.checkSaturday to "Saturday",
        R.id.checkSunday to "Sunday"
    )

    private fun saveChanges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val teamName = findViewById<EditText>(R.id.editTeamName).text.toString().trim()
        if (teamName.isBlank()) {
            Toast.makeText(this, "Enter a team name", Toast.LENGTH_SHORT).show()
            return
        }
        val game = when {
            radioLeague.isChecked -> "League of Legends"
            radioValorant.isChecked -> "Valorant"
            else -> {
                Toast.makeText(this, "Select a game", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (selectedRanks.size != 2) {
            Toast.makeText(this, "Select exactly 2 ranks", Toast.LENGTH_SHORT).show()
            return
        }
        val rankOrder = if (game == "League of Legends")
            listOf("Iron", "Bronze", "Silver", "Gold", "Platinum", "Emerald", "Diamond", "Master", "Grandmaster", "Challenger")
        else listOf("Iron", "Bronze", "Silver", "Gold", "Platinum", "Diamond", "Ascendant", "Immortal", "Radiant")
        val ranks = selectedRanks.map { it.tag.toString() }.sortedBy { rankOrder.indexOf(it) }
        if (ranks.any { it !in rankOrder }) {
            Toast.makeText(this, "Invalid ranks", Toast.LENGTH_SHORT).show()
            return
        }
        val mode = when {
            findViewById<RadioButton>(R.id.radioRanked).isChecked -> "Ranked"
            findViewById<RadioButton>(R.id.radioCasual).isChecked -> "Casual"
            else -> {
                Toast.makeText(this, "Select game mode", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val startTime = findViewById<Button>(R.id.buttonStartTime).text.toString()
        val endTime = findViewById<Button>(R.id.buttonEndTime).text.toString()
        if (startTime == "Start Time" || endTime == "End Time") {
            Toast.makeText(this, "Select play times", Toast.LENGTH_SHORT).show()
            return
        }
        val days = dayIds().filter { findViewById<CheckBox>(it.first).isChecked }.map { it.second }
        if (days.isEmpty()) {
            Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show()
            return
        }
        if (findViewById<RadioGroup>(R.id.voiceRadioGroup).checkedRadioButtonId == -1) {
            Toast.makeText(this, "Select voice chat preference", Toast.LENGTH_SHORT).show()
            return
        }
        val voiceChat = findViewById<RadioButton>(R.id.radioVoiceYes).isChecked
        val about = findViewById<EditText>(R.id.editAbout).text.toString().trim()
        val requirements = findViewById<EditText>(R.id.editRequirements).text.toString().trim()
        val saveButton = findViewById<Button>(R.id.buttonCreateTeam)
        saveButton.isEnabled = false
        val teamRef = db.collection("teams").document(teamId)
        // Recheck ownership before writing; Firestore security rules must enforce it too.
        teamRef.get().addOnSuccessListener { doc ->
            if (!doc.exists() || doc.getString("ownerId") != uid) {
                saveButton.isEnabled = true
                Toast.makeText(this, "Only the owner can edit", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val fields = hashMapOf<String, Any>(
                "teamName" to teamName, "logoId" to selectedLogoId,
                "game" to game, "minRank" to ranks[0], "maxRank" to ranks[1],
                "gameMode" to mode, "startTime" to startTime, "endTime" to endTime,
                "days" to days, "voiceChat" to voiceChat,
                "about" to about, "requirements" to requirements
            )
            teamRef.update(fields).addOnSuccessListener {
                Toast.makeText(this, "Team updated", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                saveButton.isEnabled = true
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            saveButton.isEnabled = true
            Toast.makeText(this, "Unable to verify ownership: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
