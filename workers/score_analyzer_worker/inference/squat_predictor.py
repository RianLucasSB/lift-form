import os
from pathlib import Path
import numpy as np
import joblib

MODEL_PATH = str(Path(__file__).resolve().parent.parent / "models" / "squat_score_model.joblib")

FEATURE_COLS = [
    "knee_bottom_avg", "hip_bottom_avg", "back_bottom_avg",
    "knee_standing_avg", "hip_standing_avg", "back_standing_avg",
    "knee_lockout_avg", "hip_lockout_avg", "back_lockout_avg",
    "knee_rom_avg", "hip_rom_avg",
    "ecc_duration_avg", "con_duration_avg", "tempo_ratio_avg",
    "descent_rate_avg", "ascent_rate_avg",
    "depth_ratio_avg",
    "back_delta_avg",
    "lockout_deficit_knee_avg", "lockout_deficit_hip_avg",
    "ecc_smoothness_avg", "con_smoothness_avg",
    "knee_bottom_std", "hip_bottom_std", "back_bottom_std",
    "knee_rom_std", "tempo_ratio_std",
]


class SquatScorePredictor:
    def __init__(self, model_path: str = MODEL_PATH):
        self.model = joblib.load(model_path)

    def predict(self, features: dict) -> dict:
        missing = [c for c in FEATURE_COLS if features.get(c) is None]
        if missing:
            return {"score": None, "label": "no_data", "reason": f"Missing: {missing}"}

        X = np.array([[features[c] for c in FEATURE_COLS]])
        raw_score = float(self.model.predict(X)[0])
        score = round(np.clip(raw_score, 0.0, 1.0), 4)

        feedback = self._generate_feedback(features)
        label = _score_to_label(score)

        return {
            "score": score,
            "label": label,
            "feedback": feedback,
            "features_used": {c: features[c] for c in FEATURE_COLS},
        }

    def _generate_feedback(self, f: dict) -> dict:
        """Generate per-dimension feedback with actionable cues."""
        fb = {}

        # Depth
        kb = f["knee_bottom_avg"]
        if kb <= 85:
            fb["depth"] = {"rating": "good", "detail": f"Good depth ({kb:.0f}° knee at bottom)"}
        elif kb <= 100:
            fb["depth"] = {"rating": "fair", "detail": f"Slightly shallow ({kb:.0f}°). Try to go deeper, aim for parallel or below."}
        else:
            fb["depth"] = {"rating": "poor", "detail": f"Too shallow ({kb:.0f}°). Focus on hip mobility to reach parallel."}

        # Back position
        bb = f["back_bottom_avg"]
        if 25 <= bb <= 45:
            fb["back"] = {"rating": "good", "detail": f"Good trunk angle ({bb:.0f}°)"}
        elif bb < 25:
            fb["back"] = {"rating": "fair", "detail": f"Very upright torso ({bb:.0f}°). This may indicate a front squat pattern or limited hip flexion."}
        elif bb <= 55:
            fb["back"] = {"rating": "fair", "detail": f"Moderate forward lean ({bb:.0f}°). Strengthen your upper back and core."}
        else:
            fb["back"] = {"rating": "poor", "detail": f"Excessive forward lean ({bb:.0f}°). Risk of lower back strain. Work on thoracic mobility."}

        # Tempo
        ecc = f["ecc_duration_avg"]
        tr = f["tempo_ratio_avg"]
        if 0.8 <= ecc <= 3.0 and 0.8 <= tr <= 2.5:
            fb["tempo"] = {"rating": "good", "detail": f"Controlled tempo ({ecc:.1f}s down, ratio {tr:.1f})"}
        elif ecc < 0.8:
            fb["tempo"] = {"rating": "poor", "detail": f"Descending too fast ({ecc:.1f}s). Slow down the eccentric for control and safety."}
        else:
            fb["tempo"] = {"rating": "fair", "detail": f"Tempo could be improved (ecc={ecc:.1f}s, ratio={tr:.1f})"}

        # Lockout
        ldk = f["lockout_deficit_knee_avg"]
        if ldk <= 8:
            fb["lockout"] = {"rating": "good", "detail": "Full lockout at top"}
        elif ldk <= 15:
            fb["lockout"] = {"rating": "fair", "detail": f"Incomplete lockout ({ldk:.0f}° deficit). Fully extend your hips and knees at the top."}
        else:
            fb["lockout"] = {"rating": "poor", "detail": f"Poor lockout ({ldk:.0f}° deficit). You're not reaching full extension."}

        # ROM
        kr = f["knee_rom_avg"]
        if kr >= 60:
            fb["rom"] = {"rating": "good", "detail": f"Good range of motion ({kr:.0f}°)"}
        elif kr >= 40:
            fb["rom"] = {"rating": "fair", "detail": f"Limited ROM ({kr:.0f}°). Work on ankle and hip mobility."}
        else:
            fb["rom"] = {"rating": "poor", "detail": f"Very limited ROM ({kr:.0f}°). This may not count as a valid squat rep."}

        # Consistency
        kb_std = f.get("knee_bottom_std", 0)
        if kb_std <= 5:
            fb["consistency"] = {"rating": "good", "detail": "Consistent reps"}
        elif kb_std <= 10:
            fb["consistency"] = {"rating": "fair", "detail": f"Some variation between reps (±{kb_std:.1f}°). Focus on repeatable movement patterns."}
        else:
            fb["consistency"] = {"rating": "poor", "detail": f"High rep-to-rep variation (±{kb_std:.1f}°). Practice with lighter weight for consistency."}

        return fb


def _score_to_label(score: float) -> str:
    if score >= 0.9:
        return "excellent"
    elif score >= 0.7:
        return "good"
    elif score >= 0.5:
        return "fair"
    elif score >= 0.3:
        return "poor"
    else:
        return "bad"


if __name__ == "__main__":
    example = {
  "knee_bottom_avg": 48.01,
  "hip_bottom_avg": 55.42,
  "back_bottom_avg": 50.65,
  "knee_standing_avg": 136.53,
  "hip_standing_avg": 121.91,
  "back_standing_avg": 34.35,
  "knee_lockout_avg": 148.34,
  "hip_lockout_avg": 126.44,
  "back_lockout_avg": 25.5,
  "knee_rom_avg": 88.52,
  "hip_rom_avg": 66.49,
  "ecc_duration_avg": 1.667,
  "con_duration_avg": 1.3,
  "total_duration_avg": 2.967,
  "tempo_ratio_avg": 1.282,
  "descent_rate_avg": 53.11,
  "ascent_rate_avg": 77.18,
  "depth_ratio_avg": 0.2929,
  "back_delta_avg": 16.3,
  "lockout_deficit_knee_avg": 11.81,
  "lockout_deficit_hip_avg": 4.53,
  "ecc_smoothness_avg": 1.9199,
  "con_smoothness_avg": 2.6169,
  "knee_bottom_std": 0.0,
  "hip_bottom_std": 0.0,
  "back_bottom_std": 0.0,
  "knee_rom_std": 0.0,
  "tempo_ratio_std": 0.0,
  "num_reps": 1,
  "baseline_standing_angle": 163.9
}

    predictor = SquatScorePredictor(MODEL_PATH)
    result = predictor.predict(example)
    print(f"Score: {result['score']} ({result['label']})")
    print("\nFeedback:")
    for dim, info in result["feedback"].items():
        print(f"  {dim}: [{info['rating']}] {info['detail']}")
