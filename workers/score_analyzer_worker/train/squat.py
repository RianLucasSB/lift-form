import os
import pandas as pd
import numpy as np
import joblib
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

from ..dataset.squat import FEATURE_COLS


def load_dataset(csv_path: str) -> tuple[pd.DataFrame, pd.Series]:
    df = pd.read_csv(csv_path)
    X = df[FEATURE_COLS]
    y = df["score"]
    return X, y


def train(csv_path: str, model_output_path: str = "squat_score_model.joblib"):
    X, y = load_dataset(csv_path)
    print(f"Dataset: {len(X)} samples, {len(FEATURE_COLS)} features")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    model = GradientBoostingRegressor(
        n_estimators=300,
        max_depth=5,
        learning_rate=0.05,
        subsample=0.8,
        min_samples_leaf=10,
        random_state=42,
    )

    cv_scores = cross_val_score(model, X_train, y_train, cv=5, scoring="neg_mean_absolute_error")
    print(f"CV MAE: {-cv_scores.mean():.4f} (+/- {cv_scores.std():.4f})")

    model.fit(X_train, y_train)

    y_pred = np.clip(model.predict(X_test), 0.0, 1.0)
    mae = mean_absolute_error(y_test, y_pred)
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    r2 = r2_score(y_test, y_pred)

    print(f"Test MAE:  {mae:.4f}")
    print(f"Test RMSE: {rmse:.4f}")
    print(f"Test R²:   {r2:.4f}")

    importances = sorted(
        zip(FEATURE_COLS, model.feature_importances_),
        key=lambda x: x[1], reverse=True,
    )
    print("\nFeature importances:")
    for name, imp in importances:
        print(f"  {name:>28s}: {imp:.4f}")

    os.makedirs(os.path.dirname(model_output_path) or ".", exist_ok=True)
    joblib.dump(model, model_output_path)
    print(f"\nModel saved to {model_output_path}")

    return model


if __name__ == "__main__":
    dataset_path = os.path.join(
        os.path.dirname(__file__), "..", "dataset", "squat_dataset.csv"
    )
    model_path = os.path.join(
        os.path.dirname(__file__), "..", "models", "squat_score_model.joblib"
    )
    train(dataset_path, model_path)
