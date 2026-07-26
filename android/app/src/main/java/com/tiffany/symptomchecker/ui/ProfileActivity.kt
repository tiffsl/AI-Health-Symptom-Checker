package com.tiffany.symptomchecker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tiffany.symptomchecker.R
import com.tiffany.symptomchecker.data.ProfileStore
import com.tiffany.symptomchecker.data.UserProfile

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.title = "Health Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etName = findViewById<EditText>(R.id.etProfileName)
        val etAge = findViewById<EditText>(R.id.etProfileAge)
        val spSex = findViewById<Spinner>(R.id.spProfileSex)
        val spBlood = findViewById<Spinner>(R.id.spBloodType)
        val etConditions = findViewById<EditText>(R.id.etConditions)
        val etAllergies = findViewById<EditText>(R.id.etAllergies)
        val etEmergencyName = findViewById<EditText>(R.id.etEmergencyName)
        val etEmergencyPhone = findViewById<EditText>(R.id.etEmergencyPhone)
        val etHospital = findViewById<EditText>(R.id.etPreferredHospital)
        val etHospitalPhone = findViewById<EditText>(R.id.etPreferredHospitalPhone)

        val sexOptions = listOf("Prefer not to say", "Female", "Male", "Other")
        val bloodOptions = listOf("Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        spSex.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sexOptions)
        spBlood.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodOptions)

        val current = ProfileStore.load(this)
        etName.setText(current.name)
        etAge.setText(current.age)
        spSex.setSelection(sexOptions.indexOf(current.sex).coerceAtLeast(0))
        spBlood.setSelection(bloodOptions.indexOf(current.bloodType).coerceAtLeast(0))
        etConditions.setText(current.conditions)
        etAllergies.setText(current.allergies)
        etEmergencyName.setText(current.emergencyContactName)
        etEmergencyPhone.setText(current.emergencyContactPhone)
        etHospital.setText(current.preferredHospital)
        etHospitalPhone.setText(current.preferredHospitalPhone)

        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val ageText = etAge.text.toString().trim()
            if (ageText.isNotEmpty() && (ageText.toIntOrNull() == null || ageText.toInt() !in 1..120)) {
                etAge.error = "Enter an age from 1 to 120"
                return@setOnClickListener
            }
            ProfileStore.save(
                this,
                UserProfile(
                    name = etName.text.toString().trim(),
                    age = ageText,
                    sex = spSex.selectedItem.toString(),
                    bloodType = spBlood.selectedItem.toString(),
                    conditions = etConditions.text.toString().trim(),
                    allergies = etAllergies.text.toString().trim(),
                    emergencyContactName = etEmergencyName.text.toString().trim(),
                    emergencyContactPhone = etEmergencyPhone.text.toString().trim(),
                    preferredHospital = etHospital.text.toString().trim(),
                    preferredHospitalPhone = etHospitalPhone.text.toString().trim()
                )
            )
            Toast.makeText(this, "Profile saved on this device", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
