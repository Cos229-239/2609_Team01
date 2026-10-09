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
}