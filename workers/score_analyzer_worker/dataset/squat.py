import numpy as np
import pandas as pd

def calculate_squat_score(knee_min, knee_avg, knee_max, hip_min, hip_avg, hip_max, back_min, back_avg, back_max):
    """
    Calculate squat score based on exercise science principles.
    All angles represent the BOTTOM POSITION of each rep.
    min/max/avg represent consistency across reps in the set.
    
    Back angle = trunk lean from vertical (0° = upright, 90° = horizontal).
    During a proper squat, 30-50° forward lean is normal (high-bar).
    
    Score ranges from 0 (poor form) to 1 (perfect form).
    """
    score = 1.0
    
    # KNEE ANGLE SCORING (at bottom of squat)
    # Optimal depth: 70-90 degrees at bottom
    # Above 95: insufficient depth (not reaching parallel)
    if knee_avg > 95:
        score -= min(0.40, (knee_avg - 95) / 60)
    
    # Consistency penalty: large variation between reps
    knee_range = knee_max - knee_min
    if knee_range > 20:
        score -= min(0.25, (knee_range - 20) / 80)
    
    # HIP ANGLE SCORING (at bottom of squat)
    # Optimal hip depth: 60-90 degrees
    if hip_avg < 55:
        score -= min(0.25, (55 - hip_avg) / 70)
    elif hip_avg > 100:
        score -= min(0.35, (hip_avg - 100) / 60)
    
    # Hip consistency
    hip_range = hip_max - hip_min
    if hip_range > 25:
        score -= min(0.20, (hip_range - 25) / 70)
    
    # BACK ANGLE SCORING (trunk lean from vertical at bottom of squat)
    # Normal range: 30-50° for high-bar squat
    # Below 20°: unnaturally upright (likely tracking error or very unusual form)
    # Above 55°: excessive forward lean (injury risk)
    # Above 70°: dangerous ("good morning" squat)
    
    if back_avg < 20:
        score -= 0.10  # Unusually upright
    elif back_avg > 55:
        score -= min(0.40, (back_avg - 55) / 30)  # Excessive forward lean
    
    # Back consistency (important for safety — spine should stay stable)
    back_range = back_max - back_min
    if back_range > 15:
        score -= min(0.30, (back_range - 15) / 40)
    
    if back_max > 70:
        score -= 0.30  # Dangerous maximum forward lean
    
    # COORDINATION SCORING
    consistency_diff = abs(knee_range - hip_range)
    if consistency_diff > 15:
        score -= min(0.15, (consistency_diff - 15) / 60)
    
    return max(0, min(1, score))

np.random.seed(42)
rows = []

for _ in range(10000):
    form_quality = np.random.choice(['excellent', 'good', 'fair', 'poor'], p=[0.15, 0.45, 0.25, 0.15])
    
    if form_quality == 'excellent':
        knee_avg = np.random.uniform(75, 88)
        knee_range = np.random.uniform(3, 12)
        hip_avg = np.random.uniform(65, 85)
        hip_range = np.random.uniform(3, 15)
        back_avg = np.random.uniform(30, 45)
        back_range = np.random.uniform(3, 10)
        
    elif form_quality == 'good':
        knee_avg = np.random.uniform(70, 95)
        knee_range = np.random.uniform(8, 22)
        hip_avg = np.random.uniform(60, 95)
        hip_range = np.random.uniform(8, 25)
        back_avg = np.random.uniform(25, 50)
        back_range = np.random.uniform(5, 15)
        
    elif form_quality == 'fair':
        knee_avg = np.random.uniform(60, 110)
        knee_range = np.random.uniform(15, 35)
        hip_avg = np.random.uniform(50, 110)
        hip_range = np.random.uniform(15, 35)
        back_avg = np.random.uniform(20, 60)
        back_range = np.random.uniform(10, 25)
        
    else:  # poor
        knee_avg = np.random.uniform(50, 120)
        knee_range = np.random.uniform(20, 50)
        hip_avg = np.random.uniform(40, 120)
        hip_range = np.random.uniform(20, 50)
        back_avg = np.random.uniform(15, 75)
        back_range = np.random.uniform(15, 35)
    
    # Generate min/max from avg and range
    knee_min = knee_avg - knee_range / 2
    knee_max = knee_avg + knee_range / 2
    hip_min = hip_avg - hip_range / 2
    hip_max = hip_avg + hip_range / 2
    back_min = back_avg - back_range / 2
    back_max = back_avg + back_range / 2
    
    # Ensure realistic bounds
    knee_min = max(40, knee_min)
    knee_max = min(140, knee_max)
    hip_min = max(30, hip_min)
    hip_max = min(140, hip_max)
    back_min = max(0, back_min)
    back_max = min(90, back_max)
    
    score = calculate_squat_score(
        knee_min, knee_avg, knee_max,
        hip_min, hip_avg, hip_max,
        back_min, back_avg, back_max
    )
    
    rows.append([
        knee_min, knee_avg, knee_max,
        hip_min, hip_avg, hip_max,
        back_min, back_avg, back_max,
        score
    ])

df = pd.DataFrame(rows, columns=[
    "knee_min","knee_avg","knee_max",
    "hip_min","hip_avg","hip_max",
    "back_min","back_avg","back_max",
    "score"
])

# Add some basic statistics
print(f"Dataset generated: {len(df)} samples")
print(f"\nScore distribution:")
print(df['score'].describe())
print(f"\nScore bins:")
print(pd.cut(df['score'], bins=[0, 0.5, 0.7, 0.85, 1.0], labels=['Poor', 'Fair', 'Good', 'Excellent']).value_counts())

df.to_csv("squat_dataset.csv", index=False)
print("\nDataset saved to squat_dataset.csv")