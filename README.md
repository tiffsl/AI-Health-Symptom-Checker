# AI-Based Health Symptom Checker and Triage Assistant

**Capstone Project 2 – SEG3203**

An Android-based health symptom checker that combines machine-learning condition prediction with rule-based care guidance.

## Main Features

- 132 structured symptom inputs
- Top-3 condition predictions
- Logistic Regression model
- Flask REST API backend
- Severity and duration-based care guidance
- Red-flag emergency detection
- Health Profile and Check History
- Nearby clinic/hospital search
- Emergency 999 dial function

## Technologies

- Kotlin / Android Studio
- Python
- Flask
- scikit-learn
- Retrofit 2
- SharedPreferences

## Project Structure

```text
AI-Health-Symptom-Checker/
├── android/     - Android application
├── backend/     - Flask API and ML model
├── README.md
└── .gitignore
```

## Running the Backend

```bash
cd backend
pip install -r requirements.txt
python app.py
```

The backend runs on port `5050`.

For the Android emulator, the backend can be accessed using:

```text
http://10.0.2.2:5050/
```

## Dataset

This project uses the **Disease Prediction Using Machine Learning** dataset from Kaggle:

https://www.kaggle.com/datasets/kaushil268/disease-prediction-using-machine-learning

The dataset was used for academic machine-learning development and evaluation.

## Disclaimer

This application is an academic prototype developed for Capstone Project 2.

It is not intended to provide a medical diagnosis, replace professional medical advice, or be used as an emergency medical service.

Users experiencing severe or potentially life-threatening symptoms should seek professional medical attention immediately.
