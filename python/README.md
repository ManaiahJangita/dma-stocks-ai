# Python AI Prediction Module

This folder contains the Python-based stock prediction model used for training and inference.

## Files
- `stock_prediction_model_v2.py` - main model trainer and predictor
- `requirements.txt` - Python dependencies

## Setup
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt

## Train model
python stock_prediction_model_v2.py train --input data/stock_history.csv --model-name xgboost

## Predict from CSV
python stock_prediction_model_v2.py predict --input data/stock_history.csv --model-path models/stock_prediction_model.joblib --model-name xgboost

