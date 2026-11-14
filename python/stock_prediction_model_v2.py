import argparse
import json
from pathlib import Path
from typing import Any, Dict, List, Optional

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier
from sklearn.metrics import accuracy_score, f1_score, precision_score, recall_score
from sklearn.model_selection import TimeSeriesSplit
from sklearn.preprocessing import StandardScaler

MODEL_DIR = Path(__file__).resolve().parent / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)


class StockPredictionModel:
    def __init__(self, model_name: str = "xgboost"):
        self.model_name = (model_name or "xgboost").lower()
        self.model = None
        self.scaler = StandardScaler()
        self.feature_columns: List[str] = []
        self.validation_summary: Dict[str, Any] = {}

    def _safe_float(self, value: Any, default: float = 0.0) -> float:
        try:
            return float(value)
        except (TypeError, ValueError):
            return default

    def _calculate_rsi(self, closes: pd.Series, period: int = 14) -> float:
        if len(closes) < 2:
            return 50.0
        deltas = closes.diff().fillna(0.0)
        gains = deltas.clip(lower=0)
        losses = (-deltas).clip(lower=0)
        avg_gain = gains.rolling(window=period, min_periods=period).mean().iloc[-1]
        avg_loss = losses.rolling(window=period, min_periods=period).mean().iloc[-1]
        if avg_loss == 0:
            return 70.0
        rs = avg_gain / avg_loss
        return 100.0 - (100.0 / (1.0 + rs))

    def _calculate_sma(self, values: pd.Series, period: int) -> float:
        if len(values) < period:
            return float(values.iloc[-1]) if not values.empty else 0.0
        return float(values.tail(period).mean())

    def build_features(self, df: pd.DataFrame) -> pd.DataFrame:
        if df.empty:
            return pd.DataFrame()

        required = {"symbol", "created_date", "open", "high", "low", "close", "volume"}
        missing = required - set(df.columns)
        if missing:
            raise ValueError(f"Missing required columns: {sorted(missing)}")

        df = df.copy().sort_values(["symbol", "created_date"]).reset_index(drop=True)
        for column in ["close", "open", "high", "low", "volume"]:
            df[column] = pd.to_numeric(df[column], errors="coerce").fillna(0.0)
        df["created_date"] = pd.to_datetime(df["created_date"])

        rows: List[Dict[str, Any]] = []
        for symbol, symbol_df in df.groupby("symbol", sort=False):
            symbol_df = symbol_df.sort_values("created_date").reset_index(drop=True)
            for i in range(1, len(symbol_df) - 1):
                current = symbol_df.iloc[i]
                prev = symbol_df.iloc[i - 1]
                prev5 = symbol_df.iloc[max(0, i - 5)]
                prev10 = symbol_df.iloc[max(0, i - 10)]
                prev20 = symbol_df.iloc[max(0, i - 20)]
                next_row = symbol_df.iloc[i + 1]

                prev_close = self._safe_float(prev.get("close"), 0.0)
                close = self._safe_float(current.get("close"), 0.0)
                if close <= 0 or prev_close <= 0:
                    continue

                next_close = self._safe_float(next_row.get("close"), close)
                momentum_1 = ((close / prev_close) - 1.0) * 100.0
                momentum_5 = ((close / self._safe_float(prev5.get("close"), prev_close)) - 1.0) * 100.0 if self._safe_float(prev5.get("close"), 0.0) > 0 else 0.0
                momentum_10 = ((close / self._safe_float(prev10.get("close"), prev_close)) - 1.0) * 100.0 if self._safe_float(prev10.get("close"), 0.0) > 0 else 0.0
                momentum_20 = ((close / self._safe_float(prev20.get("close"), prev_close)) - 1.0) * 100.0 if self._safe_float(prev20.get("close"), 0.0) > 0 else 0.0
                range_pct = ((self._safe_float(current.get("high"), close) - self._safe_float(current.get("low"), close)) / close) * 100.0 if close else 0.0
                open_gap = ((self._safe_float(current.get("open"), close) / prev_close) - 1.0) * 100.0
                close_gap = ((close / self._safe_float(current.get("open"), close)) - 1.0) * 100.0 if self._safe_float(current.get("open"), 0.0) > 0 else 0.0
                volume_avg = symbol_df.iloc[max(0, i - 20):i]["volume"].mean() if i > 0 else 0.0
                volume_signal = ((self._safe_float(current.get("volume"), 0.0) / volume_avg) - 1.0) * 100.0 if volume_avg > 0 else 0.0
                volatility = abs(momentum_1) + abs(momentum_5)

                closes = symbol_df["close"].iloc[max(0, i - 14):i + 1].astype(float)
                rsi_14 = self._calculate_rsi(closes, 14)
                sma_5 = self._calculate_sma(symbol_df.iloc[max(0, i - 5):i + 1]["close"], 5)
                sma_20 = self._calculate_sma(symbol_df.iloc[max(0, i - 20):i + 1]["close"], 20)
                trend_strength = ((close / sma_20) - 1.0) * 100.0 if sma_20 > 0 else 0.0

                next_return_pct = ((next_close - close) / close) * 100.0 if close else 0.0
                label = 1 if next_close > close else 0

                rows.append(
                    {
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
                        "rsi_14": rsi_14,
                        "trend_strength": trend_strength,
                        "label": int(label),
                        "next_return_pct": next_return_pct,
                        "current_close": close,
                        "next_close": next_close,
                    }
                )

        features_df = pd.DataFrame(rows)
        if features_df.empty:
            return pd.DataFrame()

        self.feature_columns = [
            "momentum_1",
            "momentum_5",
            "momentum_10",
            "momentum_20",
            "range_pct",
            "open_gap",
            "close_gap",
            "volume_signal",
            "volatility",
            "rsi_14",
            "trend_strength",
        ]
        return features_df

    def _make_classifier(self):
        if self.model_name == "lightgbm":
            try:
                import lightgbm as lgb

                return lgb.LGBMClassifier(
                    objective="binary",
                    n_estimators=300,
                    learning_rate=0.05,
                    max_depth=5,
                    num_leaves=25,
                    random_state=42,
                    subsample=0.9,
                    colsample_bytree=0.8,
                )
            except Exception:
                pass

        if self.model_name == "random_forest":
            return RandomForestClassifier(n_estimators=500, max_depth=8, random_state=42, min_samples_leaf=2)

        return HistGradientBoostingClassifier(
            loss="log_loss",
            learning_rate=0.05,
            max_depth=8,
            max_iter=300,
            random_state=42,
        )

    def _sanitize_feature_frame(self, features_df: pd.DataFrame) -> pd.DataFrame:
        if features_df.empty:
            return features_df

        safe_features = features_df.copy()
        for column in self.feature_columns:
            if column not in safe_features.columns:
                continue
            safe_features[column] = pd.to_numeric(safe_features[column], errors="coerce")

        safe_features = safe_features.replace([np.inf, -np.inf], np.nan)
        safe_features[self.feature_columns] = safe_features[self.feature_columns].fillna(0.0)
        return safe_features

    def train(self, df: pd.DataFrame):
        if df.empty:
            raise ValueError("Input dataframe is empty")

        features_df = self.build_features(df)
        if features_df.empty:
            raise ValueError("Not enough valid rows to build training data")

        features_df = self._sanitize_feature_frame(features_df)
        X = features_df[self.feature_columns]
        y = features_df["label"].astype(int)

        if len(X) < 30:
            raise ValueError("Need at least 30 valid rows to train")

        tscv = TimeSeriesSplit(n_splits=min(5, max(2, len(X) // 30)))
        fold_scores: List[Dict[str, float]] = []
        for train_idx, val_idx in tscv.split(X):
            model = self._make_classifier()
            X_train = X.iloc[train_idx]
            y_train = y.iloc[train_idx]
            X_val = X.iloc[val_idx]
            y_val = y.iloc[val_idx]

            X_scaled_train = self.scaler.fit_transform(X_train)
            model.fit(X_scaled_train, y_train)
            X_scaled_val = self.scaler.transform(X_val)
            val_pred = model.predict(X_scaled_val)

            fold_scores.append(
                {
                    "accuracy": accuracy_score(y_val, val_pred),
                    "precision": precision_score(y_val, val_pred, zero_division=0),
                    "recall": recall_score(y_val, val_pred, zero_division=0),
                    "f1": f1_score(y_val, val_pred, zero_division=0),
                }
            )

        if fold_scores:
            self.validation_summary = {
                "accuracy_percent": round(float(np.mean([s["accuracy"] for s in fold_scores])) * 100.0, 2),
                "precision_percent": round(float(np.mean([s["precision"] for s in fold_scores])) * 100.0, 2),
                "recall_percent": round(float(np.mean([s["recall"] for s in fold_scores])) * 100.0, 2),
                "f1_percent": round(float(np.mean([s["f1"] for s in fold_scores])) * 100.0, 2),
                "folds": len(fold_scores),
            }

        final_model = self._make_classifier()
        final_model.fit(self.scaler.fit_transform(X), y)
        self.model = final_model

        return self

    def get_validation_metrics(self) -> Dict[str, Any]:
        return self.validation_summary

    def predict(self, df: pd.DataFrame) -> Dict[str, Any]:
        if self.model is None:
            raise ValueError("Model is not trained")

        features_df = self.build_features(df)
        if features_df.empty:
            return {"symbol": None, "direction": "Hold", "probability_up": 0.0, "predicted_high": None, "predicted_high_change_pct": 0.0}

        features_df = self._sanitize_feature_frame(features_df)
        X = features_df[self.feature_columns]
        X_scaled = self.scaler.transform(X)
        probabilities = self.model.predict_proba(X_scaled)[:, 1]

        latest = features_df.iloc[-1]
        current_close = self._safe_float(df.iloc[-1].get("close"), 0.0)
        current_high = self._safe_float(df.iloc[-1].get("high"), 0.0)

        probability_up = float(probabilities[-1])
        probability_down = 1.0 - probability_up
        direction = "Up" if probability_up >= 0.5 else "Down"

        predicted_change_pct = (probability_up - 0.5) * 10.0
        predicted_change_pct = float(np.clip(predicted_change_pct, -8.0, 8.0))
        predicted_close = current_close * (1.0 + (predicted_change_pct / 100.0))
        predicted_high = current_high * (1.0 + max(0.0, predicted_change_pct / 100.0) + 0.006)
        predicted_low = current_close * (1.0 + min(0.0, predicted_change_pct / 100.0) - 0.006)
        confidence = max(probability_up, probability_down)

        predicted_high_change_pct = ((predicted_high - current_close) / current_close) * 100.0 if current_close else 0.0
        predicted_low_change_pct = ((predicted_low - current_close) / current_close) * 100.0 if current_close else 0.0
        predicted_close_change_pct = ((predicted_close - current_close) / current_close) * 100.0 if current_close else 0.0

        result = {
            "symbol": str(latest.get("symbol", "")),
            "direction": direction,
            "probability_up": round(probability_up, 4),
            "probability_down": round(probability_down, 4),
            "confidence": round(confidence, 4),
            "confidence_percent": round(confidence * 100.0, 2),
            "current_close": round(current_close, 4),
            "current_high": round(current_high, 4),
            "predicted_close": round(float(predicted_close), 4),
            "predicted_high": round(float(predicted_high), 4),
            "predicted_low": round(float(predicted_low), 4),
            "predicted_high_change_pct": round(float(predicted_high_change_pct), 2) if current_close else 0.0,
            "predicted_low_change_pct": round(float(predicted_low_change_pct), 2) if current_close else 0.0,
            "predicted_close_change_pct": round(float(predicted_close_change_pct), 2) if current_close else 0.0,
            "signal": "BUY" if direction == "Up" else "SELL",
        }

        if self.validation_summary:
            result["validation_metrics"] = self.validation_summary

        return result

    def evaluate_predictions(self, actual_df: pd.DataFrame) -> Dict[str, Any]:
        if self.model is None:
            raise ValueError("Model is not trained")

        features_df = self.build_features(actual_df)
        if features_df.empty:
            return {"error": "No valid rows in test data"}

        features_df = self._sanitize_feature_frame(features_df)
        X = features_df[self.feature_columns]
        y_true = features_df["label"].astype(int).to_numpy()
        X_scaled = self.scaler.transform(X)
        y_pred = self.model.predict(X_scaled)

        metrics = {
            "accuracy_percent": round(float(accuracy_score(y_true, y_pred)) * 100.0, 2),
            "precision_percent": round(float(precision_score(y_true, y_pred, zero_division=0)) * 100.0, 2),
            "recall_percent": round(float(recall_score(y_true, y_pred, zero_division=0)) * 100.0, 2),
            "f1_percent": round(float(f1_score(y_true, y_pred, zero_division=0)) * 100.0, 2),
            "validation_samples": len(y_true),
        }
        self.validation_summary = metrics
        return metrics

    def save_model(self, model_path: Optional[str] = None):
        if model_path is None:
            model_path = str(MODEL_DIR / "stock_prediction_model.joblib")
        payload = {
            "model": self.model,
            "scaler": self.scaler,
            "feature_columns": self.feature_columns,
            "model_name": self.model_name,
            "validation_summary": self.validation_summary,
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
        instance.validation_summary = payload.get("validation_summary", {})
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
        "validation_summary": model.validation_summary,
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

