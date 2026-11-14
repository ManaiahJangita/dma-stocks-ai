import json
import math
import pickle
from pathlib import Path
from typing import List, Dict, Any, Optional

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, RandomForestRegressor, GradientBoostingRegressor
from sklearn.metrics import accuracy_score, classification_report, precision_score, recall_score, f1_score, mean_squared_error, mean_absolute_error, r2_score
from sklearn.model_selection import TimeSeriesSplit
from sklearn.preprocessing import StandardScaler

MODEL_DIR = Path(__file__).resolve().parent / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)


class StockPredictionModel:
    def __init__(self, model_name: str = "xgboost"):
        self.model_name = model_name
        self.model = None
        self.scaler = StandardScaler()
        self.feature_columns: List[str] = []

    def build_features(self, df: pd.DataFrame) -> pd.DataFrame:
        df = df.copy().sort_values(["symbol", "created_date"]).reset_index(drop=True)
        df["close"] = pd.to_numeric(df["close"], errors="coerce")
        df["open"] = pd.to_numeric(df["open"], errors="coerce")
        df["high"] = pd.to_numeric(df["high"], errors="coerce")
        df["low"] = pd.to_numeric(df["low"], errors="coerce")
        df["volume"] = pd.to_numeric(df["volume"], errors="coerce").fillna(0)
        df["created_date"] = pd.to_datetime(df["created_date"])

        grouped = df.groupby("symbol", group_keys=False)
        result_frames = []

        for _, symbol_df in grouped:
            symbol_df = symbol_df.sort_values("created_date").reset_index(drop=True)
            for i in range(1, len(symbol_df)):
                row = symbol_df.iloc[i]
                prev = symbol_df.iloc[i - 1]
                prev5 = symbol_df.iloc[max(0, i - 5)]
                prev10 = symbol_df.iloc[max(0, i - 10)]
                prev20 = symbol_df.iloc[max(0, i - 20)]

                close = row["close"]
                prev_close = prev["close"]
                if pd.isna(close) or pd.isna(prev_close) or prev_close == 0:
                    continue

                momentum_1 = ((close / prev_close) - 1.0) * 100.0
                momentum_5 = ((close / prev5["close"]) - 1.0) * 100.0 if pd.notna(prev5["close"]) and prev5["close"] != 0 else 0.0
                momentum_10 = ((close / prev10["close"]) - 1.0) * 100.0 if pd.notna(prev10["close"]) and prev10["close"] != 0 else 0.0
                momentum_20 = ((close / prev20["close"]) - 1.0) * 100.0 if pd.notna(prev20["close"]) and prev20["close"] != 0 else 0.0
                range_pct = ((row["high"] - row["low"]) / close) * 100.0 if close else 0.0
                open_gap = ((row["open"] / prev_close) - 1.0) * 100.0
                close_gap = ((close / row["open"]) - 1.0) * 100.0 if row["open"] else 0.0
                volume_avg = symbol_df.iloc[max(0, i - 20):i]["volume"].mean() if i > 0 else 0
                volume_signal = ((row["volume"] / volume_avg) - 1.0) * 100.0 if volume_avg else 0.0

                future_high = symbol_df.iloc[min(i + 1, len(symbol_df) - 1)]["high"] if i + 1 < len(symbol_df) else row["high"]
                target_high = float(future_high)

                feature_row = {
                    "symbol": row["symbol"],
                    "created_date": row["created_date"],
                    "momentum_1": momentum_1,
                    "momentum_5": momentum_5,
                    "momentum_10": momentum_10,
                    "momentum_20": momentum_20,
                    "range_pct": range_pct,
                    "open_gap": open_gap,
                    "close_gap": close_gap,
                    "volume_signal": volume_signal,
                    "volatility": abs(momentum_1) + abs(momentum_5),
                    "target_high": target_high,
                }
                result_frames.append(feature_row)

        features_df = pd.DataFrame(result_frames)
        if features_df.empty:
            return pd.DataFrame(columns=[
                "symbol", "created_date", "momentum_1", "momentum_5", "momentum_10",
                "momentum_20", "range_pct", "open_gap", "close_gap", "volume_signal",
                "volatility", "target_high"
            ])

        feature_columns = [
            "momentum_1", "momentum_5", "momentum_10", "momentum_20",
            "range_pct", "open_gap", "close_gap", "volume_signal", "volatility"
        ]
        self.feature_columns = feature_columns
        return features_df

    def train(self, df: pd.DataFrame):
        if df.empty:
            raise ValueError("Input dataframe is empty")

        features_df = self.build_features(df)
        if features_df.empty:
            raise ValueError("Not enough valid rows to build training data")

        X = features_df[self.feature_columns]
        y = features_df["target_high"].astype(float)

        if len(X) < 20:
            raise ValueError("Not enough samples to train the model")

        x_train = X.iloc[:-min(10, len(X) // 5)]
        y_train = y.iloc[:-min(10, len(X) // 5)]
        x_val = X.iloc[len(x_train):]
        y_val = y.iloc[len(y_train):]

        if len(x_train) < 5:
            raise ValueError("Not enough training rows")

        X_scaled = self.scaler.fit_transform(x_train)
        if self.model_name.lower() in {"xgboost", "lightgbm", "random_forest", "gradient_boost"}:
            if self.model_name.lower() == "xgboost":
                try:
                    import xgboost as xgb
                    self.model = xgb.XGBRegressor(
                        n_estimators=200,
                        max_depth=5,
                        learning_rate=0.05,
                        subsample=0.9,
                        colsample_bytree=0.8,
                        objective="reg:squarederror",
                        random_state=42,
                    )
                except Exception:
                    self.model = RandomForestRegressor(n_estimators=300, random_state=42)
            elif self.model_name.lower() == "lightgbm":
                try:
                    import lightgbm as lgb
                    self.model = lgb.LGBMRegressor(
                        n_estimators=200,
                        learning_rate=0.05,
                        max_depth=5,
                        num_leaves=25,
                        random_state=42,
                    )
                except Exception:
                    self.model = GradientBoostingRegressor(random_state=42)
            elif self.model_name.lower() == "random_forest":
                self.model = RandomForestRegressor(n_estimators=300, random_state=42)
            else:
                self.model = GradientBoostingRegressor(random_state=42)
        else:
            self.model = GradientBoostingRegressor(random_state=42)

        self.model.fit(X_scaled, y_train)

        # Store validation data for evaluation
        self.val_predictions = self.model.predict(self.scaler.transform(x_val))
        self.val_actuals = y_val.values

        return self

    def get_validation_metrics(self) -> Dict[str, float]:
        """Calculate validation metrics comparing predictions vs actuals"""
        if not hasattr(self, 'val_predictions') or not hasattr(self, 'val_actuals'):
            return {}

        mae = mean_absolute_error(self.val_actuals, self.val_predictions)
        rmse = np.sqrt(mean_squared_error(self.val_actuals, self.val_predictions))
        r2 = r2_score(self.val_actuals, self.val_predictions)
        mape = np.mean(np.abs((self.val_actuals - self.val_predictions) / self.val_actuals)) * 100

        return {
            "mae": float(mae),
            "rmse": float(rmse),
            "r2_score": float(r2),
            "mape_percent": float(mape),
            "validation_samples": len(self.val_actuals),
        }

    def evaluate_predictions(self, actual_df: pd.DataFrame) -> Dict[str, Any]:
        """Compare predictions vs actual HIGH values from a test dataset"""
        if self.model is None:
            raise ValueError("Model is not trained")

        features_df = self.build_features(actual_df)
        if features_df.empty:
            return {"error": "No valid rows in test data"}

        X = features_df[self.feature_columns]
        X_scaled = self.scaler.transform(X)
        predictions = self.model.predict(X_scaled)
        actuals = features_df["target_high"].values

        mae = mean_absolute_error(actuals, predictions)
        rmse = np.sqrt(mean_squared_error(actuals, predictions))
        r2 = r2_score(actuals, predictions)
        mape = np.mean(np.abs((actuals - predictions) / np.where(actuals != 0, actuals, 1))) * 100

        # Calculate directional accuracy
        prediction_direction = predictions > (actuals * 1.01)
        actual_direction = actuals > (actuals * 1.01)
        directional_accuracy = np.mean(prediction_direction == actual_direction) * 100

        # Prediction error analysis
        errors = np.abs(actuals - predictions)
        error_within_1pct = np.mean(errors <= actuals * 0.01) * 100
        error_within_2pct = np.mean(errors <= actuals * 0.02) * 100
        error_within_5pct = np.mean(errors <= actuals * 0.05) * 100

        return {
            "evaluation_summary": {
                "mae": round(float(mae), 4),
                "rmse": round(float(rmse), 4),
                "r2_score": round(float(r2), 4),
                "mape_percent": round(float(mape), 2),
                "directional_accuracy_percent": round(float(directional_accuracy), 2),
            },
            "error_distribution": {
                "within_1_percent": round(float(error_within_1pct), 2),
                "within_2_percent": round(float(error_within_2pct), 2),
                "within_5_percent": round(float(error_within_5pct), 2),
            },
            "total_test_samples": len(actuals),
        }

    def predict(self, df: pd.DataFrame) -> Dict[str, Any]:
        if self.model is None:
            raise ValueError("Model is not trained")

        features_df = self.build_features(df)
        if features_df.empty:
            return {"symbol": None, "predicted_high": None, "predicted_high_change_pct": 0.0}

        X = features_df[self.feature_columns]
        X_scaled = self.scaler.transform(X)
        predicted_high = self.model.predict(X_scaled)[-1]

        latest = features_df.iloc[-1]
        current_close = float(df.iloc[-1]["close"])
        current_high = float(df.iloc[-1]["high"])

        if current_close > 0:
            high_change_pct = ((predicted_high - current_close) / current_close) * 100.0
        else:
            high_change_pct = 0.0

        result = {
            "symbol": latest.get("symbol"),
            "predicted_high": round(float(predicted_high), 4),
            "predicted_high_change_pct": round(high_change_pct, 2),
            "current_close": round(current_close, 4),
            "current_high": round(current_high, 4),
        }

        # Add validation metrics if available
        metrics = self.get_validation_metrics()
        if metrics:
            result["validation_metrics"] = metrics

        return result

    def evaluate_predictions(self, actual_df: pd.DataFrame) -> Dict[str, Any]:
        """Compare predictions vs actual HIGH values from a test dataset"""
        if self.model is None:
            raise ValueError("Model is not trained")

        features_df = self.build_features(actual_df)
        if features_df.empty:
            return {"error": "No valid rows in test data"}

        X = features_df[self.feature_columns]
        X_scaled = self.scaler.transform(X)
        predictions = self.model.predict(X_scaled)
        actuals = features_df["target_high"].values

        mae = mean_absolute_error(actuals, predictions)
        rmse = np.sqrt(mean_squared_error(actuals, predictions))
        r2 = r2_score(actuals, predictions)
        mape = np.mean(np.abs((actuals - predictions) / np.where(actuals != 0, actuals, 1))) * 100

        # Calculate directional accuracy
        prediction_direction = predictions > (actuals * 1.01)
        actual_direction = actuals > (actuals * 1.01)
        directional_accuracy = np.mean(prediction_direction == actual_direction) * 100

        # Prediction error analysis
        errors = np.abs(actuals - predictions)
        error_within_1pct = np.mean(errors <= actuals * 0.01) * 100
        error_within_2pct = np.mean(errors <= actuals * 0.02) * 100
        error_within_5pct = np.mean(errors <= actuals * 0.05) * 100

        return {
            "evaluation_summary": {
                "mae": round(float(mae), 4),
                "rmse": round(float(rmse), 4),
                "r2_score": round(float(r2), 4),
                "mape_percent": round(float(mape), 2),
                "directional_accuracy_percent": round(float(directional_accuracy), 2),
            },
            "error_distribution": {
                "within_1_percent": round(float(error_within_1pct), 2),
                "within_2_percent": round(float(error_within_2pct), 2),
                "within_5_percent": round(float(error_within_5pct), 2),
            },
            "total_test_samples": len(actuals),
        }

    def save_model(self, model_path: Optional[str] = None):
        if model_path is None:
            model_path = str(MODEL_DIR / "stock_prediction_model.joblib")
        joblib.dump({
            "model": self.model,
            "scaler": self.scaler,
            "feature_columns": self.feature_columns,
            "model_name": self.model_name,
        }, model_path)
        return model_path

    @classmethod
    def load_model(cls, model_path: Optional[str] = None):
        if model_path is None:
            model_path = str(MODEL_DIR / "stock_prediction_model.joblib")
        payload = joblib.load(model_path)
        instance = cls(payload.get("model_name", "xgboost"))
        instance.model = payload["model"]
        instance.scaler = payload["scaler"]
        instance.feature_columns = payload["feature_columns"]
        return instance


def train_and_save_model(csv_path: str, model_name: str = "xgboost") -> Dict[str, Any]:
    df = pd.read_csv(csv_path)
    model = StockPredictionModel(model_name=model_name)
    model.train(df)
    model_path = model.save_model()
    return {
        "status": "success",
        "model_path": model_path,
        "model_name": model_name,
        "features": model.feature_columns,
    }


def predict_from_csv(csv_path: str, model_path: Optional[str] = None, model_name: str = "xgboost") -> Dict[str, Any]:
    df = pd.read_csv(csv_path)
    model = StockPredictionModel.load_model(model_path) if model_path else StockPredictionModel(model_name=model_name)
    if model.model is None:
        model.train(df)
    return model.predict(df)


if __name__ == "__main__":
    example_csv = "data/stock_history.csv"
    print(json.dumps(train_and_save_model(example_csv), indent=2))

