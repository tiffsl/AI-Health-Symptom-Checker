package com.tiffany.symptomchecker.model

data class CareGuidance(
    val level: String,
    val message: String,
    val nextStep: String,
    val color: String,
    val icon: String,
    val isRedFlag: Boolean
)

object CareGuidanceEngine {

    fun evaluate(
        symptoms: List<String>,
        severity: String,
        duration: String
    ): CareGuidance {
        val keys = symptoms.map(::normalize).toSet()
        val sev = severityRank(severity)
        val dur = durationRank(duration)

        fun has(vararg names: String): Boolean =
            names.any { normalize(it) in keys }

        fun hasAll(vararg names: String): Boolean =
            names.all { normalize(it) in keys }

        fun hasAtLeast(required: Int, vararg names: String): Boolean =
            names.map(::normalize).distinct().count { it in keys } >= required

        var result = baselineGuidance(sev, dur)

        fun escalate(candidate: CareGuidance) {
            result = higherOf(result, candidate)
        }


        if (has("coma")) {
            return emergency(
                "Coma or unresponsiveness is a medical emergency.",
                "Call 999 now. Keep the person safe, check breathing, and do not leave them alone."
            )
        }

        val strokePattern = has("weakness of one body side") &&
                has("slurred speech", "altered sensorium", "visual disturbances")
        if (strokePattern) {
            return emergency(
                "Sudden one-sided weakness with speech, awareness, or vision changes may indicate a stroke.",
                "Call 999 now. Note the time symptoms started and do not drive yourself."
            )
        }

        val dangerousChestPattern = has("chest pain") && (
                has("breathlessness") ||
                        hasAtLeast(2, "sweating", "nausea", "vomiting", "dizziness", "palpitations", "fast heart rate")
                )
        if (dangerousChestPattern && sev >= 2) {
            return emergency(
                "Chest pain with breathing difficulty or other warning symptoms can be life-threatening.",
                "Call 999 now, especially if the pain is sudden, heavy, squeezing, or worsening."
            )
        }

        if (has("breathlessness") && sev == SEVERE &&
            has("altered sensorium", "chest pain", "cold hands and feet")) {
            return emergency(
                "Severe breathing difficulty with chest, circulation, or awareness changes needs emergency care.",
                "Call 999 now."
            )
        }

        if (has("stomach bleeding") &&
            has("dizziness", "altered sensorium", "fast heart rate", "cold hands and feet")) {
            return emergency(
                "Possible internal bleeding with circulation or awareness changes is an emergency.",
                "Call 999 now. Do not eat or drink while waiting for medical advice."
            )
        }

        if (has("acute liver failure")) {
            return emergency(
                "Acute liver failure requires emergency hospital assessment.",
                "Call 999 or go to an emergency department immediately."
            )
        }


        if (has("altered sensorium") &&
            has("high fever", "stiff neck", "headache", "vomiting")) {
            escalate(
                urgentMedicalAdvice(
                    "Confusion or altered awareness with fever, neck stiffness, headache, or vomiting needs urgent assessment.",
                    "Seek same-day urgent medical care. Call 999 if the person becomes difficult to wake, collapses, or worsens quickly."
                )
            )
        }

        if (has("slurred speech", "weakness of one body side", "altered sensorium")) {
            escalate(
                urgentMedicalAdvice(
                    "New speech, one-sided weakness, or awareness changes require urgent medical assessment.",
                    "Seek urgent care now. If the symptoms started suddenly, call 999."
                )
            )
        }

        if (hasAtLeast(2, "loss of balance", "unsteadiness", "spinning movements", "visual disturbances") &&
            has("headache", "vomiting", "weakness in limbs")) {
            escalate(
                seeDoctorSoon(
                    "Balance or vision disturbance with headache, vomiting, or limb weakness needs prompt review.",
                    "Arrange same-day medical assessment, especially if symptoms are new or worsening."
                )
            )
        }


        if (has("chest pain")) {
            when {
                sev == SEVERE -> escalate(
                    urgentMedicalAdvice(
                        "Severe chest pain should be assessed urgently even without other selected warning symptoms.",
                        "Seek urgent medical care now. Call 999 if pain is sudden, persistent, or accompanied by breathlessness, sweating, nausea, or dizziness."
                    )
                )
                sev == MODERATE || dur >= FEW_DAYS -> escalate(
                    seeDoctorSoon(
                        "Moderate or persistent chest pain should not be ignored.",
                        "Arrange prompt medical assessment. Call 999 if it becomes severe or is accompanied by breathlessness or faintness."
                    )
                )
                else -> escalate(
                    monitorClosely(
                        "New mild chest discomfort needs close observation.",
                        "Stop strenuous activity and monitor. Seek care promptly if it persists, returns, or worsens."
                    )
                )
            }
        }

        if (has("breathlessness")) {
            when {
                sev == SEVERE -> escalate(
                    urgentMedicalAdvice(
                        "Severe breathlessness requires urgent assessment.",
                        "Seek urgent care now. Call 999 if you are gasping, cannot speak normally, become confused, or develop chest tightness."
                    )
                )
                sev == MODERATE || dur >= FEW_DAYS -> escalate(
                    seeDoctorSoon(
                        "Moderate or persistent breathlessness needs medical assessment.",
                        "Arrange prompt medical review, particularly if it is new, worsening, or limiting normal activity."
                    )
                )
                else -> escalate(
                    monitorClosely(
                        "New mild breathlessness should be monitored carefully.",
                        "Rest and avoid exertion. Seek medical advice if it continues, returns, or worsens."
                    )
                )
            }
        }

        if (has("blood in sputum")) {
            escalate(
                if (sev == SEVERE || has("breathlessness", "chest pain", "dizziness")) {
                    urgentMedicalAdvice(
                        "Coughing up blood with severe, chest, breathing, or dizziness symptoms needs urgent care.",
                        "Seek urgent medical assessment now. Call 999 for heavy bleeding or severe breathing difficulty."
                    )
                } else {
                    seeDoctorSoon(
                        "Blood in sputum should be medically assessed even if the amount appears small.",
                        "Arrange prompt medical assessment."
                    )
                }
            )
        }

        if (has("cough") && has("high fever", "breathlessness", "chest pain", "rusty sputum")) {
            escalate(
                when {
                    sev == SEVERE -> seeDoctorSoon(
                        "A severe cough with fever, breathing difficulty, chest pain, or rusty sputum needs prompt assessment.",
                        "Seek same-day medical care."
                    )
                    dur >= FEW_DAYS -> considerMedicalAdvice(
                        "A cough with fever, chest symptoms, or unusual sputum lasting several days may need assessment.",
                        "Contact a clinic if it is not improving or is affecting breathing."
                    )
                    else -> monitorClosely(
                        "A new cough with fever or chest symptoms should be monitored closely.",
                        "Rest, hydrate, and seek care if breathing becomes difficult or symptoms worsen."
                    )
                }
            )
        }

        if (has("palpitations", "fast heart rate") && has("dizziness", "chest pain", "breathlessness")) {
            escalate(
                urgentMedicalAdvice(
                    "A fast or pounding heartbeat with dizziness, chest pain, or breathlessness needs urgent assessment.",
                    "Seek urgent medical care. Call 999 if symptoms are severe or you feel close to collapsing."
                )
            )
        }

        if (has("swollen legs", "swollen extremities", "fluid overload") &&
            has("breathlessness", "chest pain")) {
            escalate(
                urgentMedicalAdvice(
                    "Swelling or fluid retention with breathing or chest symptoms needs urgent assessment.",
                    "Seek same-day medical care. Call 999 if breathing is severe."
                )
            )
        }

        if (has("prominent veins on calf") &&
            has("swollen legs", "painful walking", "chest pain", "breathlessness")) {
            escalate(
                if (has("chest pain", "breathlessness")) {
                    urgentMedicalAdvice(
                        "Calf-vein symptoms with chest pain or breathlessness may indicate a serious circulation problem.",
                        "Seek urgent medical care now."
                    )
                } else {
                    seeDoctorSoon(
                        "New calf swelling, prominent veins, or pain while walking should be assessed.",
                        "Arrange prompt medical review, particularly if one leg is more affected."
                    )
                }
            )
        }

        // -----------------------------------------------------------------
        // 4) VOMITING, DIARRHOEA, DEHYDRATION, AND ABDOMINAL PAIN
        // -----------------------------------------------------------------

        val vomiting = has("vomiting")
        val diarrhoea = has("diarrhoea")
        val abdominalPain = has("stomach pain", "abdominal pain", "belly pain")
        val dehydrationSigns = hasAtLeast(
            2,
            "dehydration", "sunken eyes", "dark urine", "dizziness",
            "drying and tingling lips", "cold hands and feet", "lethargy"
        )

        if (vomiting && dehydrationSigns) {
            escalate(
                when {
                    sev == SEVERE || dur >= FEW_DAYS -> urgentMedicalAdvice(
                        "Vomiting with several dehydration signs needs urgent assessment.",
                        "Seek same-day medical care. If you cannot keep fluids down, become confused, or pass very little urine, seek emergency help."
                    )
                    else -> seeDoctorSoon(
                        "Vomiting with dehydration signs requires prompt medical advice.",
                        "Contact a clinic today and use oral rehydration fluids if tolerated."
                    )
                }
            )
        }

        if (vomiting && diarrhoea) {
            escalate(
                when {
                    sev == SEVERE && dur >= FEW_DAYS -> seeDoctorSoon(
                        "Severe vomiting and diarrhoea lasting several days creates a substantial dehydration risk.",
                        "Seek same-day medical care, especially if fluids will not stay down."
                    )
                    sev == SEVERE -> monitorClosely(
                        "Severe vomiting and diarrhoea starting today need close monitoring.",
                        "Use frequent small sips of oral rehydration fluid. Seek same-day care if you cannot stay hydrated."
                    )
                    dur >= ABOUT_WEEK -> arrangeAssessment(
                        "Vomiting and diarrhoea lasting about a week or longer should be assessed.",
                        "Arrange a medical appointment, sooner if worsening or dehydrated."
                    )
                    dur >= FEW_DAYS -> considerMedicalAdvice(
                        "Vomiting and diarrhoea lasting several days may need medical advice.",
                        "Contact a clinic if not improving or if hydration is difficult."
                    )
                    else -> monitor(
                        "Short-duration vomiting and diarrhoea can often be managed with fluids and rest.",
                        "Use oral rehydration fluids and monitor for dehydration."
                    )
                }
            )
        }

        if (abdominalPain && vomiting) {
            escalate(
                when {
                    has("stomach bleeding", "bloody stool") -> urgentMedicalAdvice(
                        "Abdominal pain and vomiting with possible bleeding need urgent assessment.",
                        "Seek urgent medical care now."
                    )
                    sev == SEVERE && dur >= FEW_DAYS -> seeDoctorSoon(
                        "Severe abdominal pain with vomiting lasting several days needs prompt assessment.",
                        "Seek same-day medical care. Go urgently if pain becomes sudden, localised, or unbearable."
                    )
                    sev >= MODERATE && dur >= FEW_DAYS -> considerMedicalAdvice(
                        "Abdominal pain with vomiting lasting several days may need medical review.",
                        "Contact a clinic, especially if eating or drinking is difficult."
                    )
                    else -> monitorClosely(
                        "Abdominal pain with vomiting should be monitored closely.",
                        "Seek care if pain becomes severe, localised, persistent, or is joined by fever or dehydration."
                    )
                }
            )
        }

        if (has("bloody stool")) {
            escalate(
                if (sev == SEVERE || has("dizziness", "fast heart rate", "abdominal pain", "belly pain")) {
                    urgentMedicalAdvice(
                        "Blood in stool with severe symptoms, dizziness, rapid heartbeat, or abdominal pain needs urgent assessment.",
                        "Seek urgent medical care now."
                    )
                } else {
                    seeDoctorSoon(
                        "Blood in stool should be medically assessed.",
                        "Arrange prompt medical review, even if bleeding appears small."
                    )
                }
            )
        }

        if (has("stomach bleeding")) {
            escalate(
                urgentMedicalAdvice(
                    "Possible stomach bleeding requires urgent medical assessment.",
                    "Seek urgent care now. Call 999 if there is collapse, severe dizziness, confusion, or heavy bleeding."
                )
            )
        }

        if (diarrhoea && has("high fever", "bloody stool", "dehydration", "abdominal pain")) {
            escalate(
                if (sev == SEVERE || dur >= FEW_DAYS) {
                    seeDoctorSoon(
                        "Diarrhoea with fever, blood, dehydration, or abdominal pain needs prompt medical review.",
                        "Seek same-day medical advice."
                    )
                } else {
                    considerMedicalAdvice(
                        "Diarrhoea with additional warning symptoms may need medical advice.",
                        "Contact a clinic if symptoms worsen or hydration becomes difficult."
                    )
                }
            )
        }

        if (has("constipation") && abdominalPain &&
            has("distention of abdomen", "swelling of stomach", "vomiting")) {
            escalate(
                if (sev == SEVERE) {
                    urgentMedicalAdvice(
                        "Severe constipation with abdominal swelling, pain, or vomiting needs urgent assessment.",
                        "Seek urgent medical care."
                    )
                } else {
                    seeDoctorSoon(
                        "Constipation with abdominal swelling, pain, or vomiting should be assessed promptly.",
                        "Arrange prompt medical review."
                    )
                }
            )
        }

        if (has("pain during bowel movements", "pain in anal region", "irritation in anus") &&
            has("bloody stool")) {
            escalate(
                seeDoctorSoon(
                    "Pain around bowel movements with blood in stool should be medically assessed.",
                    "Arrange prompt medical review."
                )
            )
        }


        if (has("high fever") && has("stiff neck", "altered sensorium")) {
            escalate(
                urgentMedicalAdvice(
                    "High fever with neck stiffness or altered awareness needs urgent assessment.",
                    "Seek urgent medical care now. Call 999 if consciousness is reduced or symptoms worsen rapidly."
                )
            )
        }

        if (has("high fever") && hasAtLeast(2, "chills", "shivering", "malaise", "lethargy", "fast heart rate")) {
            escalate(
                when {
                    sev == SEVERE -> seeDoctorSoon(
                        "Severe fever with several systemic illness symptoms needs prompt assessment.",
                        "Seek same-day medical care."
                    )
                    dur >= FEW_DAYS -> considerMedicalAdvice(
                        "Fever with chills or marked unwellness lasting several days may need medical advice.",
                        "Contact a clinic if not improving."
                    )
                    else -> monitorClosely(
                        "Fever with chills or marked unwellness should be monitored closely.",
                        "Rest, hydrate, and seek care if worsening."
                    )
                }
            )
        }

        if (has("high fever")) {
            escalate(
                when {
                    sev == SEVERE && dur >= FEW_DAYS -> seeDoctorSoon(
                        "A severe high fever lasting several days needs medical assessment.",
                        "Seek prompt medical care."
                    )
                    sev == SEVERE -> monitorClosely(
                        "A severe high fever that started today needs close monitoring.",
                        "Hydrate and seek care promptly if it remains very high or new warning symptoms appear."
                    )
                    dur >= ABOUT_WEEK -> arrangeAssessment(
                        "A high fever lasting about a week or longer should be assessed.",
                        "Arrange a medical appointment."
                    )
                    dur >= FEW_DAYS -> considerMedicalAdvice(
                        "A high fever lasting several days may need medical advice.",
                        "Contact a clinic if not improving."
                    )
                    else -> monitor(
                        "A short-duration fever can often be monitored with rest and fluids.",
                        "Seek care if it rises, persists, or warning symptoms develop."
                    )
                }
            )
        }

        if (has("toxic look (typhos)") && has("high fever", "altered sensorium", "dehydration")) {
            escalate(
                urgentMedicalAdvice(
                    "A very unwell appearance with fever, confusion, or dehydration needs urgent assessment.",
                    "Seek urgent medical care now."
                )
            )
        }


        val jaundice = has("yellowish skin", "yellowing of eyes")
        val liverPattern = jaundice && hasAtLeast(
            1,
            "dark urine", "yellow urine", "itching", "internal itching",
            "abdominal pain", "swelling of stomach", "distention of abdomen",
            "nausea", "vomiting", "loss of appetite"
        )

        if (liverPattern) {
            escalate(
                when {
                    has("altered sensorium", "stomach bleeding", "fluid overload") -> urgentMedicalAdvice(
                        "Jaundice with awareness changes, bleeding, or major fluid retention needs urgent assessment.",
                        "Seek urgent medical care now."
                    )
                    sev == SEVERE -> seeDoctorSoon(
                        "Severe jaundice-related symptoms need prompt medical assessment.",
                        "Seek same-day medical care."
                    )
                    else -> arrangeAssessment(
                        "Yellow skin or eyes with dark urine, itching, digestive symptoms, or abdominal swelling needs medical assessment.",
                        "Arrange a medical appointment promptly."
                    )
                }
            )
        } else if (jaundice) {
            escalate(
                arrangeAssessment(
                    "Yellowing of the skin or eyes should be medically assessed.",
                    "Arrange a medical appointment promptly."
                )
            )
        }

        // Exposure risk factors only modify liver/infection-related patterns.
        val bloodExposureRisk = has("receiving blood transfusion", "receiving unsterile injections")
        val liverRisk = has("history of alcohol consumption", "family history")
        if ((bloodExposureRisk || liverRisk) &&
            has("yellowish skin", "yellowing of eyes", "dark urine", "abdominal pain", "fatigue")) {
            escalate(
                arrangeAssessment(
                    "Relevant exposure or history together with possible liver-related symptoms should be medically assessed.",
                    "Arrange a medical appointment and mention the exposure or history to the clinician."
                )
            )
        }

        val urinaryPattern = hasAtLeast(
            2,
            "burning micturition", "bladder discomfort", "foul smell of urine",
            "continuous feel of urine", "spotting urination"
        )
        if (urinaryPattern) {
            escalate(
                when {
                    has("high fever", "chills", "back pain", "vomiting") && sev >= MODERATE -> seeDoctorSoon(
                        "Urinary symptoms with fever, chills, back pain, or vomiting may indicate a more serious infection.",
                        "Seek same-day medical assessment."
                    )
                    dur >= FEW_DAYS || sev == SEVERE -> considerMedicalAdvice(
                        "Persistent or severe urinary symptoms need medical advice.",
                        "Contact a clinic for assessment and possible urine testing."
                    )
                    else -> monitorClosely(
                        "New urinary discomfort should be monitored closely.",
                        "Hydrate and seek medical advice if symptoms persist, worsen, or fever develops."
                    )
                }
            )
        }

        if (has("polyuria", "excessive hunger", "increased appetite") &&
            has("weight loss", "fatigue", "irregular sugar level")) {
            escalate(
                when {
                    has("vomiting", "dehydration", "altered sensorium") -> urgentMedicalAdvice(
                        "Possible blood-sugar symptoms with vomiting, dehydration, or altered awareness need urgent assessment.",
                        "Seek urgent medical care now."
                    )
                    else -> arrangeAssessment(
                        "Frequent urination or increased hunger with weight loss, fatigue, or abnormal sugar readings needs assessment.",
                        "Arrange a medical appointment for blood-sugar evaluation."
                    )
                }
            )
        }


        val widespreadSkinPattern = has("skin rash", "red spots over body", "nodal skin eruptions")
        if (widespreadSkinPattern && has("high fever", "breathlessness", "altered sensorium")) {
            escalate(
                if (has("breathlessness", "altered sensorium")) {
                    urgentMedicalAdvice(
                        "A widespread rash with breathing difficulty or altered awareness needs urgent assessment.",
                        "Seek urgent medical care now."
                    )
                } else {
                    seeDoctorSoon(
                        "A widespread rash with high fever needs prompt medical assessment.",
                        "Seek same-day medical care."
                    )
                }
            )
        }

        if (has("blister", "yellow crust ooze", "pus filled pimples", "red sore around nose") &&
            has("high fever", "swelled lymph nodes", "malaise")) {
            escalate(
                seeDoctorSoon(
                    "Possible skin infection with fever, swollen lymph nodes, or marked unwellness needs prompt assessment.",
                    "Seek same-day medical care."
                )
            )
        }

        if (hasAtLeast(2, "skin peeling", "silver like dusting", "small dents in nails", "inflammatory nails")) {
            escalate(
                if (dur >= ABOUT_WEEK || sev >= MODERATE) {
                    arrangeAssessment(
                        "Persistent skin peeling or nail changes may need medical assessment.",
                        "Arrange a routine appointment with a clinician."
                    )
                } else {
                    considerMedicalAdvice(
                        "New skin or nail changes may benefit from medical advice if they persist.",
                        "Monitor and contact a clinician if spreading, painful, or persistent."
                    )
                }
            )
        }

        if (has("itching", "skin rash", "dischromic patches", "internal itching") && dur >= ABOUT_WEEK) {
            escalate(
                considerMedicalAdvice(
                    "Persistent itching, rash, or skin-colour changes may need medical advice.",
                    "Arrange a routine assessment, especially if spreading or disturbing sleep."
                )
            )
        }

        if (has("headache") && has("visual disturbances", "blurred and distorted vision") &&
            has("weakness of one body side", "slurred speech", "altered sensorium")) {
            escalate(
                urgentMedicalAdvice(
                    "Headache with vision and neurological changes requires urgent assessment.",
                    "Seek urgent medical care now. Call 999 if symptoms started suddenly."
                )
            )
        }

        if (has("headache") && has("high fever", "stiff neck", "vomiting")) {
            escalate(
                seeDoctorSoon(
                    "Headache with high fever, stiff neck, or vomiting needs prompt medical assessment.",
                    "Seek same-day medical care; seek emergency help if confusion or reduced consciousness develops."
                )
            )
        }

        if (hasAtLeast(2, "sinus pressure", "congestion", "runny nose", "throat irritation", "continuous sneezing") &&
            has("high fever", "pain behind the eyes")) {
            escalate(
                if (sev == SEVERE || dur >= ABOUT_WEEK) {
                    considerMedicalAdvice(
                        "Persistent or severe sinus and upper-respiratory symptoms with fever or eye pain may need assessment.",
                        "Contact a clinic."
                    )
                } else {
                    monitorClosely(
                        "Sinus and upper-respiratory symptoms with fever or eye pain should be monitored.",
                        "Rest, hydrate, and seek advice if worsening or not improving."
                    )
                }
            )
        }

        if (has("redness of eyes", "watering from eyes") &&
            has("visual disturbances", "pain behind the eyes")) {
            escalate(
                seeDoctorSoon(
                    "Eye redness or watering with visual disturbance or pain behind the eyes needs prompt assessment.",
                    "Arrange same-day eye or medical assessment."
                )
            )
        }

        if (has("muscle wasting") ||
            (has("muscle weakness", "weakness in limbs") && dur >= ABOUT_WEEK)) {
            escalate(
                arrangeAssessment(
                    "Muscle wasting or persistent weakness needs medical assessment.",
                    "Arrange a medical appointment for neurological and general evaluation."
                )
            )
        }

        if (hasAtLeast(2, "joint pain", "swelling joints", "movement stiffness", "knee pain", "hip joint pain") &&
            has("high fever", "red spots over body")) {
            escalate(
                seeDoctorSoon(
                    "Joint pain or swelling with fever or a widespread rash needs prompt assessment.",
                    "Seek same-day medical care."
                )
            )
        }

        if (hasAtLeast(2, "joint pain", "swelling joints", "movement stiffness", "knee pain", "hip joint pain") &&
            dur >= ABOUT_WEEK) {
            escalate(
                arrangeAssessment(
                    "Persistent joint pain, swelling, or stiffness should be medically assessed.",
                    "Arrange a routine medical appointment."
                )
            )
        }

        if (hasAtLeast(2, "spinning movements", "loss of balance", "unsteadiness", "dizziness") &&
            sev >= MODERATE) {
            escalate(
                if (has("slurred speech", "weakness of one body side", "visual disturbances")) {
                    urgentMedicalAdvice(
                        "Balance symptoms with neurological changes need urgent assessment.",
                        "Seek urgent medical care now."
                    )
                } else {
                    seeDoctorSoon(
                        "Moderate or severe dizziness and balance problems need prompt assessment.",
                        "Arrange same-day medical review and avoid driving."
                    )
                }
            )
        }

        if (has("enlarged thyroid") &&
            has("breathlessness", "palpitations", "weight loss", "weight gain")) {
            escalate(
                if (has("breathlessness") && sev == SEVERE) {
                    urgentMedicalAdvice(
                        "Thyroid enlargement with severe breathing difficulty needs urgent assessment.",
                        "Seek urgent medical care now."
                    )
                } else {
                    arrangeAssessment(
                        "Thyroid enlargement with breathing, heartbeat, or weight symptoms needs medical assessment.",
                        "Arrange a medical appointment."
                    )
                }
            )
        }

        if (has("fatigue", "lethargy", "malaise") &&
            hasAtLeast(1, "weight loss", "weight gain", "puffy face and eyes", "brittle nails", "lack of concentration") &&
            dur >= ABOUT_WEEK) {
            escalate(
                arrangeAssessment(
                    "Persistent fatigue with weight, swelling, nail, or concentration changes may need investigation.",
                    "Arrange a routine medical appointment."
                )
            )
        }

        if (has("fatigue") && dur >= LONGER_WEEK) {
            escalate(
                considerMedicalAdvice(
                    "Unexplained fatigue lasting longer than a week may benefit from medical advice.",
                    "Contact a clinic if it affects daily life, keeps worsening, or continues."
                )
            )
        }

        if (has("abnormal menstruation") &&
            has("dizziness", "fatigue", "stomach pain", "abdominal pain", "spotting urination")) {
            escalate(
                if (sev == SEVERE) {
                    seeDoctorSoon(
                        "Severe abnormal bleeding or menstrual symptoms with dizziness, fatigue, or pain need prompt assessment.",
                        "Seek same-day medical care, especially if bleeding is heavy."
                    )
                } else {
                    arrangeAssessment(
                        "Abnormal menstruation with pain, dizziness, or fatigue should be assessed.",
                        "Arrange a medical appointment."
                    )
                }
            )
        }

        if (has("extra marital contacts") &&
            has("burning micturition", "spotting urination", "skin rash", "swelled lymph nodes")) {
            escalate(
                considerMedicalAdvice(
                    "A possible sexual-exposure risk together with urinary, rash, or lymph-node symptoms should be assessed confidentially.",
                    "Arrange a clinic visit for appropriate testing and advice."
                )
            )
        }

        if (hasAtLeast(2, "depression", "anxiety", "mood swings", "irritability", "restlessness") &&
            dur >= ABOUT_WEEK) {
            escalate(
                considerMedicalAdvice(
                    "Persistent emotional or mood symptoms may benefit from professional support.",
                    "Consider speaking with a healthcare professional or counsellor, especially if daily life is affected."
                )
            )
        }

        return result
    }

