# AI-Based Health Symptom Checker and Triage Assistant

Capstone Project 2 – SEG3203

This project is an Android-based health symptom checker and triage assistant that combines machine-learning condition prediction with deterministic rule-based care guidance.

Main Features

- 132 structured symptom inputs
- Logistic Regression disease prediction
- Top-3 possible condition matches
- Flask REST API backend
- Severity and duration-based care guidance
- Red-flag emergency detection
- Health Profile
- Check History
- Nearby clinic/hospital search
- Emergency 999 dial function

Technologies

- Kotlin / Android Studio
- Python
- Flask
- scikit-learn
- Retrofit 2
- SharedPreferences

Project Structure

- `android/` – Android application
- `backend/` – Flask API and machine-learning model

Running the Backend

```bash
pip install -r requirements.txt
python app.py
