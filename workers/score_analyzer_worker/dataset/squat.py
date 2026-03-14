import numpy as np
import pandas as pd
import os

FEATURE_COLS = [
    # Bottom position
    "knee_bottom_avg", "hip_bottom_avg", "back_bottom_avg",
    # Standing position
    "knee_standing_avg", "hip_standing_avg", "back_standing_avg",
    # Lockout
    "knee_lockout_avg", "hip_lockout_avg", "back_lockout_avg",
    # ROM
    "knee_rom_avg", "hip_rom_avg",
    # Tempo
    "ecc_duration_avg", "con_duration_avg", "tempo_ratio_avg",
    # Velocity
    "descent_rate_avg", "ascent_rate_avg",
    # Depth
    "depth_ratio_avg",
    # Back change
    "back_delta_avg",
    # Lockout quality
    "lockout_deficit_knee_avg", "lockout_deficit_hip_avg",
    # Smoothness
    "ecc_smoothness_avg", "con_smoothness_avg",
    # Consistency across reps
    "knee_bottom_std", "hip_bottom_std", "back_bottom_std",
    "knee_rom_std", "tempo_ratio_std",
]

TARGET_COL = "score"
ALL_COLS = FEATURE_COLS + [TARGET_COL]


def calculate_squat_score(f):
    """
    Calculate squat score from phase-based features.
    Score: 0.0 (dangerous/terrible) to 1.0 (textbook form).

    Evaluates 6 dimensions:
      1. Depth quality (knee/hip at bottom)
      2. Back position (lean at bottom + delta)
      3. ROM completeness
      4. Tempo & control
      5. Lockout quality
      6. Rep-to-rep consistency
    """
    score = 1.0

    # === 1. DEPTH (0.25 weight) ===
    kb = f["knee_bottom_avg"]
    hb = f["hip_bottom_avg"]

    # Knee at bottom: ideal 50-85°, acceptable to 95°
    if kb < 40:
        score -= 0.05  # hypermobile / too deep
    elif kb <= 85:
        pass  # excellent depth
    elif kb <= 95:
        score -= (kb - 85) * 0.015  # up to 0.15
    elif kb <= 110:
        score -= 0.15 + (kb - 95) * 0.025  # up to 0.525
    else:
        score -= min(0.55, 0.525 + (kb - 110) * 0.01)

    # Hip at bottom: ideal 40-75°
    if hb < 30:
        score -= 0.05
    elif hb <= 75:
        pass
    elif hb <= 95:
        score -= (hb - 75) * 0.01
    elif hb > 95:
        score -= min(0.25, 0.20 + (hb - 95) * 0.008)

    # === 2. BACK POSITION (0.20 weight) ===
    bb = f["back_bottom_avg"]
    bd = f["back_delta_avg"]

    # Back lean at bottom: ideal 25-45°
    if bb < 15:
        score -= 0.12  # too upright (front squat territory or error)
    elif bb < 25:
        score -= (25 - bb) * 0.008
    elif bb <= 45:
        pass
    elif bb <= 55:
        score -= (bb - 45) * 0.02
    elif bb <= 65:
        score -= 0.20 + (bb - 55) * 0.025
    else:
        score -= min(0.40, 0.45 + (bb - 65) * 0.015)

    # Excessive forward lean change (standing→bottom)
    if bd > 25:
        score -= min(0.10, (bd - 25) * 0.005)

    # === 3. ROM (0.15 weight) ===
    kr = f["knee_rom_avg"]
    hr = f["hip_rom_avg"]

    # Knee ROM: ideal 60-100°
    if kr < 30:
        score -= min(0.20, (30 - kr) * 0.01)
    elif kr < 50:
        score -= (50 - kr) * 0.005
    elif kr <= 100:
        pass
    else:
        score -= min(0.05, (kr - 100) * 0.003)

    # Hip ROM should be roughly proportional to knee ROM
    if kr > 0:
        rom_ratio = hr / kr
        if rom_ratio < 0.4 or rom_ratio > 1.6:
            score -= min(0.08, abs(rom_ratio - 0.9) * 0.05)

    # === 4. TEMPO & CONTROL (0.15 weight) ===
    ecc = f["ecc_duration_avg"]
    con = f["con_duration_avg"]
    tr = f["tempo_ratio_avg"]
    dr = f["descent_rate_avg"]
    ar = f["ascent_rate_avg"]

    # Eccentric too fast (< 0.8s) or too slow (> 4s)
    if ecc < 0.5:
        score -= min(0.12, (0.5 - ecc) * 0.2)
    elif ecc < 0.8:
        score -= (0.8 - ecc) * 0.05
    elif ecc > 4.0:
        score -= min(0.05, (ecc - 4.0) * 0.02)

    # Concentric too slow suggests struggle (not necessarily bad, mild penalty)
    if con > 4.0:
        score -= min(0.05, (con - 4.0) * 0.015)

    # Tempo ratio: ideal 1.0-2.5
    if tr < 0.5:
        score -= min(0.08, (0.5 - tr) * 0.1)
    elif tr > 3.5:
        score -= min(0.05, (tr - 3.5) * 0.02)

    # Smoothness penalty (jerky movements)
    es = f["ecc_smoothness_avg"]
    cs = f["con_smoothness_avg"]
    if es > 3.0:
        score -= min(0.08, (es - 3.0) * 0.02)
    if cs > 4.0:
        score -= min(0.08, (cs - 4.0) * 0.02)

    # === 5. LOCKOUT QUALITY (0.10 weight) ===
    ldk = f["lockout_deficit_knee_avg"]
    ldh = f["lockout_deficit_hip_avg"]

    # Should return close to standing position
    if ldk > 10:
        score -= min(0.10, (ldk - 10) * 0.008)
    if ldh > 12:
        score -= min(0.08, (ldh - 12) * 0.006)

    # === 6. CONSISTENCY (0.15 weight) ===
    kb_std = f["knee_bottom_std"]
    hb_std = f["hip_bottom_std"]
    bb_std = f["back_bottom_std"]
    kr_std = f["knee_rom_std"]
    tr_std = f["tempo_ratio_std"]

    if kb_std > 5:
        score -= min(0.06, (kb_std - 5) * 0.005)
    if hb_std > 6:
        score -= min(0.05, (hb_std - 6) * 0.004)
    if bb_std > 4:
        score -= min(0.05, (bb_std - 4) * 0.005)
    if kr_std > 8:
        score -= min(0.04, (kr_std - 8) * 0.004)
    if tr_std > 0.4:
        score -= min(0.04, (tr_std - 0.4) * 0.05)

    return round(max(0.0, min(1.0, score)), 4)