    private fun baselineGuidance(sev: Int, dur: Int): CareGuidance = when (sev) {
        MILD -> when (dur) {
            TODAY -> selfCare(
                "Mild symptoms that started today can usually be managed with self-care.",
                "Rest, stay hydrated, and monitor for changes. Seek advice if symptoms worsen."
            )
            FEW_DAYS -> monitor(
                "Mild symptoms lasting a few days should be monitored for improvement.",
                "Continue self-care. Seek medical advice if they worsen or do not improve."
            )
            else -> considerMedicalAdvice(
                "Mild symptoms lasting about a week or longer may benefit from medical advice.",
                "Contact a clinic if symptoms are persistent, unexplained, or affecting daily activities."
            )
        }

        MODERATE -> when (dur) {
            TODAY -> monitor(
                "Moderate symptoms that started today should be monitored.",
                "Rest, hydrate, and seek advice if symptoms worsen significantly."
            )
            FEW_DAYS -> monitorClosely(
                "Moderate symptoms lasting several days need closer monitoring.",
                "Seek medical advice if they are not improving or are affecting normal activities."
            )
            ABOUT_WEEK -> considerMedicalAdvice(
                "Moderate symptoms lasting about a week should be discussed with a healthcare professional.",
                "Contact a clinic for advice or assessment."
            )
            else -> arrangeAssessment(
                "Moderate symptoms lasting longer than a week should be medically assessed.",
                "Arrange a medical appointment."
            )
        }

        else -> when (dur) {
            TODAY -> monitorClosely(
                "Severe symptoms that started today should be monitored closely.",
                "Seek prompt care if they worsen, interfere greatly with normal activity, or a warning symptom appears."
            )
            FEW_DAYS -> considerMedicalAdvice(
                "Severe symptoms lasting several days need medical advice.",
                "Contact a doctor. Seek same-day care if symptoms are worsening or difficult to manage."
            )
            else -> seeDoctorSoon(
                "Severe symptoms lasting about a week or longer need prompt medical assessment.",
                "See a doctor soon for evaluation."
            )
        }
    }

