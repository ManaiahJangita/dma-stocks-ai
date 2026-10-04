# AI Stock Analyzer v2

This upgraded starter supports Angel One historical candles for:
ONE_MINUTE, THREE_MINUTE, FIVE_MINUTE, TEN_MINUTE, FIFTEEN_MINUTE,
THIRTY_MINUTE, ONE_HOUR and ONE_DAY.

Angel One's current documentation lists maximum days per historical request:
1 minute 30 days; 3 minute 60; 5 minute 100; 10 minute 100;
15 minute 200; 30 minute 200; 1 hour 400; 1 day 2000.

Use the historical endpoint with:
{
  "symbolToken": "2885",
  "exchange": "NSE",
  "interval": "FIVE_MINUTE",
  "fromDate": "2026-01-01",
  "toDate": "2026-03-01"
}

The next production step should be:
1. instrument-master ingestion for symbol/token mapping,
2. batch PostgreSQL upserts,
3. binary WebSocket V2 tick decoding,
4. tick-to-candle aggregation,
5. technical indicators,
6. backtesting,
7. ML ranking,
8. Spring AI explanation.

The AI layer should explain quantitative results rather than directly predicting prices.