def generate_sample():
    """Generate a single synthetic sample with realistic biomechanics."""
    form_quality = np.random.choice(
        ['excellent', 'good', 'fair', 'poor'],
        p=[0.18, 0.32, 0.28, 0.22]
    )

    if form_quality == 'excellent':
        knee_standing = np.random.uniform(155, 175)
        knee_bottom = np.random.uniform(50, 85)
        hip_standing = np.random.uniform(155, 175)
        hip_bottom = np.random.uniform(42, 72)
        back_standing = np.random.uniform(3, 12)
        back_bottom = np.random.uniform(28, 42)
        ecc_dur = np.random.uniform(1.0, 2.5)
        con_dur = np.random.uniform(0.8, 1.8)
        lockout_deficit_k = np.random.uniform(0, 6)
        lockout_deficit_h = np.random.uniform(0, 7)
        ecc_smooth = np.random.uniform(0.5, 2.5)
        con_smooth = np.random.uniform(0.5, 3.0)
        consistency_noise = np.random.uniform(0, 4)

    elif form_quality == 'good':
        knee_standing = np.random.uniform(150, 175)
        knee_bottom = np.random.uniform(55, 95)
        hip_standing = np.random.uniform(148, 175)
        hip_bottom = np.random.uniform(45, 85)
        back_standing = np.random.uniform(4, 15)
        back_bottom = np.random.uniform(25, 50)
        ecc_dur = np.random.uniform(0.8, 3.0)
        con_dur = np.random.uniform(0.6, 2.2)
        lockout_deficit_k = np.random.uniform(1, 12)
        lockout_deficit_h = np.random.uniform(1, 14)
        ecc_smooth = np.random.uniform(1.0, 3.5)
        con_smooth = np.random.uniform(1.0, 4.0)
        consistency_noise = np.random.uniform(1, 8)

    elif form_quality == 'fair':
        knee_standing = np.random.uniform(140, 175)
        knee_bottom = np.random.uniform(60, 115)
        hip_standing = np.random.uniform(135, 175)
        hip_bottom = np.random.uniform(50, 105)
        back_standing = np.random.uniform(5, 20)
        back_bottom = np.random.uniform(18, 62)
        ecc_dur = np.random.uniform(0.5, 3.5)
        con_dur = np.random.uniform(0.4, 3.0)
        lockout_deficit_k = np.random.uniform(3, 20)
        lockout_deficit_h = np.random.uniform(3, 22)
        ecc_smooth = np.random.uniform(1.5, 5.0)
        con_smooth = np.random.uniform(1.5, 5.5)
        consistency_noise = np.random.uniform(4, 15)

    else:  # poor
        knee_standing = np.random.uniform(130, 175)
        knee_bottom = np.random.uniform(70, 135)
        hip_standing = np.random.uniform(120, 175)
        hip_bottom = np.random.uniform(40, 130)
        back_standing = np.random.uniform(5, 25)
        back_bottom = np.random.uniform(10, 78)
        ecc_dur = np.random.uniform(0.3, 4.5)
        con_dur = np.random.uniform(0.3, 5.0)
        lockout_deficit_k = np.random.uniform(5, 35)
        lockout_deficit_h = np.random.uniform(5, 35)
        ecc_smooth = np.random.uniform(2.0, 7.0)
        con_smooth = np.random.uniform(2.0, 8.0)
        consistency_noise = np.random.uniform(8, 25)

    knee_rom = knee_standing - knee_bottom
    hip_rom = hip_standing - hip_bottom
    back_delta = back_bottom - back_standing
    knee_lockout = knee_standing - lockout_deficit_k
    hip_lockout = hip_standing - lockout_deficit_h
    back_lockout = back_standing + np.random.uniform(-2, 3)
    tempo_ratio = ecc_dur / con_dur if con_dur > 0 else 1.0
    depth_ratio = knee_bottom / knee_standing if knee_standing > 0 else 0.5
    descent_rate = knee_rom / ecc_dur if ecc_dur > 0 else 0
    ascent_rate = (knee_lockout - knee_bottom) / con_dur if con_dur > 0 else 0
    total_duration = ecc_dur + con_dur

    # Consistency stds
    kb_std = np.random.uniform(0, consistency_noise * 0.6)
    hb_std = np.random.uniform(0, consistency_noise * 0.7)
    bb_std = np.random.uniform(0, consistency_noise * 0.5)
    kr_std = np.random.uniform(0, consistency_noise * 0.8)
    tr_std = np.random.uniform(0, consistency_noise * 0.04)

    row = {
        "knee_bottom_avg": round(knee_bottom, 4),
        "hip_bottom_avg": round(hip_bottom, 4),
        "back_bottom_avg": round(back_bottom, 4),
        "knee_standing_avg": round(knee_standing, 4),
        "hip_standing_avg": round(hip_standing, 4),
        "back_standing_avg": round(back_standing, 4),
        "knee_lockout_avg": round(knee_lockout, 4),
        "hip_lockout_avg": round(hip_lockout, 4),
        "back_lockout_avg": round(max(0, back_lockout), 4),
        "knee_rom_avg": round(max(0, knee_rom), 4),
        "hip_rom_avg": round(max(0, hip_rom), 4),
        "ecc_duration_avg": round(ecc_dur, 4),
        "con_duration_avg": round(con_dur, 4),
        "tempo_ratio_avg": round(tempo_ratio, 4),
        "descent_rate_avg": round(descent_rate, 4),
        "ascent_rate_avg": round(ascent_rate, 4),
        "depth_ratio_avg": round(depth_ratio, 4),
        "back_delta_avg": round(back_delta, 4),
        "lockout_deficit_knee_avg": round(lockout_deficit_k, 4),
        "lockout_deficit_hip_avg": round(lockout_deficit_h, 4),
        "ecc_smoothness_avg": round(ecc_smooth, 4),
        "con_smoothness_avg": round(con_smooth, 4),
        "knee_bottom_std": round(kb_std, 4),
        "hip_bottom_std": round(hb_std, 4),
        "back_bottom_std": round(bb_std, 4),
        "knee_rom_std": round(kr_std, 4),
        "tempo_ratio_std": round(tr_std, 4),
    }

    row["score"] = calculate_squat_score(row)
    return row


