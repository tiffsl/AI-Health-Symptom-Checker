import pickle, os
import numpy as np
from flask import Flask, request, jsonify

app = Flask(__name__)

BASE = os.path.dirname(os.path.abspath(__file__))
MODEL_DIR = os.path.join(BASE, "models")

with open(os.path.join(MODEL_DIR, "best_model.pkl"), "rb") as f: MODEL = pickle.load(f)
with open(os.path.join(MODEL_DIR, "label_encoder.pkl"), "rb") as f: LE = pickle.load(f)
with open(os.path.join(MODEL_DIR, "symptom_columns.pkl"), "rb") as f: SYMPTOMS = pickle.load(f)
with open(os.path.join(MODEL_DIR, "triage_map.pkl"), "rb") as f: TRIAGE_MAP = pickle.load(f)
with open(os.path.join(MODEL_DIR, "red_flags.pkl"), "rb") as f: RED_FLAGS = pickle.load(f)

TRIAGE_ADVICE = {
    "Emergency":      {"message": "Seek immediate medical attention. Go to the nearest emergency room now.", "color": "#E74C3C", "icon": "🚨"},
    "See Doctor Soon":{"message": "Schedule an appointment with a doctor within 24-48 hours.", "color": "#E8A838", "icon": "⚠️"},
    "Monitor":        {"message": "Monitor your symptoms. See a doctor if they worsen.", "color": "#3498DB", "icon": "👁️"},
    "Self-care":      {"message": "Rest and manage symptoms at home. See a doctor if symptoms worsen.", "color": "#2ECC71", "icon": "🏠"}
}

@app.route("/api/health")
def health():
    return jsonify({"status": "ok", "diseases": 41})

@app.route("/api/symptoms")
def get_symptoms():
    readable = [s.replace("_", " ").title() for s in SYMPTOMS]
    return jsonify({"symptoms": SYMPTOMS, "readable": readable, "count": len(SYMPTOMS)})

@app.route("/api/predict", methods=["POST"])
def predict():
    data = request.get_json(silent=True)
    if not data or "symptoms" not in data:
        return jsonify({"error": "Missing symptoms"}), 400

    if not isinstance(data["symptoms"], list):
        return jsonify({"error": "symptoms must be a list"}), 400
    if len(data["symptoms"]) == 0:
        return jsonify({"error": "Select at least one symptom"}), 400
    if len(data["symptoms"]) > 15:
        return jsonify({"error": "A maximum of 15 symptoms is allowed"}), 400

    x = np.zeros(len(SYMPTOMS), dtype=np.float64)
    active = []
    for sym in data["symptoms"]:
        s = sym.strip().lower().replace(" ", "_")
        if s in SYMPTOMS:
            x[SYMPTOMS.index(s)] = 1
            active.append(s)

    if not active:
        return jsonify({"error": "None of the supplied symptoms are recognised"}), 400

    proba = MODEL.predict_proba(x.reshape(1, -1))[0]
    top3_idx = np.argsort(proba)[-3:][::-1]
    top3 = []
    for i in top3_idx:
        disease = LE.inverse_transform([i])[0]
        top3.append({
            "disease": disease,
            "confidence": round(float(proba[i]) * 100, 1),
            "triage": TRIAGE_MAP.get(disease, "See Doctor Soon")
        })

    flags = [s for s in active if s in RED_FLAGS]
    triage = "Emergency" if flags else top3[0]["triage"]

    return jsonify({
        "top3_predictions": top3,
        "triage_level": triage,
        "triage_advice": TRIAGE_ADVICE.get(triage, TRIAGE_ADVICE["See Doctor Soon"]),
        "red_flags_triggered": flags,
        "symptoms_analysed": active,
        "disclaimer": "This tool is for informational purposes only. Always consult a qualified healthcare professional."
    })

if __name__ == "__main__":
    print("CP2 Symptom Checker API running on port 5050...")
    app.run(host="0.0.0.0", port=5050, debug=False)
