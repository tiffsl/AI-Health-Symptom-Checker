package com.tiffany.symptomchecker.data

import android.content.Context

data class UserProfile(
    val name: String = "",
    val age: String = "",
    val sex: String = "Prefer not to say",
    val bloodType: String = "Unknown",
    val conditions: String = "",
    val allergies: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val preferredHospital: String = "",
    val preferredHospitalPhone: String = ""
)

object ProfileStore {
    private const val PREFS = "symptom_checker_profile"

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", profile.name)
            .putString("age", profile.age)
            .putString("sex", profile.sex)
            .putString("bloodType", profile.bloodType)
            .putString("conditions", profile.conditions)
            .putString("allergies", profile.allergies)
            .putString("emergencyContactName", profile.emergencyContactName)
            .putString("emergencyContactPhone", profile.emergencyContactPhone)
            .putString("preferredHospital", profile.preferredHospital)
            .putString("preferredHospitalPhone", profile.preferredHospitalPhone)
            .apply()
    }

    fun load(context: Context): UserProfile {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return UserProfile(
            name = p.getString("name", "") ?: "",
            age = p.getString("age", "") ?: "",
            sex = p.getString("sex", "Prefer not to say") ?: "Prefer not to say",
            bloodType = p.getString("bloodType", "Unknown") ?: "Unknown",
            conditions = p.getString("conditions", "") ?: "",
            allergies = p.getString("allergies", "") ?: "",
            emergencyContactName = p.getString("emergencyContactName", "") ?: "",
            emergencyContactPhone = p.getString("emergencyContactPhone", "") ?: "",
            preferredHospital = p.getString("preferredHospital", "") ?: "",
            preferredHospitalPhone = p.getString("preferredHospitalPhone", "") ?: ""
        )
    }
}