    private fun severityRank(value: String): Int = when (normalize(value)) {
        "severe" -> SEVERE
        "moderate" -> MODERATE
        else -> MILD
    }

    private fun durationRank(value: String): Int = when (normalize(value)) {
        "longer than 1 week", "longer than one week", "more than 1 week" -> LONGER_WEEK
        "about 1 week", "about one week", "1 week" -> ABOUT_WEEK
        "2-3 days", "2–3 days", "a few days" -> FEW_DAYS
        else -> TODAY
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace("_", " ")
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), " ")

    private fun levelRank(level: String): Int = when (level) {
        "Self-care" -> 1
        "Monitor" -> 2
        "Monitor Closely" -> 3
        "Consider Medical Advice" -> 4
        "Arrange Medical Assessment" -> 5
        "See Doctor Soon" -> 6
        "Urgent Medical Advice" -> 7
        "Emergency" -> 8
        else -> 1
    }

    private fun higherOf(a: CareGuidance, b: CareGuidance): CareGuidance =
        if (levelRank(b.level) > levelRank(a.level)) b else a

    private fun emergency(reason: String, action: String) = CareGuidance(
        level = "Emergency",
        message = reason,
        nextStep = action,
        color = "#B71C1C",
        icon = "⚠",
        isRedFlag = true
    )

    private fun urgentMedicalAdvice(reason: String, action: String) = CareGuidance(
        level = "Urgent Medical Advice",
        message = reason,
        nextStep = action,
        color = "#E64A19",
        icon = "!",
        isRedFlag = true
    )

    private fun seeDoctorSoon(reason: String, action: String) = CareGuidance(
        level = "See Doctor Soon",
        message = reason,
        nextStep = action,
        color = "#D97706",
        icon = "●",
        isRedFlag = false
    )

    private fun arrangeAssessment(reason: String, action: String) = CareGuidance(
        level = "Arrange Medical Assessment",
        message = reason,
        nextStep = action,
        color = "#C58A00",
        icon = "●",
        isRedFlag = false
    )

    private fun considerMedicalAdvice(reason: String, action: String) = CareGuidance(
        level = "Consider Medical Advice",
        message = reason,
        nextStep = action,
        color = "#A66A00",
        icon = "i",
        isRedFlag = false
    )

    private fun monitorClosely(reason: String, action: String) = CareGuidance(
        level = "Monitor Closely",
        message = reason,
        nextStep = action,
        color = "#1565C0",
        icon = "i",
        isRedFlag = false
    )

    private fun monitor(reason: String, action: String) = CareGuidance(
        level = "Monitor",
        message = reason,
        nextStep = action,
        color = "#2E7D9A",
        icon = "i",
        isRedFlag = false
    )

    private fun selfCare(reason: String, action: String) = CareGuidance(
        level = "Self-care",
        message = reason,
        nextStep = action,
        color = "#2E7D32",
        icon = "✓",
        isRedFlag = false
    )

    private const val MILD = 1
    private const val MODERATE = 2
    private const val SEVERE = 3

    private const val TODAY = 1
    private const val FEW_DAYS = 2
    private const val ABOUT_WEEK = 3
    private const val LONGER_WEEK = 4
}