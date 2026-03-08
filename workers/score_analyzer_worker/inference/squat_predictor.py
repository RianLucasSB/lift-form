import os
import numpy as np
import joblib

MODEL_PATH = os.path.join(os.path.dirname(__file__), "models", "squat_score_model.joblib")

FEATURES = [
    "knee_min", "knee_avg", "knee_max",
    "hip_min", "hip_avg", "hip_max",
    "back_min", "back_avg", "back_max",
]

_model = None


def _load_model():
    global _model
    if _model is None:
        if not os.path.exists(MODEL_PATH):
            raise FileNotFoundError(
                f"Model not found at {MODEL_PATH}. Run train/squat.py first."
            )
        _model = joblib.load(MODEL_PATH)
    return _model


def predict_squat_score(features: dict) -> float:
    """
    Predict squat form score from extracted features.

    Args:
        features: dict with keys matching FEATURES
            e.g. {"knee_min": 75.0, "knee_avg": 80.0, "knee_max": 85.0,
                   "hip_min": 65.0, "hip_avg": 75.0, "hip_max": 80.0,
                   "back_min": 8.0,  "back_avg": 12.0, "back_max": 15.0}

    Returns:
        float: score between 0.0 and 1.0
    """
    model = _load_model()
    X = np.array([[features[f] for f in FEATURES]])
    score = float(np.clip(model.predict(X)[0], 0.0, 1.0))
    return round(score, 4)


if __name__ == "__main__":
    # Quick test with example inputs
    examples = [
       {
            "knee_min": 53.0, "knee_avg": 57.9, "knee_max": 63.8,
            "hip_min": 42.2, "hip_avg": 49.4, "hip_max": 54.8,
            "back_min": 32.1,  "back_avg": 36.0, "back_max": 41.2,
        }
    ]

    labels = ["Good form", "Poor form", "Inconsistent"]
    for label, ex in zip(labels, examples):
        score = predict_squat_score(ex)
        print(f"{label:15s} -> Score: {score}")
