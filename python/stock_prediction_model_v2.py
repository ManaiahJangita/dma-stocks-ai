import argparse
import json
from pathlib import Path
from typing import Any, Dict, List, Optional

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor, RandomForestClassifier, RandomForestRegressor
from sklearn.preprocessing import StandardScaler

MODEL_DIR = Path(__file__).resolve().parent / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)


def _safe_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


class StockPredictionModel:
    def __init__(self, model_name: str = "xgboost"):
        self.model_name = model_name
        self.model = None
        self.scaler = StandardScaler()
        self.feature_columns: List[str] = []

    def build_features(self, df: pd.DataFrame) -> pd.DataFrame:
        if df.empty:
            return pd.DataFrame()

        required = {"symbol", "created_date", "open", "high", "low", "close", "volume"}
        missing = required - set(df.columns)
        if missing:
            raise ValueError(f"Missing required columns: {sorted(missing)}")

        df = df.copy().sort_values(["symbol", "created_date"]).reset_index(drop=True)
        df["close"] = pd.to_numeric(df["close"], errors="coerce")
        df["open"] = pd.to_numeric(df["open"], errors="coerce")
        df["high"] = pd.to_numeric(df["high"], errors="coerce")
        df["low"] = pd.to_numeric(df["low"], errors="coerce")
        df["volume"] = pd.to_numeric(df["volume"], errors="coerce").fillna(0)
        df["created_date"] = pd.to_datetime(df["created_date"])

        records = []
        for symbol, symbol_df in df.groupby("symbol", sort=False):
            symbol_df = symbol_df.sort_values("created_date").reset_index(drop=True)
            for i in range(1, len(symbol_df)):
                current = symbol_df.iloc[i]
                previous = symbol_df.iloc[i - 1]
                day_5 = symbol_df.iloc[max(0, i - 5)]
                day_10 = symbol_df.iloc[max(0, i - 10)]
                day_20 = symbol_df.iloc[max(0, i - 20)]

                close = _safe_float(current.get("close"), 0.0)
                prev_close = _safe_float(previous.get("close"), 0.0)
                if close <= 0 or prev_close <= 0:
                    continue

                volume_avg = symbol_df.iloc[max(0, i - 20):i]["volume"].mean() if i > 0 else 0.0
                volume_signal = ((current["volume"] / volume_avg) - 1.0) * 100.0 if volume_avg else 0.0

                momentum_1 = ((close / prev_close) - 1.0) * 100.0
                momentum_5 = ((close / _safe_float(day_5.get("close"), prev_close)) - 1.0) * 100.0 if _safe_float(day_5.get("close"), 0.0) > 0 else 0.0
                momentum_10 = ((close / _safe_float(day_10.get("close"), prev_close)) - 1.0) * 100.0 if _safe_float(day_10.get("close"), 0.0) > 0 else 0.0
                momentum_20 = ((close / _safe_float(day_20.get("close"), prev_close)) - 1.0) * 100.0 if _safe_float(day_20.get("close"), 0.0) > 0 else 0.0
                range_pct = ((current["high"] - current["low"]) / close) * 100.0 if close else 0.0
                open_gap = ((current["open"] / prev_close) - 1.0) * 100.0
                close_gap = ((close / current["open"]) - 1.0) * 100.0 if current["open"] else 0.0
                volatility = abs(momentum_1) + abs(momentum_5)

                future_high = symbol_df.iloc[min(i + 1, len(symbol_df) - 1)]["high"] if i + 1 < len(symbol_df) else current["high"]
                target_high = _safe_float(future_high, close)

                records.append({
                    "symbol": symbol,
                    "created_date": current["created_date"],
                    "momentum_1": momentum_1,
                    "momentum_5": momentum_5,
                    "momentum_10": momentum_10,
                    "momentum_20": momentum_20,
                    "range_pct": range_pct,
                    "open_gap": open_gap,
                    "close_gap": close_gap,
                    "volume_signal": volume_signal,
                    "volatility": volatility,
                    "target_high": float(target_high),
                })

        features_df = pd.DataFrame(records)
        self.feature_columns = [
            "momentum_1", "momentum_5", "momentum_10", "momentum_20",
            "range_pct", "open_gap", "close_gap", "volume_signal", "volatility"
        ]
        return features_df

    def train(self, df: pd.DataFrame):
        features_df = self.build_features(df)
        if features_df.empty:
            raise ValueError("No valid training rows found")

        X = features_df[self.feature_columns]
        y = features_df["target_high"].astype(float)

        if len(X) < 20:
            raise ValueError("Need at least 20 valid rows to train")

        train_size = max(5, int(len(X) * 0.8))
        X_train = X.iloc[:train_size]
        y_train = y.iloc[:train_size]

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
                self.model = GradientBoostingRegressor(random_state=42, n_estimators=200)
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
                self.model = GradientBoostingRegressor(random_state=42, n_estimators=200)
        elif self.model_name.lower() == "random_forest":
            self.model = RandomForestRegressor(n_estimators=300, random_state=42)
        else:
            self.model = GradientBoostingRegressor(random_state=42, n_estimators=200)

        self.scaler.fit(X_train)
        X_scaled = self.scaler.transform(X_train)
        self.model.fit(X_scaled, y_train)
        return self

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
        current_close = _safe_float(df.iloc[-1]["close"], 0.0)
        current_high = _safe_float(df.iloc[-1]["high"], 0.0)

        if current_close > 0:
            high_change_pct = ((predicted_high - current_close) / current_close) * 100.0
        else:
            high_change_pct = 0.0

        return {
            "symbol": str(latest.get("symbol", "")),
            "predicted_high": round(float(predicted_high), 4),
            "predicted_high_change_pct": round(high_change_pct, 2),
            "current_close": round(current_close, 4),
            "current_high": round(current_high, 4),
        }

    def save_model(self, model_path: Optional[str] = None):
        if model_path is None:
            model_path = str(MODEL_DIR / "stock_prediction_model.joblib")
        payload = {
            "model": self.model,
            "scaler": self.scaler,
            "feature_columns": self.feature_columns,
            "model_name": self.model_name,
        }
        joblib.dump(payload, model_path)
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
    if model_path:
        model = StockPredictionModel.load_model(model_path)
    else:
        model = StockPredictionModel(model_name=model_name)
        model.train(df)
    return model.predict(df)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Stock prediction model")
    parser.add_argument("command", choices=["train", "predict"], help="Operation to run")
    parser.add_argument("--input", help="CSV input file path", required=True)
    parser.add_argument("--model-name", default="xgboost", help="Model name to use")
    parser.add_argument("--model-path", default=None, help="Pretrained model path")
    return parser


def main() -> None:
    args = _build_parser().parse_args()
    if args.command == "train":
        result = train_and_save_model(args.input, model_name=args.model_name)
        print(json.dumps(result))
        return

    result = predict_from_csv(args.input, model_path=args.model_path, model_name=args.model_name)
    print(json.dumps(result))


if __name__ == "__main__":
    main()

