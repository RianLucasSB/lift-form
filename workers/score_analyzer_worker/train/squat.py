import os
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.ensemble import GradientBoostingRegressor
import joblib

DATASET_PATH = os.path.join(os.path.dirname(__file__), "..", "dataset", "squat_dataset.csv")
MODEL_OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "squat_score_model.joblib")

FEATURES = [
    "knee_min", "knee_avg", "knee_max",
    "hip_min", "hip_avg", "hip_max",
    "back_min", "back_avg", "back_max",
]
TARGET = "score"


def load_data(path: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    assert all(col in df.columns for col in FEATURES + [TARGET]), "Missing columns in dataset"
    return df


def train():
    print("Loading dataset...")
    df = load_data(DATASET_PATH)
    print(f"  Samples: {len(df)}")

    X = df[FEATURES].values
    y = df[TARGET].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    print(f"  Train: {len(X_train)} | Test: {len(X_test)}")

    model = GradientBoostingRegressor(
        n_estimators=500,
        max_depth=5,
        learning_rate=0.05,
        subsample=0.8,
        min_samples_split=10,
        min_samples_leaf=5,
        random_state=42,
    )

    print("\nTraining model...")
    model.fit(X_train, y_train)

    # Evaluate
    y_pred_train = np.clip(model.predict(X_train), 0, 1)
    y_pred_test = np.clip(model.predict(X_test), 0, 1)

    print("\n--- Train Metrics ---")
    print(f"  MAE:  {mean_absolute_error(y_train, y_pred_train):.4f}")
    print(f"  RMSE: {np.sqrt(mean_squared_error(y_train, y_pred_train)):.4f}")
    print(f"  R²:   {r2_score(y_train, y_pred_train):.4f}")

    print("\n--- Test Metrics ---")
    print(f"  MAE:  {mean_absolute_error(y_test, y_pred_test):.4f}")
    print(f"  RMSE: {np.sqrt(mean_squared_error(y_test, y_pred_test)):.4f}")
    print(f"  R²:   {r2_score(y_test, y_pred_test):.4f}")

    # Feature importance
    print("\n--- Feature Importance ---")
    importances = model.feature_importances_
    for name, imp in sorted(zip(FEATURES, importances), key=lambda x: -x[1]):
        print(f"  {name:12s}: {imp:.4f}")

    # Save model
    os.makedirs(os.path.dirname(MODEL_OUTPUT_PATH), exist_ok=True)
    joblib.dump(model, MODEL_OUTPUT_PATH)
    print(f"\nModel saved to {MODEL_OUTPUT_PATH}")


if __name__ == "__main__":
    train()