def load_and_validate(csv_path=None):
    if csv_path is None:
        csv_path = os.path.join(os.path.dirname(__file__), "squat_dataset.csv")
    df = pd.read_csv(csv_path)
    missing = [c for c in ALL_COLS if c not in df.columns]
    if missing:
        raise ValueError(f"Missing columns: {missing}")
    if df[ALL_COLS].isnull().any().any():
        raise ValueError(f"Null values found")
    if not df[TARGET_COL].between(0, 1).all():
        raise ValueError("Scores outside [0, 1]")
    return df


def summary(csv_path=None):
    df = load_and_validate(csv_path)
    print(f"Samples: {len(df)}")
    print(f"\nScore distribution:")
    print(f"  Mean:   {df[TARGET_COL].mean():.4f}")
    print(f"  Median: {df[TARGET_COL].median():.4f}")
    print(f"  Std:    {df[TARGET_COL].std():.4f}")
    bins = [0, 0.3, 0.5, 0.7, 0.9, 1.01]
    labels = ["bad", "poor", "fair", "good", "excellent"]
    df["bucket"] = pd.cut(df[TARGET_COL], bins=bins, labels=labels, right=False)
    print(f"\n{df['bucket'].value_counts().sort_index()}")
    print(f"\nFeature ranges:")
    for col in FEATURE_COLS:
        print(f"  {col:>28s}: [{df[col].min():.2f}, {df[col].max():.2f}]  mean={df[col].mean():.2f}")


if __name__ == "__main__":
    np.random.seed(42)
    N = 25000
    rows = [generate_sample() for _ in range(N)]
    df = pd.DataFrame(rows)

    print(f"Dataset generated: {len(df)} samples")
    print(f"\nScore distribution:")
    print(df['score'].describe())
    print(f"\nScore bins:")
    print(pd.cut(df['score'], bins=[0, 0.3, 0.5, 0.7, 0.9, 1.01],
                 labels=['Bad', 'Poor', 'Fair', 'Good', 'Excellent']).value_counts().sort_index())

    out_path = os.path.join(os.path.dirname(__file__), "squat_dataset.csv")
    df.to_csv(out_path, index=False)
    print(f"\nSaved to {out_path}")
    summary(out_path)